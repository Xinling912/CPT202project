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



@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository; // 找客户用

    @Autowired
    private SpecialistProfileRepository specialistRepository; // 找专家用

    @PostMapping("/create")
    public ResponseEntity<?> createBooking(@RequestBody BookingRequest request) {
        try {
            // 1. 根据前端传来的 ID 找到真实的数据库对象
            User customer = userRepository.findById(request.customerId())
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

            SpecialistProfile specialist = specialistRepository.findById(request.specialistId())
                    .orElseThrow(() -> new IllegalArgumentException("Specialist not found"));

            // 2. 把查到的对象和散参数拆出来喂给 Service
            Booking newOrder = bookingService.createBooking(
                    customer,
                    specialist,
                    request.slotId(),
                    request.notes()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(newOrder);

        } catch (IllegalStateException e) {
            // Task 4.2: 冲突报错 (ERROR_SLOT_TAKEN)
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            // ID 找不着人时的报错
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    public record BookingRequest(
            Long customerId,
            Long specialistId,
            Long slotId,
            String notes
    ) {}
}