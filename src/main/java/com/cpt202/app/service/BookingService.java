package com.cpt202.app.service;
import com.cpt202.app.model.*;
import com.cpt202.app.repository.BookingRepository;
import com.cpt202.app.repository.TimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    /**
     * 【PBI 4: 核心预约逻辑】
     * 功能：创建预约订单，并执行严格的并发冲突检测与业务规则校验
     * * @param customer   下单客户
     * @param specialist 被预约的专家
     * @param slotId     选择的时间段ID
     * @param notes      用户备注
     * @return 成功创建的订单对象
     */
    @Transactional(rollbackFor = Exception.class) // 开启事务，任何异常都会触发回滚
    public Booking createBooking(User customer, SpecialistProfile specialist, Long slotId, String notes) {

        // 1. 获取时间段 (建议使用 findByIdForUpdate 悲观锁锁定此行，防止并发抢单)
        // 对应 Task 4.2: Concurrency conflict prevention
        TimeSlot slot = timeSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("ERROR_SLOT_NOT_FOUND"));

        // 2. 状态预检：只有 AVAILABLE 的时间段才能被预约
        if (!"AVAILABLE".equals(slot.getStatus())) {
            // 对应 Task 4.4: 返回特定错误码，供前端 AJAX 判断并提示“该时段已被抢占”
            throw new IllegalStateException("ERROR_SLOT_TAKEN");
        }

        // 3. 业务逻辑校验：检查该客户当月违约次数
        // 对应 PBI 1.2: 软约束规则，防止恶意占位
        long cancelCount = countMonthlyCancellations(customer.getId());
        if (cancelCount >= 3) {
            throw new IllegalStateException("ERROR_MONTHLY_LIMIT_REACHED");
        }

        // 4. 执行状态同步更新 (关键：先改状态，后建订单)
        // 将状态设为 LOCKED，专家在 PBI 5 中确认后会变为 CONFIRMED
        slot.setStatus("LOCKED");
        timeSlotRepository.save(slot);

        // 5. 构造订单实体 (Task 4.1)
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setSpecialist(specialist);
        booking.setTimeSlot(slot);
        booking.setStatus(BookingStatus.PENDING); // 初始状态设为待确认
        booking.setNotes(notes);

        // 自动计算费用：从专家配置中获取

        booking.setTotalAmount(specialist.getHourlyFee());

        // 6. 持久化到数据库
        return bookingRepository.save(booking);
    }

    /**
     * 【PBI 5: 订单状态变更】
     * 功能：取消预约并释放对应的时间段资源
     */
    @Transactional
    public void cancelBooking(Long bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("ERROR_BOOKING_NOT_FOUND"));

        // 业务规则：已完成的订单禁止取消
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("ERROR_CANNOT_CANCEL_COMPLETED");
        }

        // 更新订单状态与取消原因
        booking.setStatus(BookingStatus.CANCELLED);
        String updatedNotes = (booking.getNotes() == null) ? "" : booking.getNotes();
        booking.setNotes(updatedNotes + " | Cancel Reason: " + reason);

        // 释放资源：将对应的时间段重新设为“可用”
        TimeSlot slot = booking.getTimeSlot();
        if (slot != null) {
            slot.setStatus("AVAILABLE");
            timeSlotRepository.save(slot);
        }

        bookingRepository.save(booking);
    }

    /**
     * 【内部辅助：查询优化】
     * 对应 Task 5.1: 通过数据库聚合查询替代 Java 内存过滤，提升系统响应速度
     */
    private long countMonthlyCancellations(Long customerId) {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0);

        // 这里建议在 BookingRepository 中通过 @Query 实现，避免加载全表数据
        return bookingRepository.countByCustomerIdAndStatusAndCreatedAtAfter(
                customerId, BookingStatus.CANCELLED, startOfMonth);
    }
}


