package com.cpt202.app.service;

import com.cpt202.app.model.*;
import com.cpt202.app.repository.BookingRepository;
import com.cpt202.app.repository.SpecialistProfileRepository;
import com.cpt202.app.repository.TimeSlotRepository;
import com.cpt202.app.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;



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

    // 2. 构造器注入
    public BookingService(BookingRepository bookingRepository, TimeSlotRepository timeSlotRepository, UserRepository userRepository, SpecialistProfileRepository specialistRepository) {
        this.bookingRepository = bookingRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.userRepository = userRepository;
        this.specialistRepository = specialistRepository;

    }

    // 1. Record：专门用来定义查询结果最后传送时
    // 只能有这 8 个固定的格子，绝不允许多装其他涉及隐私的数据
    // 依靠位置对应赋值执行
    // new BookedScheduleResponse(...) 时，传入的参数顺序必须和定义时的顺序一模一样
    public record BookedScheduleResponse(
            Long bookingId,
            Long timeSlotId,
            String date,
            String startTime,
            String endTime,
            Long customerId,
            String customerName,
            String customerEmail
    ) {}

    // 2. 业务方法
    public List<BookedScheduleResponse> getBookedSchedulesForSpecialist(Long specialistId) {
        // 定义允许上表单的白名单状态
        List<BookingStatus> validStatuses = Arrays.asList(BookingStatus.PENDING, BookingStatus.CONFIRMED);
        // 根据专家ID，和符合要求的订单状态查找符合要求的booking
        List<Booking> bookings = bookingRepository.findByTimeSlot_Specialist_IdAndStatusIn(specialistId, validStatuses);
        // Stream 流转换：把刚才获取的 Booking 倒进流水线，一个一个处理。
        return bookings.stream()
                //把booking转换可直接获取的信息
                //从 booking 里提取 Id、深入到 TimeSlot 表里提取日期和时间
                //再深入到 Customer (User) 表里提取姓名和邮箱
                .map(booking -> new BookedScheduleResponse(
                        booking.getId(),
                        booking.getTimeSlot().getId(),
                        booking.getTimeSlot().getSlotDate().toString(),
                        booking.getTimeSlot().getStartTime().toString(),
                        booking.getTimeSlot().getEndTime().toString(),
                        booking.getCustomer().getId(),
                        booking.getCustomer().getUsername(),
                        booking.getCustomer().getEmail()
                ))
                .collect(Collectors.toList()); //把分开的数据再次打包进list
    }


    @Transactional(rollbackFor = Exception.class)
    public Booking createBooking(String email, Long specialistId, Long slotId, String notes) {
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
        booking.setTotalAmount(specialist.getHourlyFee());

        return bookingRepository.save(booking);
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

    @Transactional
    public void confirmOrder(Long orderId, String email) {
        // 1. 在这里做身份转换
        SpecialistProfile profile = getProfileByEmail(email);

        // 2. 调用原有的核心逻辑
        this.confirmOrder(orderId, profile.getId());
    }

    @Transactional
    public void completeOrder(Long orderId, String email) {
        // 1. 解析身份
        Long specialistId = getProfileByEmail(email).getId();

        // 2. 调用核心逻辑 (复用你原本写好的那个方法)
        this.completeOrder(orderId, specialistId);
    }


    //专家确认订单(确保只有专家能完成)
    @Transactional
    public void confirmOrder(Long orderId, Long currentSpecialistId) {
        Booking booking = getVerifiedBookingForSpecialist(orderId, currentSpecialistId);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("ERROR_INVALID_STATUS");
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
    }

    // 专家标记完成
    @Transactional
    public void completeOrder(Long orderId, Long currentSpecialistId) {
        // 1. 校验所属权
        Booking booking = getVerifiedBookingForSpecialist(orderId, currentSpecialistId);

        // 2. 校验状态流转
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("ERROR_ONLY_CONFIRMED_CAN_BE_COMPLETED");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);
    }

    /**
     * 【查询优化】
     */
    public List<Booking> getOrdersByCustomer(Long customerId) {
        return bookingRepository.findByCustomerId(customerId);
    }

    public List<Booking> getOrdersBySpecialist(Long specialistId) {
        return bookingRepository.findBySpecialistId(specialistId);
    }

    private long countMonthlyCancellations(Long customerId) {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        return bookingRepository.countByCustomerIdAndStatusAndCreatedAtAfter(customerId, BookingStatus.CANCELLED, startOfMonth);
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