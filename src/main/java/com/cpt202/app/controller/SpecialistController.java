package com.cpt202.app.controller;

import com.cpt202.app.model.*;
import com.cpt202.app.repository.SpecialistProfileRepository;
import com.cpt202.app.repository.TimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/specialists")
@CrossOrigin
public class SpecialistController {

    @Autowired(required = false) // 暂时设为 false 方便你跑 Mock 测试
    private SpecialistProfileRepository specialistRepository;

    @Autowired(required = false)
    private TimeSlotRepository timeSlotRepository;

    // ==========================================
    // 接口 1: 分页获取专家大厅列表
    // ==========================================
    @GetMapping
    public Map<String, Object> getSpecialists(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Map<String, Object> response = new HashMap<>();

        // --- 真实数据库调用 (连上数据库后解开) ---
        Pageable pageable = PageRequest.of(page, size);
        Page<SpecialistProfile> specialistPage = specialistRepository.findByStatus(SpecialistStatus.ACTIVE, pageable);
        response.put("content", specialistPage.getContent());
        response.put("totalElements", specialistPage.getTotalElements());
        response.put("totalPages", specialistPage.getTotalPages());
        return response;
        //--------------------------------------- */

//        // --- 基于新 Entity 的 Mock 数据 ---
//        List<SpecialistProfile> content = new ArrayList<>();
//
//        SpecialistProfile p = new SpecialistProfile();
//        p.setId(1L);
//        p.setLevel(SpecialistLevel.EXPERT);
//        p.setHourlyFee(new BigDecimal("500.00"));
//        p.setStatus(SpecialistStatus.ACTIVE);
//
//        // 关联 User (对应 User.java)
//        User u = new User();
//        u.setId(101L);
//        u.setUsername("张教授");
//        u.setEmail("zhang@xjtlu.edu.cn");
//        u.setRole(UserRole.SPECIALIST);
//        u.setCreatedAt(LocalDateTime.now());
//        p.setUser(u);
//
//        // 关联 ExpertiseCategory (对应 ExpertiseCategory.java)
//        ExpertiseCategory cat = new ExpertiseCategory();
//        cat.setId(10L);
//        cat.setName("人工智能");
//        p.setExpertise(cat);
//
//        content.add(p);
//        response.put("content", content);
//        response.put("totalElements", 1);
//        return response;
    }

    // ==========================================
    // 接口 2: 获取专家详细信息
    // ==========================================
    @GetMapping("/{id}")
    public SpecialistProfile getSpecialistDetail(@PathVariable("id") Long id) {
        // --- 真实数据库调用 ---
        return specialistRepository.findById(id).orElseThrow();
        //---------------------- */

//        // --- 严格匹配你实体类结构的 Mock 数据 ---
//        SpecialistProfile p = new SpecialistProfile();
//        p.setId(id);
//        p.setLevel(SpecialistLevel.SENIOR);
//        p.setHourlyFee(new BigDecimal("300.00"));
//        p.setStatus(SpecialistStatus.ACTIVE);
//
//        // 填充关联的 User 信息 (来自 User.java)
//        User u = new User();
//        u.setUsername("李博士");
//        u.setEmail("li@test.com");
//        p.setUser(u);
//
//        // 填充关联的专业分类 (来自 ExpertiseCategory.java)
//        ExpertiseCategory cat = new ExpertiseCategory();
//        cat.setName("前端工程化");
//        cat.setId(10L);
//        cat.setDescription("精通深度学习与计算机视觉");
//        p.setExpertise(cat);
//
//        return p;
    }

    // ==========================================
    // 接口 3: 获取专家的可用排班
    // ==========================================
    @GetMapping("/{id}/schedules")
    public List<TimeSlot> getSchedules(@PathVariable("id") Long id) {
        // --- 真实数据库调用 ---
        return timeSlotRepository.findBySpecialistIdAndStatus(id, TimeSlotStatus.AVAILABLE);
        //---------------------- */

//        List<TimeSlot> schedules = new ArrayList<>();
//        TimeSlot t = new TimeSlot();
//        t.setId(501L);
//        t.setSlotDate(LocalDate.of(2026, 4, 20));
//        t.setStartTime(LocalTime.of(14, 0));
//        t.setEndTime(LocalTime.of(15, 0));
//        t.setBooked(false);
//        schedules.add(t);
//        return schedules;
    }
}