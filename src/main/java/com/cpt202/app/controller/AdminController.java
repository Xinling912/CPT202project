package com.cpt202.app.controller;

import com.cpt202.app.model.SpecialistProfile;
import com.cpt202.app.model.SpecialistProfileEditRequest;
import com.cpt202.app.model.SpecialistProfileEditStatus;
import com.cpt202.app.model.SpecialistStatus;
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

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
@PreAuthorize("hasRole('ADMIN')") // 💡 严格保安：只有管理员能访问这些接口
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private SpecialistProfileRepository profileRepository;

    @Autowired
    private SpecialistProfileEditRequestRepository editRequestRepository;


    // ==========================================
    // 1. 列表渲染接口 (供管理员 Dashboard 呈现表格用)
    // ==========================================

    /**
     * 获取所有【待审核的新专家申请】
     */
    @GetMapping("/applications/pending")
    public ResponseEntity<?> getPendingApplications() {
        // 查主表里状态为 PENDING 的记录
        List<SpecialistProfile> pendingProfiles = profileRepository.findByStatus(SpecialistStatus.PENDING);
        return ResponseEntity.ok(Map.of("data", pendingProfiles));
    }

    /**
     * 获取所有【待审核的老专家修改申请】
     */
    @GetMapping("/edits/pending")
    public ResponseEntity<?> getPendingEdits() {
        // 查影子表里状态为 PENDING 的记录
        List<SpecialistProfileEditRequest> pendingEdits = editRequestRepository.findByStatus(SpecialistProfileEditStatus.PENDING);
        return ResponseEntity.ok(Map.of("data", pendingEdits));
    }


    // ==========================================
    // 2. 审批操作接口 (同意 / 拒绝)
    // ==========================================

    /**
     * 同意【新专家入驻申请】
     */
    @PostMapping("/applications/{profileId}/approve")
    public ResponseEntity<?> approveApplication(@PathVariable Long profileId) {
        try {
            adminService.approveNewSpecialist(profileId);
            return ResponseEntity.ok(Map.of("message", "新专家入驻审核已通过！"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 拒绝【新专家入驻申请】
     */
    @PostMapping("/applications/{profileId}/reject")
    public ResponseEntity<?> rejectApplication(@PathVariable Long profileId) {
        try {
            adminService.rejectNewSpecialist(profileId);
            return ResponseEntity.ok(Map.of("message", "已驳回该专家的入驻申请！"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 同意【老专家资料修改】
     */
    @PostMapping("/edits/{requestId}/approve")
    public ResponseEntity<?> approveEditRequest(@PathVariable Long requestId) {
        try {
            adminService.approveEditRequest(requestId);
            return ResponseEntity.ok(Map.of("message", "专家资料修改已通过，并同步至大厅！"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 拒绝【老专家资料修改】
     */
    @PostMapping("/edits/{requestId}/reject")
    public ResponseEntity<?> rejectEditRequest(@PathVariable Long requestId) {
        try {
            adminService.rejectEditRequest(requestId);
            return ResponseEntity.ok(Map.of("message", "已驳回该专家的修改申请，其原资料保持不变。"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}