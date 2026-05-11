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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecialistServiceTest {

    @Mock
    private SpecialistProfileRepository profileRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ExpertiseCategoryRepository expertiseRepository;
    @Mock
    private SpecialistProfileEditRequestRepository editRequestRepository;
    @Mock
    private SpecialistProfileRepository specialistProfileRepository; // Included since it's autowired separately in your service
    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private SpecialistService specialistService;

    private User testUser;
    private SpecialistProfile testProfile;
    private ExpertiseCategory testExpertise;

    @BeforeEach
    void setUp() {
        // Prepare common mock data
        testUser = new User();
        testUser.setId(100L);
        testUser.setUsername("testSpecialist");
        testUser.setRole(UserRole.SPECIALIST); // Assuming user is already assigned the role

        testExpertise = new ExpertiseCategory();
        testExpertise.setId(1L);
        testExpertise.setName("Psychology");

        testProfile = new SpecialistProfile();
        testProfile.setId(10L);
        testProfile.setUser(testUser);
        testProfile.setStatus(SpecialistStatus.ACTIVE);
    }

    // ==========================================
    // 1. Submit Profile Application Tests (Scenario A: New / Rejected)
    // ==========================================

    @Test
    void submitProfileApplication_NewApplication_ShouldSaveToProfileRepo() {
        // Arrange: User exists, but has no existing profile
        SpecialistService.SpecialistApplyRequest request = new SpecialistService.SpecialistApplyRequest(
                "John Doe", SpecialistLevel.EXPERT, new BigDecimal("100"), "My Resume", 1L, null
        );

        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUser(testUser)).thenReturn(Optional.empty()); // No profile yet
        when(expertiseRepository.findById(1L)).thenReturn(Optional.of(testExpertise));

        // Act
        specialistService.submitProfileApplication(testUser.getUsername(), request);

        // Assert: It should create a new profile and save it to the main profile repository
        verify(profileRepository, times(1)).save(any(SpecialistProfile.class));
        verify(editRequestRepository, never()).save(any()); // Should NOT touch the shadow table
    }

    @Test
    void submitProfileApplication_PendingApplicationExists_ShouldThrowException() {
        // Arrange: Profile exists and is currently PENDING
        testProfile.setStatus(SpecialistStatus.PENDING);
        SpecialistService.SpecialistApplyRequest request = new SpecialistService.SpecialistApplyRequest(
                "John Doe", SpecialistLevel.EXPERT, new BigDecimal("100"), "My Resume", 1L, null
        );

        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUser(testUser)).thenReturn(Optional.of(testProfile));

        // Act & Assert
        Exception e = assertThrows(RuntimeException.class, () -> {
            specialistService.submitProfileApplication(testUser.getUsername(), request);
        });
        assertTrue(e.getMessage().contains("You already have a specialist application pending review"));
    }

    // ==========================================
    // 2. Submit Profile Edit Tests (Scenario B: Active Specialist)
    // ==========================================

    @Test
    void submitProfileApplication_ActiveSpecialist_ShouldSaveToShadowTable() {
        // Arrange: Active profile exists, wants to edit information
        testProfile.setStatus(SpecialistStatus.ACTIVE);
        SpecialistService.SpecialistApplyRequest request = new SpecialistService.SpecialistApplyRequest(
                "John New Name", SpecialistLevel.SENIOR, new BigDecimal("150"), "Updated Resume", 1L, null
        );

        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUser(testUser)).thenReturn(Optional.of(testProfile));
        // No pending edit requests exist
        when(editRequestRepository.existsBySpecialistProfileAndStatus(testProfile, SpecialistProfileEditStatus.PENDING)).thenReturn(false);
        when(expertiseRepository.findById(1L)).thenReturn(Optional.of(testExpertise));

        // Act
        specialistService.submitProfileApplication(testUser.getUsername(), request);

        // Assert: Main profile should NOT be modified directly. A new EditRequest must be saved.
        verify(profileRepository, never()).save(any(SpecialistProfile.class));
        verify(editRequestRepository, times(1)).save(any(SpecialistProfileEditRequest.class));
    }

    @Test
    void submitProfileApplication_ActiveSpecialistWithPendingEdit_ShouldThrowException() {
        // Arrange: Active profile exists, but already has a pending edit request
        testProfile.setStatus(SpecialistStatus.ACTIVE);
        SpecialistService.SpecialistApplyRequest request = new SpecialistService.SpecialistApplyRequest(
                "Spamming Edit", SpecialistLevel.SENIOR, new BigDecimal("150"), "Updated Resume", 1L, null
        );

        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUser(testUser)).thenReturn(Optional.of(testProfile));
        // Mock that a pending edit request already exists
        when(editRequestRepository.existsBySpecialistProfileAndStatus(testProfile, SpecialistProfileEditStatus.PENDING)).thenReturn(true);

        // Act & Assert
        Exception e = assertThrows(RuntimeException.class, () -> {
            specialistService.submitProfileApplication(testUser.getUsername(), request);
        });
        assertTrue(e.getMessage().contains("You already have a modification request pending review"));
    }

    // ==========================================
    // 3. Get Total Earnings Tests (Financial Logic)
    // ==========================================

    @Test
    void getTotalEarnings_HappyPath_ShouldReturnCorrectAmount() {
        // Arrange: Active specialist with completed bookings
        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUser(testUser)).thenReturn(Optional.of(testProfile));

        // Mock the sum calculation from the database. Let's say they earned 550.556
        BigDecimal mockEarnings = new BigDecimal("550.556");
        when(bookingRepository.sumTotalAmountBySpecialistIdAndStatus(testProfile.getId(), BookingStatus.COMPLETED))
                .thenReturn(mockEarnings);

        // Act
        BigDecimal result = specialistService.getTotalEarnings(testUser.getUsername());

        // Assert: System must round the amount to 2 decimal places (550.56)
        assertEquals(new BigDecimal("550.56"), result);
    }

    @Test
    void getTotalEarnings_NotASpecialist_ShouldThrowException() {
        // Arrange: A normal customer tries to access earnings
        User customer = new User();
        customer.setUsername("customer1");
        customer.setRole(UserRole.CUSTOMER); // Wrong role

        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(customer));

        // Act & Assert
        Exception e = assertThrows(RuntimeException.class, () -> {
            specialistService.getTotalEarnings("customer1");
        });
        assertEquals("Only specialists can view earnings", e.getMessage());
    }

    // ==========================================
    // 4. Application Status Verification Test
    // ==========================================
    @Test
    void getCurrentApplicationStatus_NeverApplied_ShouldReturnNone() {
        // Arrange
        when(specialistProfileRepository.findByUser(testUser)).thenReturn(Optional.empty());

        // Act
        ApplicationStatus status = specialistService.getCurrentApplicationStatus(testUser);

        // Assert
        assertEquals(ApplicationStatus.NONE, status);
    }
}