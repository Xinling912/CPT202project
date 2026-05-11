package com.cpt202.app.integration;

import com.cpt202.app.model.*;
import com.cpt202.app.repository.SpecialistProfileRepository;
import com.cpt202.app.repository.UserRepository;
import com.cpt202.app.service.AdminService;
import com.cpt202.app.service.SpecialistService;
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
class SpecialistOnboardingIntegrationTest {

    @Autowired
    private SpecialistService specialistService;
    @Autowired
    private AdminService adminService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SpecialistProfileRepository profileRepository;

    private User customer;

    @BeforeEach
    void setUp() {
        // Prepare a pure CUSTOMER in the database with the required creation timestamp
        customer = new User();
        customer.setUsername("future_specialist");
        customer.setEmail("future@test.com");
        customer.setPassword("Pass1234");
        customer.setRole(UserRole.CUSTOMER);
        customer.setCreatedAt(LocalDateTime.now()); // Prevent DataIntegrityViolationException
        userRepository.save(customer);
    }

    @Test
    void completeOnboardingFlow_FromApplyToApprove() {
        // --- STEP 1: Customer submits an application to become a specialist ---
        SpecialistService.SpecialistApplyRequest request = new SpecialistService.SpecialistApplyRequest(
                "John Doe", SpecialistLevel.EXPERT, new BigDecimal("500.0"), "My Resume", null, "AI Consultant"
        );
        specialistService.submitProfileApplication(customer.getUsername(), request);

        // Verify Step 1: The profile should be created in PENDING status, and the user role remains CUSTOMER
        SpecialistProfile pendingProfile = profileRepository.findByUser(customer).orElseThrow();
        assertEquals(SpecialistStatus.PENDING, pendingProfile.getStatus());
        assertEquals(UserRole.CUSTOMER, pendingProfile.getUser().getRole());

        // --- STEP 2: Admin approves the application ---
        // Using the exact method name found in your AdminService
        adminService.approveNewSpecialist(pendingProfile.getId());

        // --- STEP 3: Final Verification (Cross-Repository Check) ---
        User updatedUser = userRepository.findById(customer.getId()).orElseThrow();
        SpecialistProfile activeProfile = profileRepository.findById(pendingProfile.getId()).orElseThrow();

        // 1. The specialist profile must be activated
        assertEquals(SpecialistStatus.ACTIVE, activeProfile.getStatus());

        // 2. CRITICAL: The user's role MUST be upgraded to SPECIALIST
        assertEquals(UserRole.SPECIALIST, updatedUser.getRole());

        // 3. Verify that the custom expertise category was successfully created and assigned
        assertEquals("AI Consultant", activeProfile.getExpertise().getName());
    }
}