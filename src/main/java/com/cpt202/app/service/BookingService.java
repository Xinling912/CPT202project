package com.cpt202.app.service;
import com.cpt202.app.model.*;
import com.cpt202.app.repository.BookingRepository;
import com.cpt202.app.repository.TimeSlotRepository;
import com.cpt202.app.model.TimeSlotStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


import java.util.Arrays;
import java.util.List;

@Service
public class BookingService {


    // 1. 使用 final 关键字，确保依赖不可变
    private final BookingRepository bookingRepository;
    private final TimeSlotRepository timeSlotRepository;

    // 2. 构造器注入（Spring 4.3+ 之后，如果只有一个构造器，@Autowired 注解可以省略）
    public BookingService(BookingRepository bookingRepository, TimeSlotRepository timeSlotRepository) {
        this.bookingRepository = bookingRepository;
        this.timeSlotRepository = timeSlotRepository;
    }

    @Transactional(rollbackFor = Exception.class) // 开启事务，任何异常都会触发回滚
    public Booking createBooking(User customer, SpecialistProfile specialist, Long slotId, String notes) {

        // 1. 获取时间段 (建议使用 findByIdForUpdate 悲观锁锁定此行，防止并发抢单)
        // 对应 Task 4.2: Concurrency conflict prevention
        TimeSlot slot = timeSlotRepository.findByIdWithLock(slotId)
                .orElseThrow(() -> new IllegalArgumentException("ERROR_SLOT_NOT_FOUND"));


        // 只有当状态 不等于 AVAILABLE 时，才说明被抢占了
        if (slot.getStatus() != TimeSlotStatus.AVAILABLE) {
            throw new IllegalStateException("ERROR_SLOT_TAKEN");
        }

        // 3. 业务逻辑校验：检查该客户当月违约次数
        long cancelCount = countMonthlyCancellations(customer.getId());
        if (cancelCount >= 3) {
            throw new IllegalStateException("ERROR_MONTHLY_LIMIT_REACHED");
        }

        // 4.重复预约检查
        List<BookingStatus> activeStatuses = Arrays.asList(BookingStatus.PENDING, BookingStatus.CONFIRMED);
        boolean alreadyBooked = bookingRepository.existsByCustomerIdAndTimeSlotIdAndStatusIn(
                customer.getId(), slotId, activeStatuses);

        if (alreadyBooked) {
            throw new IllegalStateException("ERROR_DUPLICATE_BOOKING_AT_SAME_TIME");
        }

        // 5. 执行状态同步更新 (关键：先改状态，后建订单)
        // 将状态设为 LOCKED，专家在 PBI 5 中确认后会变为 CONFIRMED
        slot.setStatus(TimeSlotStatus.BOOKED);
        timeSlotRepository.save(slot);

        // 6. 构造订单实体 (Task 4.1)
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

    //pbi5
    //专家确认订单
    @Transactional
    public void confirmOrder(Long orderId) {
        Booking booking = bookingRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("ERROR_BOOKING_NOT_FOUND"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("ERROR_INVALID_STATUS_FOR_CONFIRMATION");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
    }


    // 取消订单功能
    @Transactional
    public void cancelBooking(Long bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("ERROR_BOOKING_NOT_FOUND"));

        // 状态机校验：已完成或已取消的订单不可操作
        if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("ERROR_CANNOT_CANCEL_FINALIZED_ORDER");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        String updatedNotes = (booking.getNotes() == null) ? "" : booking.getNotes();
        booking.setNotes(updatedNotes + " | Cancel Reason: " + reason);

        // 释放 TimeSlot 资源，使其重新变为可用
        TimeSlot slot = booking.getTimeSlot();
        if (slot != null) {
            slot.setStatus(TimeSlotStatus.AVAILABLE);
            timeSlotRepository.save(slot);
        }

        bookingRepository.save(booking);
    }

    // 标记订单完成
    @Transactional
    public void completeOrder(Long orderId) {
        Booking booking = bookingRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("ERROR_BOOKING_NOT_FOUND"));

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

}


