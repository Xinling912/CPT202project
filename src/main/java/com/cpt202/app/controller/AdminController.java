package com.cpt202.app.controller;

import com.cpt202.app.model.*;
import com.cpt202.app.repository.SpecialistProfileEditRequestRepository;
import com.cpt202.app.repository.SpecialistProfileRepository;
import com.cpt202.app.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
@PreAuthorize("hasRole('ADMIN')") // Only administrators can access
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private SpecialistProfileRepository profileRepository;

    @Autowired
    private SpecialistProfileEditRequestRepository editRequestRepository;



    // 1. List rendering endpoints (for presenting tables on the Admin Dashboard)

    /**Get all Pending New Specialist Applications*/
    @GetMapping("/applications/pending")
    public ResponseEntity<?> getPendingApplications() {
        // Query the main table for records with PENDING status
        List<SpecialistProfile> pendingProfiles = profileRepository.findByStatus(SpecialistStatus.PENDING);
        return ResponseEntity.ok(Map.of("data", pendingProfiles));
    }

    /**Get all Pending Edit Requests from Existing Specialists*/
    @GetMapping("/edits/pending")
    public ResponseEntity<?> getPendingEdits() {
        // Query the shadow table for records with PENDING status
        List<SpecialistProfileEditRequest> pendingEdits = editRequestRepository.findByStatus(SpecialistProfileEditStatus.PENDING);
        return ResponseEntity.ok(Map.of("data", pendingEdits));
    }



    // 2. Approval operation (Approve / Reject)

    /** Approve New Specialist Onboarding Application*/
    @PostMapping("/applications/{profileId}/approve")
    public ResponseEntity<?> approveApplication(@PathVariable Long profileId) {
        try {
            adminService.approveNewSpecialist(profileId);
            return ResponseEntity.ok(Map.of("message", "New specialist onboarding application approved!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**Reject New Specialist Onboarding Application*/
    @PostMapping("/applications/{profileId}/reject")
    public ResponseEntity<?> rejectApplication(@PathVariable Long profileId) {
        try {
            adminService.rejectNewSpecialist(profileId);
            return ResponseEntity.ok(Map.of("message", "The specialist's onboarding application has been rejected!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**Approve Existing Specialist Profile Edit*/
    @PostMapping("/edits/{requestId}/approve")
    public ResponseEntity<?> approveEditRequest(@PathVariable Long requestId) {
        try {
            adminService.approveEditRequest(requestId);
            return ResponseEntity.ok(Map.of("message", "Specialist profile edit approved and synchronized to the hall!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**Reject Existing Specialist Profile Edit*/
    @PostMapping("/edits/{requestId}/reject")
    public ResponseEntity<?> rejectEditRequest(@PathVariable Long requestId) {
        try {
            adminService.rejectEditRequest(requestId);
            return ResponseEntity.ok(Map.of("message", "The specialist's edit request has been rejected, their original profile remains unchanged."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 3. Specialist account management endpoints (Suspend / Activate)


    /**Get all Approved Specialists (including ACTIVE and INACTIVE), used for display on the adjustment page
     * Supports: Search (Username/Real Name/Expertise Name) + Expertise Filter + Level Filter*/
    @GetMapping("/specialists")
    public ResponseEntity<?> getAllExistingSpecialists(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long expertiseId,
            @RequestParam(required = false) SpecialistLevel level) {

        // Get all specialists, but filter out those still under review (PENDING) or rejected, keeping only official specialists
        List<SpecialistProfile> existingProfiles = profileRepository.findAll().stream()
                .filter(p -> p.getStatus() == SpecialistStatus.ACTIVE || p.getStatus() == SpecialistStatus.INACTIVE)
                .collect(Collectors.toList());

        // 1. Filter by expertise
        if (expertiseId != null) {
            existingProfiles = existingProfiles.stream()
                    .filter(p -> p.getExpertise() != null && p.getExpertise().getId().equals(expertiseId))
                    .collect(Collectors.toList());
        }

        // 2. Filter by level
        if (level != null) {
            existingProfiles = existingProfiles.stream()
                    .filter(p -> p.getLevel() == level)
                    .collect(Collectors.toList());
        }

        // 3. Search by keyword (Username / Real Name / Expertise Name)
        if (keyword != null && !keyword.isBlank()) {
            String lowerKeyword = keyword.toLowerCase().trim();
            existingProfiles = existingProfiles.stream()
                    .filter(p ->
                            // Search by username
                            (p.getUser().getUsername() != null &&
                                    p.getUser().getUsername().toLowerCase().contains(lowerKeyword)) ||
                                    // Search by real name
                                    (p.getRealName() != null &&
                                            p.getRealName().toLowerCase().contains(lowerKeyword)) ||
                                    // Search by expertise name
                                    (p.getExpertise() != null &&
                                            p.getExpertise().getName().toLowerCase().contains(lowerKeyword))
                    )
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(Map.of("data", existingProfiles));
    }

    /**Suspend specialist -> Status changed to INACTIVE*/
    @PostMapping("/specialists/{id}/suspend")
    public ResponseEntity<?> suspendSpecialist(@PathVariable Long id) {
        try {
            SpecialistProfile profile = profileRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Specialist profile not found"));

            profile.setStatus(SpecialistStatus.INACTIVE);
            profileRepository.save(profile);

            return ResponseEntity.ok(Map.of("message", "Specialist has been successfully suspended!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**Activate specialist -> Status changed to ACTIVE*/
    @PostMapping("/specialists/{id}/activate")
    public ResponseEntity<?> activateSpecialist(@PathVariable Long id) {
        try {
            SpecialistProfile profile = profileRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Specialist profile not found"));

            profile.setStatus(SpecialistStatus.ACTIVE);
            profileRepository.save(profile);

            return ResponseEntity.ok(Map.of("message", "Specialist has been successfully reactivated!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}