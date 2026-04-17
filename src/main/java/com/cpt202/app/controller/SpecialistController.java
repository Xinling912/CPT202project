package com.cpt202.app.controller;

import com.cpt202.app.model.*;
import com.cpt202.app.repository.ExpertiseCategoryRepository;
import com.cpt202.app.repository.SpecialistProfileRepository;
import com.cpt202.app.repository.TimeSlotRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/specialists")
@CrossOrigin
public class SpecialistController {

    private final SpecialistProfileRepository specialistRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final ExpertiseCategoryRepository expertiseCategoryRepository;

    public SpecialistController(
            SpecialistProfileRepository specialistRepository,
            TimeSlotRepository timeSlotRepository,
            ExpertiseCategoryRepository expertiseCategoryRepository
    ) {
        this.specialistRepository = specialistRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.expertiseCategoryRepository = expertiseCategoryRepository;
    }

    /**
     * 专家大厅列表接口（支持搜索 + 筛选 + 分页）
     * 功能：
     * 1) keyword：按专家用户名模糊搜索（如 lisa -> 名字包含 lisa 的专家）
     * 2) expertiseId：按专业筛选
     * 3) level：按等级筛选（JUNIOR/SENIOR/EXPERT）
     * 4) date：按日期筛选“当天仍有 AVAILABLE 时段”的专家
     * 5) 多筛选条件可叠加，最终结果是“同时满足所有条件”的交集
     */
    @GetMapping
    public Map<String, Object> getSpecialists(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long expertiseId,
            @RequestParam(required = false) SpecialistLevel level,
            @RequestParam(required = false) String date) {

        Map<String, Object> response = new HashMap<>();
        Pageable pageable = PageRequest.of(page, size);
        LocalDate targetDate = (date == null || date.isBlank()) ? null : LocalDate.parse(date);

        // 基础条件：只显示 ACTIVE 的专家
        Specification<SpecialistProfile> spec = (root, query, cb) -> cb.equal(root.get("status"), SpecialistStatus.ACTIVE);

        if (keyword != null && !keyword.isBlank()) {
            String likePattern = "%" + keyword.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.join("user").get("username")), likePattern));
        }
        if (expertiseId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.join("expertise").get("id"), expertiseId));
        }
        if (level != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("level"), level));
        }
        if (targetDate != null) {
            // availability 条件：子查询 time_slot 表，要求该专家在指定日期有可预约时段
            spec = spec.and((root, query, cb) -> {
                var subquery = query.subquery(Long.class);
                var timeSlotRoot = subquery.from(TimeSlot.class);
                subquery.select(cb.literal(1L));
                subquery.where(
                        cb.equal(timeSlotRoot.get("specialist"), root),
                        cb.equal(timeSlotRoot.get("status"), TimeSlotStatus.AVAILABLE),
                        cb.equal(timeSlotRoot.get("slotDate"), targetDate)
                );
                return cb.exists(subquery);
            });
        }

        Page<SpecialistProfile> specialistPage = specialistRepository.findAll(spec, pageable);
        response.put("content", specialistPage.getContent());
        response.put("totalElements", specialistPage.getTotalElements());
        response.put("totalPages", specialistPage.getTotalPages());
        response.put("page", specialistPage.getNumber());
        response.put("size", specialistPage.getSize());
        return response;
    }

    /**
     * 获取单个专家详情
     * 功能：前端点击某个专家卡片后，进入详情页时拉取该专家完整资料。
     */
    @GetMapping("/{id}")
    public SpecialistProfile getSpecialistDetail(@PathVariable("id") Long id) {
        return specialistRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "ID为 " + id + " 的专家档案不存在"
                ));
    }

    /**
     * 获取筛选项字典
     * 功能：给前端 filter 下拉框提供“专业列表 + 等级列表”。
     */
    @GetMapping("/filters")
    public Map<String, Object> getFilterOptions() {
        Map<String, Object> response = new HashMap<>();

        List<Map<String, Object>> expertises = expertiseCategoryRepository.findAll().stream()
                .map(item -> Map.<String, Object>of(
                        "id", item.getId(),
                        "name", item.getName()
                ))
                .collect(Collectors.toList());

        List<String> levels = List.of(SpecialistLevel.values()).stream()
                .map(Enum::name)
                .collect(Collectors.toList());

        response.put("expertises", expertises);
        response.put("levels", levels);
        return response;
    }
}