package com.cpt202.app.controller;

import com.cpt202.app.model.Booking;
import com.cpt202.app.model.User;
import com.cpt202.app.model.SpecialistProfile;
import com.cpt202.app.model.UserRole;
import com.cpt202.app.service.BookingService;
import com.cpt202.app.repository.UserRepository;
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

    // 采用构造器注入，保证依赖清晰
    public BookingController(BookingService bookingService,
                             UserRepository userRepository,
                             SpecialistProfileRepository specialistRepository) {
        this.bookingService = bookingService;
        this.userRepository = userRepository;
        this.specialistRepository = specialistRepository;
    }


    /**
     * 【PBI 4: 核心预约逻辑 - 安全增强版】
     * 现在的逻辑：从 Token (Principal) 中获取用户身份，防止越权下单
     */
    @PostMapping("/create")
    public ResponseEntity<?> createBooking(
            @RequestBody BookingRequest request,
            Principal principal // ✨ 从安全上下文中注入当前用户
    ) {
        try {
            // 1. 安全校验：从 Principal 获取当前登录者的唯一标识（如 Email 或 Username）

            //String identifier = principal.getName();
            User customer = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("ERROR_USER_NOT_FOUND"));

            // 2. 找到专家对象
            SpecialistProfile specialist = specialistRepository.findById(request.specialistId())
                    .orElseThrow(() -> new IllegalArgumentException("ERROR_SPECIALIST_NOT_FOUND"));

            // 3. 调用 Service 执行业务（包含并发检测和月取消次数校验）
            Booking newOrder = bookingService.createBooking(
                    customer,
                    specialist,
                    request.slotId(),
                    request.notes()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(newOrder);

        } catch (IllegalStateException e) {
            // 统一使用 Map.of
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An unexpected error occurred: " + e.getMessage()));
        }
    }


    /**
     * 【PBI 5: 用户/专家取消订单】
     */
    @PostMapping("/cancel/{orderId}")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId, @RequestParam(required = false) String reason) {
        try {
            bookingService.cancelBooking(orderId, reason != null ? reason : "Cancelled by user");
            return ResponseEntity.ok(Map.of("message", "Cancellation successful"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
    /**
     * 【PBI 5: 专家确认订单】
     */
    @PostMapping("/confirm/{orderId}")
    public ResponseEntity<?> confirmOrder(@PathVariable Long orderId) {
        try {
            bookingService.confirmOrder(orderId);
            return ResponseEntity.ok(Map.of("message", "Order confirmed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 【PBI 5: 完成订单】
     */
    @PostMapping("/complete/{orderId}")
    public ResponseEntity<?> completeOrder(@PathVariable Long orderId) {
        try {
            bookingService.completeOrder(orderId);
            return ResponseEntity.ok(Map.of("message", "Order marked as completed"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
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

    /**
     * DTO：不再需要 customerId
     */
    public record BookingRequest(
            Long specialistId,
            Long slotId,
            String notes
    ) {}
}