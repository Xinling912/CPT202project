package com.cpt202.app.controller;

import com.cpt202.app.model.Booking;
import com.cpt202.app.model.User;
import com.cpt202.app.model.SpecialistProfile;
import com.cpt202.app.model.UserRole;
import com.cpt202.app.service.BookingService;
import com.cpt202.app.repository.UserRepository;
import com.cpt202.app.repository.BookingRepository;
import com.cpt202.app.repository.SpecialistProfileRepository;

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
    private final UserRepository userRepository;
    private final SpecialistProfileRepository specialistRepository;
    private final BookingRepository bookingRepository;

    // 采用构造器注入，保证依赖清晰
    public BookingController(BookingService bookingService,
                             UserRepository userRepository,
                             SpecialistProfileRepository specialistRepository,
                             BookingRepository bookingRepository) {
        this.bookingService = bookingService;
        this.userRepository = userRepository;
        this.specialistRepository = specialistRepository;
        this.bookingRepository = bookingRepository;
    }

    /**
     * 【下单：核心预约逻辑】
     */
    @PostMapping("/create")
    public ResponseEntity<?> createBooking(@RequestBody BookingRequest request, Principal principal) {
        try {
            // principal.getName() 在你的系统里提取出的是 Username (如 "Carrot")
            Booking newOrder = bookingService.createBooking(
                    principal.getName(),
                    request.specialistId(),
                    request.slotId(),
                    request.notes()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(newOrder);

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
     * 🌟 核心修复：放宽了 Role 限制，并使用 findByUsername
     */
    @GetMapping("/myOrders")
    public ResponseEntity<?> getMyOrders(Principal principal) {
        try {
            // 通过 Token 里的 Username 找人
            User user = userRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found for username: " + principal.getName()));

            List<Booking> orders = bookingService.getOrdersByCustomer(user.getId());
            return ResponseEntity.ok(orders);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * 【查询: 专家收到的预约 (专家视角)】
     * 🌟 核心修复：使用 findByUsername 确保专家也能正确拉取数据
     */
    @GetMapping("/specialist/my-bookings")
    public ResponseEntity<?> getSpecialistOrders(Principal principal) {
        try {
            User user = userRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found for username: " + principal.getName()));

            // 只有专家才能进入工作台查看自己的业务订单
            if (!user.getRole().equals(UserRole.SPECIALIST)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Access denied: You are not a specialist"));
            }

            SpecialistProfile profile = specialistRepository.findByUser(user)
                    .orElseThrow(() -> new IllegalArgumentException("Specialist profile not found"));

            List<Booking> orders = bookingService.getOrdersBySpecialist(profile.getId());

            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", e.getMessage()));
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