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

    /**
     * 获取当前登录用户的 ID
     */
    private Long getCurrentUserId(Authentication authentication) {
        String currentUsername = authentication.getName();
        User currentUser = userService.getByUsername(currentUsername);
        return currentUser.getId();
    }

    /**
     * 用户举报订单
     */
    @PostMapping("/report")
    public ResponseEntity<?> reportBooking(
            Authentication authentication,
            @RequestParam Long bookingId,
            @RequestParam String reason) {

        try {
            Long userId = getCurrentUserId(authentication);
            Complaint complaint = complaintService.reportBooking(bookingId, userId, reason);
            return ResponseEntity.ok(Map.of(
                    "message", "举报已提交，管理员会尽快处理",
                    "complaintId", complaint.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 用户：获取自己的举报记录（分页）
     */
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

    /**
     * 管理员：获取所有待处理举报
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPendingComplaints() {
        List<Complaint> complaints = complaintService.getPendingComplaints();
        return ResponseEntity.ok(Map.of("data", complaints));
    }

    /**
     * 管理员：获取所有举报
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllComplaints() {
        List<Complaint> complaints = complaintService.getAllComplaints();
        return ResponseEntity.ok(Map.of("data", complaints));
    }

    /**
     * 管理员：驳回举报
     */
    @PostMapping("/{complaintId}/dismiss")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> dismissComplaint(@PathVariable Long complaintId) {
        try {
            complaintService.dismissComplaint(complaintId);
            return ResponseEntity.ok(Map.of("message", "已驳回该举报"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 管理员：封禁专家（通过举报）
     */
    @PostMapping("/{complaintId}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> banSpecialist(@PathVariable Long complaintId) {
        try {
            complaintService.banSpecialistByComplaint(complaintId);
            return ResponseEntity.ok(Map.of("message", "已封禁该专家，举报已处理"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}