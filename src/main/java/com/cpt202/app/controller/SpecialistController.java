package com.cpt202.app.controller;

import com.cpt202.app.model.*;
import com.cpt202.app.repository.BookingRepository;
import com.cpt202.app.repository.SpecialistProfileRepository;
import com.cpt202.app.repository.TimeSlotRepository;
import com.cpt202.app.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/specialists")
@CrossOrigin(origins = "*")
public class SpecialistController {

    @Autowired(required = false) // 暂时设为 false 方便你跑 Mock 测试
    private SpecialistProfileRepository specialistRepository;

    @Autowired(required = false)
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private BookingService bookingService;

    // ==========================================
    // 接口 1: 分页获取专家大厅列表
    // ==========================================
    @GetMapping
    public Map<String, Object> getSpecialists(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Map<String, Object> response = new HashMap<>();

        Pageable pageable = PageRequest.of(page, size);
        Page<SpecialistProfile> specialistPage = specialistRepository.findByStatus(SpecialistStatus.ACTIVE, pageable);
        response.put("content", specialistPage.getContent());
        response.put("totalElements", specialistPage.getTotalElements());
        response.put("totalPages", specialistPage.getTotalPages());
        return response;
    }

    // ==========================================
    // 接口 2: 获取专家详细信息
    // ==========================================
    @GetMapping("/{id}")
    public SpecialistProfile getSpecialistDetail(@PathVariable("id") Long id) {
        return specialistRepository.findById(id).orElseThrow();

    }

    // ==========================================
    // 接口 3: 获取专家的可用排班
    // ==========================================
    @GetMapping("/{id}/schedules")
    public List<TimeSlot> getSchedules(@PathVariable("id") Long id) {
        return timeSlotRepository.findBySpecialistIdAndStatus(id, TimeSlotStatus.AVAILABLE);

    }

    // ==========================================
    // 接口 4: 专家查看自己的已预约课表
    // ==========================================
    @GetMapping("/{id}/booked-schedules")
    public ResponseEntity<?> getBookedSchedules(@PathVariable("id") Long specialistId) {
        try {
            // 使用 Service 里的内部 Record
            List<BookingService.BookedScheduleResponse> schedules = bookingService.getBookedSchedulesForSpecialist(specialistId);

            if (schedules.isEmpty()) {
                return ResponseEntity.ok(Map.of("message", "当前没有被预约的时间段", "data", schedules));
            }

            return ResponseEntity.ok(schedules);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}