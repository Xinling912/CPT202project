package com.cpt202.app.integration;

import com.cpt202.app.model.*;
import com.cpt202.app.repository.*;
import com.cpt202.app.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ProfileUpdateIntegrationTest {

    @Autowired
    private AdminService adminService;
    @Autowired
    private SpecialistProfileRepository profileRepository;
    @Autowired
    private SpecialistProfileEditRequestRepository editRequestRepository;
    @Autowired
    private UserRepository userRepository;

    private SpecialistProfile activeSpecialist;

    @BeforeEach
    void setUp() {
        // Initialize an already active specialist
        User specUser = new User();
        specUser.setUsername("veteran_doctor");
        specUser.setEmail("veteran@test.com");
        specUser.setPassword("password123");
        specUser.setRole(UserRole.SPECIALIST);
        specUser.setCreatedAt(LocalDateTime.now());
        userRepository.save(specUser);

        activeSpecialist = new SpecialistProfile();
        activeSpecialist.setUser(specUser);
        activeSpecialist.setStatus(SpecialistStatus.ACTIVE);
        activeSpecialist.setHourlyFee(new BigDecimal("100.00"));
        activeSpecialist.setLevel(SpecialistLevel.JUNIOR);
        activeSpecialist.setRealName("Old Name");
        profileRepository.save(activeSpecialist);
    }

    @Test
    void profileUpdateApprovalFlow_ShouldMergeShadowDataToMainTable() {
        // --- STEP 1: Specialist submits an edit request (Shadow Table Creation) ---
        SpecialistProfileEditRequest editRequest = new SpecialistProfileEditRequest();
        editRequest.setSpecialistProfile(activeSpecialist);
        editRequest.setNewRealName("New Upgraded Name");
        editRequest.setNewHourlyFee(new BigDecimal("300.00"));
        editRequest.setNewLevel(SpecialistLevel.EXPERT);
        editRequest.setStatus(SpecialistProfileEditStatus.PENDING);
        editRequestRepository.save(editRequest);

        // Pre-verification: Main table remains unchanged while pending
        assertEquals("Old Name", activeSpecialist.getRealName());

        // --- STEP 2: Admin approves the edit request ---
        adminService.approveEditRequest(editRequest.getId());

        // --- STEP 3: Verification ---
        SpecialistProfile updatedProfile = profileRepository.findById(activeSpecialist.getId()).orElseThrow();
        SpecialistProfileEditRequest resolvedRequest = editRequestRepository.findById(editRequest.getId()).orElseThrow();

        // 1. The edit request status should be APPROVED
        assertEquals(SpecialistProfileEditStatus.APPROVED, resolvedRequest.getStatus());

        // 2. The main profile must be updated with the shadow table's data
        assertEquals("New Upgraded Name", updatedProfile.getRealName());
        assertEquals(new BigDecimal("300.00"), updatedProfile.getHourlyFee());
        assertEquals(SpecialistLevel.EXPERT, updatedProfile.getLevel());

        // 3. The specialist status must remain ACTIVE
        assertEquals(SpecialistStatus.ACTIVE, updatedProfile.getStatus());
    }
}