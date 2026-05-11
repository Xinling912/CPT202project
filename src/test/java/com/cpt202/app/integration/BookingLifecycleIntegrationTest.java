package com.cpt202.app.integration;

import com.cpt202.app.model.*;
import com.cpt202.app.repository.BookingRepository;
import com.cpt202.app.repository.SpecialistProfileRepository;
import com.cpt202.app.repository.TimeSlotRepository;
import com.cpt202.app.repository.UserRepository;
import com.cpt202.app.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class BookingLifecycleIntegrationTest {

    @Autowired
    private BookingService bookingService;
    @Autowired
    private TimeSlotRepository timeSlotRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SpecialistProfileRepository profileRepository;

    private User customer;
    private SpecialistProfile specialist;
    private TimeSlot availableSlot;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setUsername("client_user");
        customer.setEmail("client@test.com");
        customer.setPassword("password123");
        customer.setRole(UserRole.CUSTOMER);
        customer.setCreatedAt(LocalDateTime.now());
        userRepository.save(customer);

        User specUser = new User();
        specUser.setUsername("doctor_user");
        specUser.setEmail("doctor@test.com");
        specUser.setPassword("password123");
        specUser.setRole(UserRole.SPECIALIST);
        specUser.setCreatedAt(LocalDateTime.now());
        userRepository.save(specUser);

        specialist = new SpecialistProfile();
        specialist.setUser(specUser);
        specialist.setStatus(SpecialistStatus.ACTIVE);
        specialist.setHourlyFee(new java.math.BigDecimal("100.00"));
        specialist.setRealName("Dr. House");
        specialist.setLevel(SpecialistLevel.EXPERT);

        profileRepository.save(specialist);

        TimeSlot slot = new TimeSlot();
        slot.setSpecialist(specialist);
        slot.setSlotDate(LocalDate.now().plusDays(3));
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setStatus(TimeSlotStatus.AVAILABLE);
        availableSlot = timeSlotRepository.save(slot);
    }

    @Test
    void completeBookingFlow_FromCreateToComplete() {
        // --- STEP 1: Customer creates a booking ---
        BookingService.BookingResponse newBookingResponse = bookingService.createBooking(
                customer.getUsername(),
                specialist.getId(),
                availableSlot.getId(),
                "Need help"
        );

        assertNotNull(newBookingResponse.id());
        assertEquals("PENDING", newBookingResponse.status()); // DTO 中的 status 是 String

        // CRITICAL CHECK: The TimeSlot MUST be locked as BOOKED to prevent overbooking!
        TimeSlot lockedSlot = timeSlotRepository.findById(availableSlot.getId()).orElseThrow();
        assertEquals(TimeSlotStatus.BOOKED, lockedSlot.getStatus());

        // --- STEP 2: Specialist confirms the order ---
        bookingService.confirmOrder(newBookingResponse.id(), specialist.getUser().getUsername());

        Booking confirmedBooking = bookingRepository.findById(newBookingResponse.id()).orElseThrow();
        assertEquals(BookingStatus.CONFIRMED, confirmedBooking.getStatus());

        TimeSlot timeTravelSlot = timeSlotRepository.findById(availableSlot.getId()).orElseThrow();
        timeTravelSlot.setSlotDate(LocalDate.now().minusDays(1));
        timeSlotRepository.save(timeTravelSlot);

        // --- STEP 3: Specialist completes the service ---
        bookingService.completeOrder(newBookingResponse.id(), specialist.getUser().getUsername());

        Booking completedBooking = bookingRepository.findById(newBookingResponse.id()).orElseThrow();
        assertEquals(BookingStatus.COMPLETED, completedBooking.getStatus());
    }
}