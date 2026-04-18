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
import java.security.Principal; // 必须导入这个
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
     * 【PBI 4: 核心预约逻辑 - 安全增强版】
     * 现在的逻辑：从 Token (Principal) 中获取用户身份，防止越权下单
     */
    @PostMapping("/create")
    public ResponseEntity<?> createBooking(
            @RequestBody BookingRequest request,
            Principal principal
    ) {
        try {
            // Controller 不再查找 User 或 SpecialistProfile
            // 只负责提取请求参数和当前用户标识(Email)
            Booking newOrder = bookingService.createBooking(
                    principal.getName(), // 传入 Email
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
     * 【PBI 5: 用户/专家取消订单】
     */
    @PostMapping("/cancel/{orderId}")
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long orderId,
            @RequestParam String reason,
            Principal principal
    ) {
        try {
            // 直接调用 Service，所有权限和业务规则都在 Service 内部校验
            bookingService.cancelBooking(orderId, reason, principal.getName());

            return ResponseEntity.ok(Map.of("message", "Order cancelled successfully"));
        } catch (IllegalStateException e) {
            // 区分是鉴权失败(403)还是业务规则失败(409)
            if (e.getMessage().contains("AUTHORIZED")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 【PBI 5: 专家确认订单】简化后只需一行
     */
    @PostMapping("/confirm/{orderId}")
    public ResponseEntity<?> confirmOrder(@PathVariable Long orderId, Principal principal) {
        try {
            // 直接把 identifier (email) 传给 Service，让 Service 去处理 profile 查询
            bookingService.confirmOrder(orderId, principal.getName());
            return ResponseEntity.ok(Map.of("message", "Order confirmed successfully"));
        } catch (Exception e) {
            return handleException(e);
        }
    }

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
     * 【查询: 我的预约列表】
     */
    @GetMapping("/myOrders")
    public ResponseEntity<?> getMyOrders(Principal principal) {
        try {
            // 这里统一逻辑：先根据 Principal 找到 User 对象，再拿 ID 查
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // 2.角色校验
            if (!user.getRole().equals(UserRole.CUSTOMER)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Access denied: This portal is for customers only"));
            }
            //3.
            List<Booking> orders = bookingService.getOrdersByCustomer(user.getId());
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 【查询: 专家收到的预约】
     */
    @GetMapping("/specialist/my-bookings")
    public ResponseEntity<?> getSpecialistOrders(Principal principal) {
        try {
            // 1. 先根据 Email 找到 User
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // 2. 角色校验
            if (!user.getRole().equals(UserRole.SPECIALIST)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Access denied: You are not a specialist"));
            }

            // 3. 【关键：重要修正】通过 User 对象找到关联的 SpecialistProfile
            SpecialistProfile profile = specialistRepository.findByUser(user)
                    .orElseThrow(() -> new IllegalArgumentException("Specialist profile not found"));

            // 4. 使用 Profile 的 ID 去查订单
            List<Booking> orders = bookingService.getOrdersBySpecialist(profile.getId());

            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", e.getMessage()));
        }
    }


    // 统一的异常处理器，减少每个方法里的 catch 块冗余
    private ResponseEntity<?> handleException(Exception e) {
        if (e instanceof IllegalStateException) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        }
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }


    /**
     * DTO：不再需要 customerId
     */
    public record BookingRequest(
            Long specialistId,
            Long slotId,
            String notes
    ) {}
}