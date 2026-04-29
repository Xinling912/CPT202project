package com.cpt202.app.service;

import com.cpt202.app.model.*;
import com.cpt202.app.repository.BookingRepository;
import com.cpt202.app.repository.SpecialistProfileRepository;
import com.cpt202.app.repository.TimeSlotRepository;
import com.cpt202.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private TimeSlotRepository timeSlotRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SpecialistProfileRepository specialistRepository;

    @InjectMocks
    private BookingService bookingService;

    // Common test data
    private User customer;
    private User specialistUser;
    private SpecialistProfile specialistProfile;
    private TimeSlot validSlot;

    // Prepare basic mock data before executing each test method
    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId(100L);
        customer.setUsername("customer@test.com");
        customer.setRole(UserRole.CUSTOMER);

        specialistUser = new User();
        specialistUser.setId(200L);
        specialistUser.setUsername("specialist@test.com");
        specialistUser.setRole(UserRole.SPECIALIST);

        specialistProfile = new SpecialistProfile();
        specialistProfile.setId(10L);
        specialistProfile.setUser(specialistUser);
        specialistProfile.setHourlyFee(new BigDecimal("100.00")); // Hourly rate $100

        validSlot = new TimeSlot();
        validSlot.setId(1L);
        validSlot.setSlotDate(LocalDate.now().plusDays(2)); // Two days later
        validSlot.setStartTime(LocalTime.of(14, 0)); // 2:00 PM
        validSlot.setEndTime(LocalTime.of(16, 0));   // 4:00 PM (Duration: 2 hours)
        validSlot.setStatus(TimeSlotStatus.AVAILABLE);
    }

    // ==========================================
    // 1. Create Booking Tests
    // ==========================================
    @Test
    void createBooking_HappyPath_ShouldSucceed() {
        // Arrange
        when(userRepository.findByUsername(customer.getUsername())).thenReturn(Optional.of(customer));
        when(specialistRepository.findById(specialistProfile.getId())).thenReturn(Optional.of(specialistProfile));
        when(timeSlotRepository.findByIdWithLock(validSlot.getId())).thenReturn(Optional.of(validSlot));

        // Simulate bypassing any anti-spam/duplicate booking limits
        when(bookingRepository.existsByCustomerIdAndTimeSlotIdAndStatus(any(), any(), any())).thenReturn(false);
        when(bookingRepository.existsByCustomerIdAndTimeSlotIdAndStatusIn(any(), any(), any())).thenReturn(false);

        // Intercept the save method to return a booking with an ID for DTO conversion
        Booking savedBooking = new Booking();
        savedBooking.setId(999L);
        savedBooking.setCustomer(customer);
        savedBooking.setSpecialist(specialistProfile);
        savedBooking.setTimeSlot(validSlot);
        savedBooking.setStatus(BookingStatus.PENDING);
        savedBooking.setTotalAmount(new BigDecimal("200.00"));
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        // Act
        BookingService.BookingResponse response = bookingService.createBooking(
                customer.getUsername(), specialistProfile.getId(), validSlot.getId(), "Test Notes"
        );

        // Assert
        assertNotNull(response);
        assertEquals(999L, response.id());
        assertEquals(TimeSlotStatus.BOOKED, validSlot.getStatus()); // Verify the slot is marked as booked
        verify(timeSlotRepository, times(1)).save(validSlot);
    }

    @Test
    void createBooking_SlotAlreadyTaken_ShouldThrowException() {
        // Arrange
        validSlot.setStatus(TimeSlotStatus.BOOKED); // Intentionally set the time slot as already booked
        when(userRepository.findByUsername(customer.getUsername())).thenReturn(Optional.of(customer));
        when(specialistRepository.findById(specialistProfile.getId())).thenReturn(Optional.of(specialistProfile));
        when(timeSlotRepository.findByIdWithLock(validSlot.getId())).thenReturn(Optional.of(validSlot));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            bookingService.createBooking(customer.getUsername(), specialistProfile.getId(), validSlot.getId(), "Notes");
        }, "ERROR_SLOT_TAKEN");
    }

    // ==========================================
    // 2. Cancel Booking Tests
    // ==========================================
    @Test
    void cancelBooking_CustomerLessThan24Hours_ShouldThrowException() {
        // Arrange
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setCustomer(customer);
        booking.setStatus(BookingStatus.CONFIRMED);

        // Set the appointment time to 2 hours later (less than the 24-hour limit)
        TimeSlot urgentSlot = new TimeSlot();
        urgentSlot.setSlotDate(LocalDate.now());
        urgentSlot.setStartTime(LocalTime.now().plusHours(2));
        booking.setTimeSlot(urgentSlot);

        when(userRepository.findByUsername(customer.getUsername())).thenReturn(Optional.of(customer));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        // Act & Assert
        Exception e = assertThrows(IllegalStateException.class, () -> {
            bookingService.cancelBooking(1L, "Emergency", customer.getUsername());
        });
        assertTrue(e.getMessage().contains("24 hours"));
    }

    @Test
    void cancelBooking_SpecialistCancels_ShouldDisableSlot() {
        // Arrange: Specialist proactively cancels, the slot should be directly disabled
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setCustomer(customer);
        booking.setSpecialist(specialistProfile);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTimeSlot(validSlot); // validSlot is two days later

        when(userRepository.findByUsername(specialistUser.getUsername())).thenReturn(Optional.of(specialistUser));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        // Act
        bookingService.cancelBooking(1L, "Specialist is sick", specialistUser.getUsername());

        // Assert
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertTrue(booking.getNotes().contains("Specialist"));
        assertEquals(TimeSlotStatus.DISABLED, validSlot.getStatus()); // Verify the slot is disabled
        verify(timeSlotRepository, times(1)).save(validSlot);
    }

    // ==========================================
    // 3. Confirm Order Tests
    // ==========================================
    @Test
    void confirmOrder_HappyPath_ShouldSucceed() {
        // Arrange
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setSpecialist(specialistProfile);
        booking.setStatus(BookingStatus.PENDING); // Status must be PENDING to be confirmed
        booking.setTimeSlot(validSlot);

        // Simulate the process of finding user and profile by email
        when(userRepository.findByUsername(specialistUser.getUsername())).thenReturn(Optional.of(specialistUser));
        when(specialistRepository.findByUser(specialistUser)).thenReturn(Optional.of(specialistProfile));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        // Act
        bookingService.confirmOrder(1L, specialistUser.getUsername());

        // Assert
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        verify(bookingRepository, times(1)).save(booking);
    }

    // ==========================================
    // 4. Complete Order Tests
    // ==========================================
    @Test
    void completeOrder_HappyPath_ShouldSucceed() {
        // Arrange
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setSpecialist(specialistProfile);
        booking.setStatus(BookingStatus.CONFIRMED); // Status must be CONFIRMED to be completed

        // Set the time to yesterday (ensuring the appointment has definitely started/passed)
        TimeSlot pastSlot = new TimeSlot();
        pastSlot.setSlotDate(LocalDate.now().minusDays(1));
        pastSlot.setStartTime(LocalTime.of(10, 0));
        booking.setTimeSlot(pastSlot);

        when(userRepository.findByUsername(specialistUser.getUsername())).thenReturn(Optional.of(specialistUser));
        when(specialistRepository.findByUser(specialistUser)).thenReturn(Optional.of(specialistProfile));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        // Act
        bookingService.completeOrder(1L, specialistUser.getUsername());

        // Assert
        assertEquals(BookingStatus.COMPLETED, booking.getStatus());
        verify(bookingRepository, times(1)).save(booking);
    }

    // ==========================================
    // 5. Get Orders Tests
    // ==========================================
    @Test
    void getOrdersByCustomerResponse_ShouldReturnList() {
        // Arrange
        Booking booking = new Booking();
        booking.setId(88L);
        booking.setCustomer(customer);
        booking.setSpecialist(specialistProfile);
        booking.setTimeSlot(validSlot);
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setTotalAmount(new BigDecimal("150.00"));

        when(userRepository.findByUsername(customer.getUsername())).thenReturn(Optional.of(customer));
        when(bookingRepository.findByCustomerId(customer.getId())).thenReturn(List.of(booking));

        // Act
        List<BookingService.BookingResponse> responses = bookingService.getOrdersByCustomerResponse(customer.getUsername());

        // Assert
        assertEquals(1, responses.size());
        assertEquals(88L, responses.get(0).id());
        assertEquals("COMPLETED", responses.get(0).status());
    }

    // ==========================================
    // 6. Get Specialist Orders Tests
    // ==========================================
    @Test
    void getSpecialistOrders_ShouldReturnList() {
        // Arrange
        Booking booking = new Booking();
        booking.setId(77L);
        booking.setCustomer(customer);
        booking.setSpecialist(specialistProfile);
        booking.setTimeSlot(validSlot);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(new BigDecimal("100.00"));

        // Simulate finding the specialist by username, retrieving their profile, and finding orders under this specialist
        when(userRepository.findByUsername(specialistUser.getUsername())).thenReturn(Optional.of(specialistUser));
        when(specialistRepository.findByUser(specialistUser)).thenReturn(Optional.of(specialistProfile));
        when(bookingRepository.findBySpecialistId(specialistProfile.getId())).thenReturn(List.of(booking));

        // Act
        List<BookingService.BookingResponse> responses = bookingService.getSpecialistOrders(specialistUser.getUsername());

        // Assert
        assertEquals(1, responses.size()); // Ensure 1 record is returned
        assertEquals(77L, responses.get(0).id()); // Verify the order ID is correct
        assertEquals("PENDING", responses.get(0).status()); // Verify the status is correct
    }
}