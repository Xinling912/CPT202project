package com.cpt202.app.service;

import com.cpt202.app.model.Booking;
import com.cpt202.app.model.BookingStatus;
import com.cpt202.app.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {

    //依赖注入
    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
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
}