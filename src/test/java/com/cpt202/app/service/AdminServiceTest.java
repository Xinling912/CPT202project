package com.cpt202.app.service;

import com.cpt202.app.model.*;
import com.cpt202.app.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private SpecialistProfileRepository profileRepository;
    @Mock
    private SpecialistProfileEditRequestRepository editRequestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ExpertiseCategoryRepository expertiseRepository;

    @InjectMocks
    private AdminService adminService;

    private User testUser;
    private SpecialistProfile testProfile;
    private ExpertiseCategory existingExpertise;

    @BeforeEach
    void setUp() {
        // Prepare a basic user (starts as CUSTOMER)
        testUser = new User();
        testUser.setId(100L);
        testUser.setUsername("applicant1");
        testUser.setRole(UserRole.CUSTOMER);

        // Prepare an existing expertise category
        existingExpertise = new ExpertiseCategory();
        existingExpertise.setId(1L);
        existingExpertise.setName("Psychology");

        // Prepare a pending profile application
        testProfile = new SpecialistProfile();
        testProfile.setId(10L);
        testProfile.setUser(testUser);
        testProfile.setStatus(SpecialistStatus.PENDING);
    }

    // ==========================================
    // Module 1: New Specialist Application Tests
    // ==========================================

    @Test
    void approveNewSpecialist_WithExistingProposedExpertise_ShouldBindExistingAndApprove() {
        // Arrange: Applicant proposed "psychology" (case-insensitive duplicate)
        testProfile.setProposedExpertiseName("psyCHOlogy");

        when(profileRepository.findById(10L)).thenReturn(Optional.of(testProfile));
        // Mock DB returning the existing category
        when(expertiseRepository.findAll()).thenReturn(List.of(existingExpertise));

        // Act
        adminService.approveNewSpecialist(10L);

        // Assert
        assertEquals(existingExpertise, testProfile.getExpertise()); // Bound to existing!
        assertNull(testProfile.getProposedExpertiseName()); // Temp field cleared
        assertEquals(SpecialistStatus.ACTIVE, testProfile.getStatus()); // Status upgraded
        assertEquals(UserRole.SPECIALIST, testUser.getRole()); // Role upgraded

        verify(expertiseRepository, never()).save(any()); // IMPORTANT: Should NOT create a new duplicate entry
        verify(userRepository, times(1)).save(testUser);
        verify(profileRepository, times(1)).save(testProfile);
    }

    @Test
    void approveNewSpecialist_WithBrandNewProposedExpertise_ShouldCreateAndApprove() {
        // Arrange: Applicant proposed a completely new expertise
        testProfile.setProposedExpertiseName("Quantum Therapy");

        when(profileRepository.findById(10L)).thenReturn(Optional.of(testProfile));
        // DB only has "Psychology", doesn't match "Quantum Therapy"
        when(expertiseRepository.findAll()).thenReturn(List.of(existingExpertise));

        // Act
        adminService.approveNewSpecialist(10L);

        // Assert
        assertEquals("Quantum Therapy", testProfile.getExpertise().getName()); // Bound to newly created category
        assertEquals(SpecialistStatus.ACTIVE, testProfile.getStatus());
        assertEquals(UserRole.SPECIALIST, testUser.getRole());

        verify(expertiseRepository, times(1)).save(any(ExpertiseCategory.class)); // Must create new entry
    }

    @Test
    void rejectNewSpecialist_ShouldChangeStatusToRejected() {
        // Arrange
        when(profileRepository.findById(10L)).thenReturn(Optional.of(testProfile));

        // Act
        adminService.rejectNewSpecialist(10L);

        // Assert
        assertEquals(SpecialistStatus.REJECTED, testProfile.getStatus());
        assertEquals(UserRole.CUSTOMER, testUser.getRole()); // Role MUST remain CUSTOMER
        verify(profileRepository, times(1)).save(testProfile);
    }

    // ==========================================
    // Module 2: Edit Request Approval Tests
    // ==========================================

    @Test
    void approveEditRequest_HappyPath_ShouldCopyDataToMainProfile() {
        // Arrange: Setup a pending edit request (Shadow Table)
        SpecialistProfileEditRequest editReq = new SpecialistProfileEditRequest();
        editReq.setId(99L);
        editReq.setSpecialistProfile(testProfile);
        editReq.setNewRealName("Updated Name");
        editReq.setNewHourlyFee(new BigDecimal("250.00"));
        editReq.setNewExpertise(existingExpertise); // Selecting from existing dropdown
        editReq.setStatus(SpecialistProfileEditStatus.PENDING);

        when(editRequestRepository.findById(99L)).thenReturn(Optional.of(editReq));

        // Act
        adminService.approveEditRequest(99L);

        // Assert: Verify data was copied from shadow table to main profile
        assertEquals("Updated Name", testProfile.getRealName());
        assertEquals(new BigDecimal("250.00"), testProfile.getHourlyFee());
        assertEquals(existingExpertise, testProfile.getExpertise());
        assertEquals(SpecialistProfileEditStatus.APPROVED, editReq.getStatus()); // Request marked approved

        verify(profileRepository, times(1)).save(testProfile);
        verify(editRequestRepository, times(1)).save(editReq);
    }

    @Test
    void rejectEditRequest_ShouldOnlyUpdateShadowTable() {
        // Arrange
        SpecialistProfileEditRequest editReq = new SpecialistProfileEditRequest();
        editReq.setId(99L);
        editReq.setSpecialistProfile(testProfile);
        editReq.setStatus(SpecialistProfileEditStatus.PENDING);

        when(editRequestRepository.findById(99L)).thenReturn(Optional.of(editReq));

        // Act
        adminService.rejectEditRequest(99L);

        // Assert: Only the edit request status changes, profile remains untouched
        assertEquals(SpecialistProfileEditStatus.REJECTED, editReq.getStatus());
        verify(editRequestRepository, times(1)).save(editReq);
        verify(profileRepository, never()).save(any()); // Main table protected!
    }
}