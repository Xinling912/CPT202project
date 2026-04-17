package com.cpt202.app.service;

import com.cpt202.app.model.Booking;
import com.cpt202.app.model.BookingStatus;
import com.cpt202.app.repository.BookingRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BookingService {

    //专家需要确认PENDING状态的订单
    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    // PENDING → CONFIRMED
    public void confirmOrder(Long orderId) {
        Booking booking = bookingRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("只有待确认的订单可以确认");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
    }

    public void cancelOrder(Long id) {
        Booking booking = bookingRepository.findById(id).orElseThrow();

        // 已完成 / 已取消 → 不能取消
        if (booking.getStatus() == BookingStatus.COMPLETED ||
                booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("已完成或已取消的订单无法取消");
        }

        // PENDING / CONFIRMED 都可以取消
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    // CONFIRMED → COMPLETED
    public void completeOrder(Long orderId) {
        Booking booking = bookingRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException("只有已确认的订单可以标记完成");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);
    }

    // 根据用户ID查订单
    public List<Booking> getOrdersByCustomer(Long customerId) {
        return bookingRepository.findByCustomerId(customerId);
    }

    // 根据专家ID查订单
    public List<Booking> getOrdersBySpecialist(Long specialistId) {
        return bookingRepository.findBySpecialistId(specialistId);
    }
}
