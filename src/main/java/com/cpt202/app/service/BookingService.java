package com.cpt202.app.service;

import com.cpt202.app.model.*;
import com.cpt202.app.repository.BookingRepository;
import com.cpt202.app.repository.SpecialistProfileRepository;
import com.cpt202.app.repository.TimeSlotRepository;
import com.cpt202.app.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import org.springframework.scheduling.annotation.Scheduled;





import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {


    // 1. 使用 final 关键字，确保依赖不可变
    private final BookingRepository bookingRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final UserRepository userRepository;
    private final SpecialistProfileRepository specialistRepository;
    private final PriceService priceService;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BookingService.class);

    // 2. 构造器注入
    public BookingService(BookingRepository bookingRepository, TimeSlotRepository timeSlotRepository, UserRepository userRepository, SpecialistProfileRepository specialistRepository,PriceService priceService) {
        this.bookingRepository = bookingRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.userRepository = userRepository;
        this.specialistRepository = specialistRepository;
        this.priceService = priceService;

    }


    // 在 BookingService 类内部的 record 定义处修改：
    public record BookingResponse(
            Long id,
            String customerName,
            String specialistName,
            String date,
            String startTime,
            String endTime,
            String status,
            String notes,
            BigDecimal totalAmount // <--- 必须添加这个字段
    ) {}


    @Transactional(rollbackFor = Exception.class)
    public BookingResponse createBooking(String email, Long specialistId, Long slotId, String notes) {
        // 1. 【安全查找】Service 内部完成身份确认
        User customer = userRepository.findByUsername(email)
                .orElseThrow(() -> new IllegalArgumentException("ERROR_USER_NOT_FOUND"));

        SpecialistProfile specialist = specialistRepository.findById(specialistId)
                .orElseThrow(() -> new IllegalArgumentException("ERROR_SPECIALIST_NOT_FOUND"));

        // 2. 获取时间段 (悲观锁锁定)
        TimeSlot slot = timeSlotRepository.findByIdWithLock(slotId)
                .orElseThrow(() -> new IllegalArgumentException("ERROR_SLOT_NOT_FOUND"));

        // 3. 业务校验
        LocalDateTime appointmentDateTime = LocalDateTime.of(slot.getSlotDate(), slot.getStartTime());
        if (appointmentDateTime.isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("ERROR_SLOT_EXPIRED");
        }

        //4.确保只有状态为available的订单才能被预定
        if (slot.getStatus() != TimeSlotStatus.AVAILABLE) {
            throw new IllegalStateException("ERROR_SLOT_TAKEN");
        }

        // 5. 一个月内取消预约大于三次的顾客不能预定
        long cancelCount = countMonthlyCancellations(customer.getId());
        if (cancelCount >= 3) {
            throw new IllegalStateException("ERROR_MONTHLY_LIMIT_REACHED");
        }

        //6.顾客重复预约效验
        // 6.5 防止恶意刷单：如果该顾客曾经取消过这个时间段，禁止他立即重新预定！
        // （注意：这里只拦截当前 customer，不拦截其他顾客，完美实现把机会留给别人）
        boolean hasCancelledBefore = bookingRepository.existsByCustomerIdAndTimeSlotIdAndStatus(
                customer.getId(), slotId, BookingStatus.CANCELLED);

        if (hasCancelledBefore) {
            // 抛出我们前端专属定制的错误码！
            throw new IllegalStateException("ERROR_RECENTLY_CANCELLED");
        }
        List<BookingStatus> activeStatuses = Arrays.asList(BookingStatus.PENDING, BookingStatus.CONFIRMED);
        boolean alreadyBooked = bookingRepository.existsByCustomerIdAndTimeSlotIdAndStatusIn(
                customer.getId(), slotId, activeStatuses);

        if (alreadyBooked) {
            throw new IllegalStateException("ERROR_DUPLICATE_BOOKING_AT_SAME_TIME");
        }


        // 7. 防止专家用自己的账号预约
        if (customer.getId().equals(specialist.getUser().getId())) {
            throw new IllegalStateException("You cannot book your own service.");
        }

        // 8.执行状态同步更新
        slot.setStatus(TimeSlotStatus.BOOKED);
        timeSlotRepository.save(slot);

        // 9.构造订单实体
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setSpecialist(specialist);
        booking.setTimeSlot(slot);
        booking.setStatus(BookingStatus.PENDING);
        booking.setNotes(notes);

        BigDecimal total = priceService.calculateTotalAmount(
                specialist.getHourlyFee(),
                slot.getStartTime(),
                slot.getEndTime()
        );
        booking.setTotalAmount(total);

        //10.添加日志
        log.info("Booking created successfully. BookingId: {}, Customer: {}", booking.getId(), customer.getUsername());

        // 【修改点】：不再直接返回 Entity，而是先保存，然后转成 DTO
        Booking savedBooking = bookingRepository.save(booking);
        return convertToResponse(savedBooking);
    }


    //pbi5
    // 取消订单功能
    @Transactional
    public void cancelBooking(Long bookingId, String reason, String email) {
        // 1. 获取用户与订单信息
        User user = userRepository.findByUsername(email)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("ERROR_BOOKING_NOT_FOUND"));

        // 2. 鉴权：使用内部辅助方法判断角色和所属权
        boolean isSpecialist = user.getRole().equals(UserRole.SPECIALIST);
        validateAuthorization(booking, user, isSpecialist);

        // 3. 业务规则校验：如果是客户取消，检查 24 小时限制
        if (!isSpecialist) {
            LocalDateTime appointmentTime = LocalDateTime.of(booking.getTimeSlot().getSlotDate(), booking.getTimeSlot().getStartTime());
            if (appointmentTime.isBefore(LocalDateTime.now().plusHours(24))) {
                throw new IllegalStateException("ERROR_CANCEL_LIMIT_EXCEEDED: Must cancel at least 24 hours in advance");
            }
        }
        // 4. 执行业务逻辑 (状态流转)，完成或者已关闭booking不能取消
        if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("ERROR_CANNOT_CANCEL_FINALIZED_ORDER");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        String operator = isSpecialist ? "Specialist" : "Customer";
        String currentNotes = (booking.getNotes() == null) ? "" : booking.getNotes();
        booking.setNotes(currentNotes + " | Cancelled by " + operator + ". Reason: " + reason);

        // 5. 更新 Slot 状态
        TimeSlot slot = booking.getTimeSlot();
        if (slot != null) {
            slot.setStatus(isSpecialist ? TimeSlotStatus.DISABLED : TimeSlotStatus.AVAILABLE);
            timeSlotRepository.save(slot);
        }


    }

    @Scheduled(cron = "0 * * * * *") // 务必改为每分钟执行一次
    @Transactional(rollbackFor = Exception.class)
    public void processAutoStatusTransitions() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> pendingBookings = bookingRepository.findByStatus(BookingStatus.PENDING);

        for (Booking b : pendingBookings) {
            LocalDateTime startTime = LocalDateTime.of(b.getTimeSlot().getSlotDate(), b.getTimeSlot().getStartTime());

            // --- 优先级 1: 5分钟硬核截止线 (无论是否紧急，直接 DISABLED) ---
            if (now.isAfter(startTime.minusMinutes(5))) {
                cancelBookingAsDisabled(b, "EXPIRED: Less than 5 mins to start");
                continue; // 跳过后续逻辑，直接进入下一个循环
            }

            // --- 优先级 2: 确认时限 (根据紧急程度判断) ---
            boolean isUrgent = startTime.isBefore(now.plusHours(24));
            LocalDateTime deadline = isUrgent ? b.getCreatedAt().plusHours(1) : b.getCreatedAt().plusHours(24);

            if (now.isAfter(deadline)) {
                cancelBookingAsAvailable(b, "TIMEOUT: Confirmation deadline exceeded");
            }
        }

        // 3. 自动完成 (Confirmed -> Completed)
        List<Booking> confirmedBookings = bookingRepository.findByStatus(BookingStatus.CONFIRMED);
        for (Booking b : confirmedBookings) {
            LocalDateTime end = LocalDateTime.of(b.getTimeSlot().getSlotDate(), b.getTimeSlot().getEndTime());
            if (now.isAfter(end.plusHours(24))) {
                b.setStatus(BookingStatus.COMPLETED);
                bookingRepository.save(b);
            }
        }
    }

    /// 1. 入口方法：Controller 调用此方法，开启事务
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrder(Long orderId, String email) {
        Long specialistId = getProfileByEmail(email).getId();
        // 直接调用内部核心逻辑
        confirmOrderInternal(orderId, specialistId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void completeOrder(Long orderId, String email) {
        Long specialistId = getProfileByEmail(email).getId();
        // 直接调用内部核心逻辑
        completeOrderInternal(orderId, specialistId);
    }


    // 2. 核心业务逻辑：private 方法，不需要 @Transactional
    // 因为它由上面的入口方法调用，入口方法已经开启了事务
    // 在 confirmOrderInternal 中加入拦截
    private void confirmOrderInternal(Long orderId, Long specialistId) {
        Booking booking = getVerifiedBookingForSpecialist(orderId, specialistId);

        // 【错误：不应该限制提前5小时】
        LocalDateTime startTime = LocalDateTime.of(booking.getTimeSlot().getSlotDate(), booking.getTimeSlot().getStartTime());
        if (LocalDateTime.now().isAfter(startTime.minusMinutes(5))) {
            throw new IllegalStateException("ERROR_CONFIRMATION_EXPIRED");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("ERROR_INVALID_STATUS");
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
    }

    private void completeOrderInternal(Long orderId, Long specialistId) {
        Booking booking = getVerifiedBookingForSpecialist(orderId, specialistId);

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("ERROR_ONLY_CONFIRMED_CAN_BE_COMPLETED");
        }

        LocalDateTime startTime = LocalDateTime.of(
                booking.getTimeSlot().getSlotDate(),
                booking.getTimeSlot().getStartTime()
        );
        // 确保订单已经真正开始了
        if (LocalDateTime.now().isBefore(startTime)) {
            throw new IllegalStateException("ERROR_BOOKING_NOT_STARTED_YET");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);
    }

    // 辅助方法：状态设为 CANCELLED，资源设为 DISABLED (封禁)
    private void cancelBookingAsDisabled(Booking b, String reason) {
        b.setStatus(BookingStatus.CANCELLED);
        b.setNotes((b.getNotes() == null ? "" : b.getNotes()) + " | " + reason);

        TimeSlot slot = b.getTimeSlot();
        slot.setStatus(TimeSlotStatus.DISABLED); // 封禁资源

        timeSlotRepository.save(slot);
        bookingRepository.save(b);
        log.warn("Booking {} cancelled. Slot {} DISABLED due to: {}", b.getId(), slot.getId(), reason);
    }

    // 辅助方法：状态设为 CANCELLED，资源设为 AVAILABLE (释放)
    private void cancelBookingAsAvailable(Booking b, String reason) {
        b.setStatus(BookingStatus.CANCELLED);
        b.setNotes((b.getNotes() == null ? "" : b.getNotes()) + " | " + reason);

        TimeSlot slot = b.getTimeSlot();
        slot.setStatus(TimeSlotStatus.AVAILABLE); // 释放资源

        timeSlotRepository.save(slot);
        bookingRepository.save(b);
        log.info("Booking {} cancelled. Slot {} released.", b.getId(), slot.getId());
    }





    private long countMonthlyCancellations(Long customerId) {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        return bookingRepository.countByCustomerIdAndStatusAndCreatedAtAfter(customerId, BookingStatus.CANCELLED, startOfMonth);
    }

    //实现专家查数据库
    public List<BookingResponse> getSpecialistOrders(String username) {
        // 1. 查用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 2. 校验角色 (逻辑搬移)
        if (!user.getRole().equals(UserRole.SPECIALIST)) {
            throw new IllegalStateException("Access denied: You are not a specialist");
        }

        // 3. 查档案
        SpecialistProfile profile = specialistRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Specialist profile not found"));

        // 4. 查订单并【转换成 DTO】(解决 Entity 泄露)
        return bookingRepository.findBySpecialistId(profile.getId())
                .stream()
                .map(this::convertToResponse) // 这一步把 Booking 变成前端要的 DTO
                .collect(Collectors.toList());
    }

    // 为普通用户提供其个人的订单列表
    public List<BookingResponse> getOrdersByCustomerResponse(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return bookingRepository.findByCustomerId(user.getId())
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // 私有辅助方法：专门用来“瘦身”数据
    private BookingResponse convertToResponse(Booking b) {
        return new BookingResponse(
                b.getId(),
                b.getCustomer().getUsername(),
                b.getSpecialist().getUser().getUsername(),
                b.getTimeSlot().getSlotDate().toString(),
                b.getTimeSlot().getStartTime().toString(),
                b.getTimeSlot().getEndTime().toString(),
                b.getStatus().toString(),
                b.getNotes(),
                b.getTotalAmount() // <--- 确保加上这个
        );
    }


    // 将“用户提供的邮箱（外部凭证）”转化为“系统内部的业务对象（专家档案）”
    private SpecialistProfile getProfileByEmail(String email) {
        // 你需要注入 userRepository 和 specialistRepository
        User user = userRepository.findByUsername(email)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
        return specialistRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("SPECIALIST_NOT_FOUND"));
    }

//获取数据 + 检查拥有权（确保该订单是否属于当前的id）
    private Booking getVerifiedBookingForSpecialist(Long orderId, Long currentSpecialistId) {
        Booking booking = bookingRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("ERROR_BOOKING_NOT_FOUND"));

        if (!booking.getSpecialist().getId().equals(currentSpecialistId)) {
            throw new IllegalStateException("ERROR_NOT_AUTHORIZED_TO_OPERATE_THIS_ORDER");
        }
        return booking;
    }



    // 根据角色（专家/客户/管理员）执行不同的校验规则
    private void validateAuthorization(Booking booking, User user, boolean isSpecialist) {
        // 1. 管理员拥有最高权限，直接跳过校验 (或者记录审计日志)
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }
        // 2. 如果是专家，校验是否为该订单的服务者
        if (isSpecialist) {
            if (!booking.getSpecialist().getUser().getId().equals(user.getId())) {
                throw new IllegalStateException("ERROR_NOT_AUTHORIZED: You are not the specialist for this booking.");
            }
        }
        // 3. 如果是顾客，校验是否为该订单的预订者
        else {
            if (!booking.getCustomer().getId().equals(user.getId())) {
                throw new IllegalStateException("ERROR_NOT_AUTHORIZED: This booking does not belong to you.");
            }
        }
    }
}