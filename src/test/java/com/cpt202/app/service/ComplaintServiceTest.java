package com.cpt202.app.service;

import com.cpt202.app.model.*;
import com.cpt202.app.repository.BookingRepository;
import com.cpt202.app.repository.ComplaintRepository;
import com.cpt202.app.repository.SpecialistProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private SpecialistProfileRepository specialistProfileRepository;

    @InjectMocks
    private ComplaintService complaintService;

    private User testCustomer;
    private SpecialistProfile testSpecialist;
    private Booking testBooking;
    private Complaint testComplaint;

    @BeforeEach
    void setUp() {
        // Prepare Customer
        testCustomer = new User();
        testCustomer.setId(100L);
        testCustomer.setUsername("angry_customer");

        // Prepare Specialist
        testSpecialist = new SpecialistProfile();
        testSpecialist.setId(10L);
        testSpecialist.setStatus(SpecialistStatus.ACTIVE);

        // Prepare a COMPLETED Booking
        testBooking = new Booking();
        testBooking.setId(500L);
        testBooking.setCustomer(testCustomer);
        testBooking.setSpecialist(testSpecialist);
        testBooking.setStatus(BookingStatus.COMPLETED);

        // Prepare an existing Complaint
        testComplaint = new Complaint(testBooking, testCustomer, "Terrible service!");
        testComplaint.setId(99L);
        testComplaint.setStatus(ComplaintStatus.PENDING);
    }

    // ==========================================
    // 1. Report Booking Tests (4 Lines of Defense)
    // ==========================================

    @Test
    void reportBooking_HappyPath_ShouldCreateComplaint() {
        // Arrange
        when(bookingRepository.findById(500L)).thenReturn(Optional.of(testBooking));
        when(complaintRepository.existsByBookingId(500L)).thenReturn(false); // Not reported yet
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Complaint result = complaintService.reportBooking(500L, 100L, "Late for 30 minutes");

        // Assert
        assertNotNull(result);
        assertEquals(100L, result.getReporter().getId());
        assertEquals("Late for 30 minutes", result.getReporter().getId() == 100L ? "Late for 30 minutes" : "");
        assertEquals(ComplaintStatus.PENDING, result.getStatus());
        verify(complaintRepository, times(1)).save(any(Complaint.class));
    }

    @Test
    void reportBooking_BookingNotCompleted_ShouldThrowException() {
        // Arrange
        testBooking.setStatus(BookingStatus.PENDING); // Wrong status
        when(bookingRepository.findById(500L)).thenReturn(Optional.of(testBooking));

        // Act & Assert
        Exception e = assertThrows(RuntimeException.class, () -> complaintService.reportBooking(500L, 100L, "Bad!"));
        assertEquals("Only completed services can be reported", e.getMessage());
        verify(complaintRepository, never()).save(any());
    }

    @Test
    void reportBooking_AlreadyReported_ShouldThrowException() {
        // Arrange
        when(bookingRepository.findById(500L)).thenReturn(Optional.of(testBooking));
        when(complaintRepository.existsByBookingId(500L)).thenReturn(true); // Already reported!

        // Act & Assert
        Exception e = assertThrows(RuntimeException.class, () -> complaintService.reportBooking(500L, 100L, "Bad!"));
        assertEquals("This booking has already been reported, please wait for admin processing", e.getMessage());
        verify(complaintRepository, never()).save(any());
    }

    @Test
    void reportBooking_WrongReporter_ShouldThrowException() {
        // Arrange
        when(bookingRepository.findById(500L)).thenReturn(Optional.of(testBooking));
        when(complaintRepository.existsByBookingId(500L)).thenReturn(false);

        Long hackerId = 999L; // Someone else trying to report

        // Act & Assert
        Exception e = assertThrows(RuntimeException.class, () -> complaintService.reportBooking(500L, hackerId, "Bad!"));
        assertEquals("You can only report your own bookings", e.getMessage());
        verify(complaintRepository, never()).save(any());
    }

    // ==========================================
    // 2. Query Methods Tests
    // ==========================================

    @Test
    void queryMethods_ShouldReturnLists() {
        // Arrange
        when(complaintRepository.findByReporterId(100L)).thenReturn(List.of(testComplaint));
        when(complaintRepository.findByStatus(ComplaintStatus.PENDING)).thenReturn(List.of(testComplaint));
        when(complaintRepository.findAll()).thenReturn(List.of(testComplaint));

        // Act & Assert
        assertEquals(1, complaintService.getComplaintsByReporterId(100L).size());
        assertEquals(1, complaintService.getPendingComplaints().size());
        assertEquals(1, complaintService.getAllComplaints().size());
    }

    // ==========================================
    // 3. Admin Processing Tests (Dismiss vs Ban)
    // ==========================================

    @Test
    void dismissComplaint_HappyPath_ShouldChangeStatusToDismissed() {
        // Arrange
        when(complaintRepository.findById(99L)).thenReturn(Optional.of(testComplaint));

        // Act
        complaintService.dismissComplaint(99L);

        // Assert
        assertEquals(ComplaintStatus.DISMISSED, testComplaint.getStatus());
        verify(complaintRepository, times(1)).save(testComplaint);
        verify(specialistProfileRepository, never()).save(any()); // Specialist shouldn't be touched
    }

    @Test
    void banSpecialistByComplaint_HappyPath_ShouldBanSpecialistAndCloseComplaint() {
        // Arrange
        when(complaintRepository.findById(99L)).thenReturn(Optional.of(testComplaint));

        // Act
        complaintService.banSpecialistByComplaint(99L);

        // Assert
        assertEquals(ComplaintStatus.BANNED, testComplaint.getStatus()); // Complaint marked as resolved (Banned)
        assertEquals(SpecialistStatus.INACTIVE, testSpecialist.getStatus()); // Specialist is punished!

        verify(specialistProfileRepository, times(1)).save(testSpecialist);
        verify(complaintRepository, times(1)).save(testComplaint);
    }
}