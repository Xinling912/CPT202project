package com.cpt202.app.controller;

import com.cpt202.app.model.Booking;
import com.cpt202.app.repository.BookingRepository;
import com.cpt202.app.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

import java.util.List;

@RestController // 改成RestController，用来返回JSON接口
@RequestMapping("/booking") // 给所有接口加一个统一前缀
public class BookingController {

    // 注入Service，不再直接用Repository
    private final BookingService bookingService;

    // 构造器注入
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // 保留原来的测试页面接口
    @GetMapping("/test-page")
    public String showJQueryTestPage(){
        return "index";
    }

    // 专家确认订单
    @PostMapping("/confirm/{orderId}")
    public String confirmOrder(@PathVariable Long orderId) {
        bookingService.confirmOrder(orderId);
        return "确认成功";
    }

    // 用户取消订单
    @PostMapping("/cancel/{orderId}")
    public String cancelOrder(@PathVariable Long orderId, Principal principal) {
        bookingService.cancelOrder(orderId);
        return "取消成功";
    }

    // 完成订单
    @PostMapping("/complete/{orderId}")
    public String completeOrder(@PathVariable Long orderId) {
        bookingService.completeOrder(orderId);
        return "订单已完成";
    }

    // 用户（客户）查询自己的订单
    @GetMapping("/myOrders")
    public List<Booking> getMyOrders(Principal principal) {
        Long customerId = Long.parseLong(principal.getName());
        return bookingService.getOrdersByCustomer(customerId);
    }

    // 专家查询自己的预约
    @GetMapping("/specialist/{specialistId}")
    public List<Booking> getSpecialistOrders(@PathVariable Long specialistId) {
        return bookingService.getOrdersBySpecialist(specialistId);
    }
}