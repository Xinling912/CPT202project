package com.cpt202.app.controller;



import com.cpt202.app.service.BookingService;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.security.Principal;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;



    // 采用构造器注入，保证依赖清晰
    public BookingController(BookingService bookingService
                           ) {
        this.bookingService = bookingService;


    }

    /**
     * 【下单：核心预约逻辑】
     */
    @PostMapping("/create")
    public ResponseEntity<?> createBooking(@RequestBody BookingRequest request, Principal principal) {
        try {
            // 【修改前】Booking newOrder = bookingService.createBooking(...);

            // 【修改后】：变量类型要与 Service 的返回值类型完全一致
            BookingService.BookingResponse response = bookingService.createBooking(
                    principal.getName(),
                    request.specialistId(),
                    request.slotId(),
                    request.notes()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An unexpected error occurred: " + e.getMessage()));
        }
    }

    /**
     * 【取消订单】用户/专家均可操作
     */
    @PostMapping("/cancel/{orderId}")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId, @RequestParam String reason, Principal principal) {
        try {
            bookingService.cancelBooking(orderId, reason, principal.getName());
            return ResponseEntity.ok(Map.of("message", "Order cancelled successfully"));
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("AUTHORIZED")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 【专家确认订单】
     */
    @PostMapping("/confirm/{orderId}")
    public ResponseEntity<?> confirmOrder(@PathVariable Long orderId, Principal principal) {
        try {
            bookingService.confirmOrder(orderId, principal.getName());
            return ResponseEntity.ok(Map.of("message", "Order confirmed successfully"));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * 【完成订单】
     */
    @PostMapping("/complete/{orderId}")
    public ResponseEntity<?> completeOrder(@PathVariable Long orderId, Principal principal) {
        try {
            bookingService.completeOrder(orderId, principal.getName());
            return ResponseEntity.ok(Map.of("message", "Order marked as completed"));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * 【查询: 我的预约历史 (顾客视角)】

     */
    @GetMapping("/myOrders")
    public ResponseEntity<?> getMyOrders(Principal principal) {
        try {
            // Service 已经搞定了一切，Controller 只需要“指挥”
            List<BookingService.BookingResponse> response = bookingService.getOrdersByCustomerResponse(principal.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 【查询: 专家收到的预约 (专家视角)】

     */
    @GetMapping("/specialist/my-bookings")
    public ResponseEntity<?> getSpecialistOrders(Principal principal) {
        try {
            // 这一行搞定所有事：业务逻辑、校验、转换数据
            List<BookingService.BookingResponse> response = bookingService.getSpecialistOrders(principal.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 如果是权限不足，这里捕获异常并返回 403
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        }
    }


    // 统一的异常处理器
    private ResponseEntity<?> handleException(Exception e) {
        if (e instanceof IllegalStateException) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        }
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

    /**
     * 前端请求的 DTO
     */
    public record BookingRequest(
            Long specialistId,
            Long slotId,
            String notes
    ) {}
}