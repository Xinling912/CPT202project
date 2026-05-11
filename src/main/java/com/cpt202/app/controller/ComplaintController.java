package com.cpt202.app.controller;

import com.cpt202.app.model.Complaint;
import com.cpt202.app.model.User;
import com.cpt202.app.service.ComplaintService;
import com.cpt202.app.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin
public class ComplaintController {

    private final ComplaintService complaintService;
    private final UserService userService;

    public ComplaintController(ComplaintService complaintService,
                               UserService userService) {
        this.complaintService = complaintService;
        this.userService = userService;
    }

    /**Get the ID of the currently logged-in user*/
    private Long getCurrentUserId(Authentication authentication) {
        String currentUsername = authentication.getName();
        User currentUser = userService.getByUsername(currentUsername);
        return currentUser.getId();
    }

    /**User reports a booking*/
    @PostMapping("/report")
    public ResponseEntity<?> reportBooking(
            Authentication authentication,
            @RequestParam Long bookingId,
            @RequestParam String reason) {

        try {
            Long userId = getCurrentUserId(authentication);
            Complaint complaint = complaintService.reportBooking(bookingId, userId, reason);
            return ResponseEntity.ok(Map.of(
                    "message", "Report submitted, an administrator will process it as soon as possible",
                    "complaintId", complaint.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**User: Get own complaint records (paginated)*/
    @GetMapping("/my")
    public ResponseEntity<?> getMyComplaints(Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            List<Complaint> complaints = complaintService.getComplaintsByReporterId(userId);
            return ResponseEntity.ok(Map.of("data", complaints));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**Admin: Get all pending complaints*/
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPendingComplaints() {
        List<Complaint> complaints = complaintService.getPendingComplaints();
        return ResponseEntity.ok(Map.of("data", complaints));
    }

    /**Admin: Get all complaints*/
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllComplaints() {
        List<Complaint> complaints = complaintService.getAllComplaints();
        return ResponseEntity.ok(Map.of("data", complaints));
    }

    /**Admin: Dismiss complaint*/
    @PostMapping("/{complaintId}/dismiss")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> dismissComplaint(@PathVariable Long complaintId) {
        try {
            complaintService.dismissComplaint(complaintId);
            return ResponseEntity.ok(Map.of("message", "Complaint dismissed"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**Admin: Ban specialist (via complaint)*/
    @PostMapping("/{complaintId}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> banSpecialist(@PathVariable Long complaintId) {
        try {
            complaintService.banSpecialistByComplaint(complaintId);
            return ResponseEntity.ok(Map.of("message", "Specialist banned, complaint processed"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}