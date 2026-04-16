package com.cpt202.app.controller;

import com.cpt202.app.model.Booking;
import com.cpt202.app.model.User;
import com.cpt202.app.model.SpecialistProfile;
import com.cpt202.app.service.BookingService;
import com.cpt202.app.repository.UserRepository;
import com.cpt202.app.repository.SpecialistProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal; // 必须导入这个


@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SpecialistProfileRepository specialistRepository;

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

            String identifier = principal.getName();
            User customer = userRepository.findByEmail(identifier)
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

            // 2. 找到专家对象
            SpecialistProfile specialist = specialistRepository.findById(request.specialistId())
                    .orElseThrow(() -> new IllegalArgumentException("Specialist not found"));

            // 3. 调用 Service 执行业务（包含并发检测和月取消次数校验）
            Booking newOrder = bookingService.createBooking(
                    customer,
                    specialist,
                    request.slotId(),
                    request.notes()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(newOrder);

        } catch (IllegalStateException e) {
            // 处理并发冲突 (ERROR_SLOT_TAKEN) 或 次数限制 (ERROR_MONTHLY_LIMIT_REACHED)
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error");
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