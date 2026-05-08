package com.cpt202.app.controller;

import com.cpt202.app.repository.UserRepository;
import com.cpt202.app.service.SpecialistService;
import com.cpt202.app.service.SpecialistService.SpecialistApplyRequest;
import com.cpt202.app.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import com.cpt202.app.model.*;
import com.cpt202.app.repository.ExpertiseCategoryRepository;
import com.cpt202.app.repository.SpecialistProfileRepository;
import com.cpt202.app.repository.UserRepository;
import com.cpt202.app.repository.TimeSlotRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;


import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/specialists")
@CrossOrigin
public class SpecialistController {

    @Autowired
    private SpecialistService specialistService;
    @Autowired
    private UserService userService;

    private final SpecialistProfileRepository specialistRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final ExpertiseCategoryRepository expertiseCategoryRepository;
    private final UserRepository userRepository;

    public SpecialistController(
            SpecialistProfileRepository specialistRepository,
            TimeSlotRepository timeSlotRepository,
            ExpertiseCategoryRepository expertiseCategoryRepository,
            UserRepository userRepository
    ) {
        this.specialistRepository = specialistRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.expertiseCategoryRepository = expertiseCategoryRepository;
        this.userRepository = userRepository;
    }

    /**
     * Specialist hall list interface (supports search + filter + pagination)
     * Features:
     * 1) keyword: Fuzzy search by specialist username (e.g., lisa -> specialists whose name contains lisa)
     * 2) expertiseId: Filter by expertise
     * 3) level: Filter by level (JUNIOR/SENIOR/EXPERT)
     * 4) date: Filter specialists who still have "AVAILABLE time slots on the current day"
     * 5) Multiple filter conditions can be stacked, the final result is the intersection of "satisfying all conditions simultaneously"
     */
    @GetMapping
    public ResponseEntity<?> getSpecialists(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long expertiseId,
            @RequestParam(required = false) SpecialistLevel level,
            @RequestParam(required = false) String date) {

        Map<String, Object> response = new HashMap<>();
        Pageable pageable = PageRequest.of(page, size);
        LocalDate parsedDate = null;
        try {
            if (date != null && !date.isBlank()) {
                parsedDate = LocalDate.parse(date);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Incorrect date format, please enter yyyy-MM-dd"));
        }

        // Define a final variable for Lambda use
        final LocalDate finalTargetDate = parsedDate;

        // Only show ACTIVE specialists
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
        if (finalTargetDate != null) {
            // availability condition: subquery the time_slot table, requiring the specialist to have available time slots on the specified date
            spec = spec.and((root, query, cb) -> {
                var subquery = query.subquery(Long.class);
                var timeSlotRoot = subquery.from(TimeSlot.class);
                subquery.select(cb.literal(1L));
                subquery.where(
                        cb.equal(timeSlotRoot.get("specialist"), root),
                        cb.equal(timeSlotRoot.get("status"), TimeSlotStatus.AVAILABLE),
                        cb.equal(timeSlotRoot.get("slotDate"), finalTargetDate)
                );
                return cb.exists(subquery);
            });
        }

        Page<SpecialistProfile> specialistPage = specialistRepository.findAll(spec, pageable);
        // Assemble return data...
        response.put("content", specialistPage.getContent());
        response.put("totalElements", specialistPage.getTotalElements());
        response.put("totalPages", specialistPage.getTotalPages());
        response.put("page", specialistPage.getNumber());
        response.put("size", specialistPage.getSize());

        return ResponseEntity.ok(response);
    }

    /**
     * Get details of a single specialist
     * Feature: When the frontend clicks on a specialist card, it fetches the complete profile of the specialist upon entering the detail page.
     */
    @GetMapping("/{id}")
    public SpecialistProfile getSpecialistDetail(@PathVariable("id") Long id) {
        return specialistRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Specialist profile with ID " + id + " does not exist"
                ));
    }

    /**
     * Get filter options dictionary
     * Feature: Provide "expertise list + level list" for the frontend filter dropdowns.
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

    @PostMapping("/apply")
    public ResponseEntity<?> submitProfile(@RequestBody SpecialistApplyRequest request, Authentication auth) {
        try {
            // Check that real name cannot be empty
            if (request.realName() == null || request.realName().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Real name cannot be empty"));
            }

            specialistService.submitProfileApplication(auth.getName(), request);

            // Return in JSON format
            return ResponseEntity.ok(Map.of("message", "Personal information submitted successfully, please wait for admin approval."));
        } catch (Exception e) {
            // Return error message in JSON format
            return ResponseEntity.badRequest().body(Map.of("error", "Submission failed: " + e.getMessage()));
        }
    }

    /**
     * Get total accumulated earnings of a specialist
     */
    @GetMapping("/earnings")
    public ResponseEntity<Map<String, Object>> getTotalEarnings(Authentication auth) {
        try {
            String username = auth.getName();

            // 1. Get user information and verify role
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User does not exist"));

            if (user.getRole() != UserRole.SPECIALIST) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Only specialists can view earnings"));
            }

            // 2. Get earnings
            BigDecimal earnings = specialistService.getTotalEarnings(username);

            return ResponseEntity.ok(Map.of(
                    "totalEarnings", earnings,
                    "currency", "CNY"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get personal information of the currently logged-in specialist (used for the "My Profile" page on the specialist side)
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getMyProfile(Authentication authentication) {
        try {
            String username = authentication.getName();

            // 1. Get user information and verify role
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User does not exist"));

            if (user.getRole() != UserRole.SPECIALIST) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Only specialists can view their own profile"));
            }

            // 2. Get specialist profile
            SpecialistProfile profile = specialistRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("Specialist profile not found, please submit a specialist application first"));

            return ResponseEntity.ok(Map.of("data", profile));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/apply-status")
    public ResponseEntity<?> getMyApplicationStatus(Authentication auth) {
        String username = auth.getName();
        User user = userService.getByUsername(username);

        ApplicationStatus status = specialistService.getCurrentApplicationStatus(user);

        return ResponseEntity.ok(Map.of(
                "status", status.name(),
                "message", getStatusMessage(status)
        ));
    }

    private String getStatusMessage(ApplicationStatus status) {
        switch (status) {
            case NONE:
                return "You have not applied to be a specialist yet";
            case APPLY_PENDING:
                return "Specialist application is under review, please wait patiently";
            case APPLY_REJECTED:
                return "Specialist application rejected, you can modify and resubmit";
            case IS_ACTIVE_SPECIALIST:
                return "You are already an official specialist";
            case EDIT_PENDING:
                return "Profile modification application is under review";
            case EDIT_APPROVED:
                return "Profile modification approved";
            case EDIT_REJECTED:
                return "Profile modification rejected, can be resubmitted";
            default:
                return "";
        }
    }
}