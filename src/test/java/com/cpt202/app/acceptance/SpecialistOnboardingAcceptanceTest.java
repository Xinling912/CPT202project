package com.cpt202.app.acceptance;

import com.cpt202.app.model.*;
import com.cpt202.app.service.SpecialistService.SpecialistApplyRequest;
import com.cpt202.app.service.SpecialistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

public class SpecialistOnboardingAcceptanceTest extends BaseAcceptanceTest {

    @Autowired
    private SpecialistService specialistService; // Injecting service to simulate real business logic

    /**
     * Feature: Specialist Lifecycle
     * Scenario: Validates the transition from a new applicant to an active specialist,
     * and the subsequent "Shadow Table" logic for profile updates.
     */
    @Test
    void testSpecialistLifecycle_FromNewApplyToEditRequest() {
        // 1. [Setup] Create a standard Customer user
        User user = createTestUser("applicant_01", UserRole.CUSTOMER);

        // 2. [Scenario A] Initial Application: Verify that submitProfileApplication creates a PENDING profile
        SpecialistApplyRequest applyReq = new SpecialistApplyRequest(
                "Real Name",
                SpecialistLevel.EXPERT,
                new BigDecimal("150.00"),
                "My Resume",
                null,
                "New Category" // Simulating a custom expertise entry
        );

        specialistService.submitProfileApplication(user.getUsername(), applyReq);

        SpecialistProfile profile = specialistRepository.findByUser(user)
                .orElseThrow(() -> new AssertionError("Specialist profile was not created"));

        // Assertions for initial application
        assertEquals(SpecialistStatus.PENDING, profile.getStatus());
        assertEquals("New Category", profile.getProposedExpertiseName()); // Verify custom expertise is held in the temp field

        // 3. [Mock Admin Approval] Transition the specialist to ACTIVE status
        profile.setStatus(SpecialistStatus.ACTIVE);
        user.setRole(UserRole.SPECIALIST); // Synchronize user role with profile status
        specialistRepository.save(profile);
        userRepository.save(user);

        // 4. [Scenario B] Profile Update: Verify that active specialists trigger an EditRequest (Shadow Table)
        SpecialistApplyRequest editReq = new SpecialistApplyRequest(
                "Updated Real Name",
                SpecialistLevel.EXPERT,
                new BigDecimal("200.00"),
                "Updated Resume",
                null,
                null
        );

        specialistService.submitProfileApplication(user.getUsername(), editReq);

        // Verification: The main profile should NOT change immediately (Shadow Table rule)
        SpecialistProfile mainProfile = specialistRepository.findByUser(user).get();
        assertEquals("Real Name", mainProfile.getRealName());

        // Verification: A PENDING EditRequest must be generated for admin review
        boolean hasEditRequest = editRequestRepository.existsBySpecialistProfileAndStatus(
                mainProfile,
                SpecialistProfileEditStatus.PENDING
        );
        assertTrue(hasEditRequest, "A pending edit request should have been created in the shadow table");
    }
}