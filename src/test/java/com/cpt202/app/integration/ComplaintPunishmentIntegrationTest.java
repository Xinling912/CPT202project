package com.cpt202.app.integration;

import com.cpt202.app.model.*;
import com.cpt202.app.repository.*;
import com.cpt202.app.service.ComplaintService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional // Ensures data rolls back automatically after the test, keeping the database clean
class ComplaintPunishmentIntegrationTest {

    @Autowired
    private ComplaintService complaintService;
    @Autowired
    private ComplaintRepository complaintRepository;
    @Autowired
    private SpecialistProfileRepository profileRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private TimeSlotRepository timeSlotRepository;

    private User customer;
    private SpecialistProfile specialist;
    private Booking completedBooking;

    @BeforeEach
    void setUp() {
        // ==========================================
        // 1. Initialize the customer (All Not-Null constraints satisfied)
        // ==========================================
        customer = new User();
        customer.setUsername("angry_customer");
        customer.setEmail("angry@test.com"); // Prevent email null constraint violation
        customer.setPassword("password123"); // Prevent password null constraint violation
        customer.setRole(UserRole.CUSTOMER);
        customer.setCreatedAt(LocalDateTime.now()); // Prevent createdAt null constraint violation
        userRepository.save(customer);

        // ==========================================
        // 2. Initialize the specialist to be reported (All Not-Null constraints satisfied)
        // ==========================================
        User specUser = new User();
        specUser.setUsername("bad_doctor");
        specUser.setEmail("bad@test.com");
        specUser.setPassword("password123");
        specUser.setRole(UserRole.SPECIALIST);
        specUser.setCreatedAt(LocalDateTime.now());
        userRepository.save(specUser);

        specialist = new SpecialistProfile();
        specialist.setUser(specUser);
        specialist.setStatus(SpecialistStatus.ACTIVE);
        specialist.setHourlyFee(new BigDecimal("100.00")); // Prevent hourlyFee null constraint violation
        specialist.setRealName("Dr. Bad");
        specialist.setLevel(SpecialistLevel.EXPERT);
        profileRepository.save(specialist);

        // ==========================================
        // 3. Initialize a completed booking (Using Setters to avoid constructor issues)
        // ==========================================
        TimeSlot slot = new TimeSlot();
        slot.setSpecialist(specialist);
        // Set the booking time to yesterday to ensure logical validity
        slot.setSlotDate(LocalDate.now().minusDays(1));
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setStatus(TimeSlotStatus.BOOKED);
        TimeSlot savedSlot = timeSlotRepository.save(slot);

        completedBooking = new Booking();
        completedBooking.setCustomer(customer);
        completedBooking.setSpecialist(specialist); // Link specialist to maintain referential integrity
        completedBooking.setTimeSlot(savedSlot);
        completedBooking.setStatus(BookingStatus.COMPLETED); // Status must be COMPLETED to allow a complaint
        completedBooking.setTotalAmount(new BigDecimal("100.00")); // Prevent totalAmount null constraint violation
        completedBooking.setCreatedAt(LocalDateTime.now()); // Prevent createdAt null constraint violation
        bookingRepository.save(completedBooking);
    }

    @Test
    void banSpecialistFlow_ShouldCloseComplaintAndDeactivateProfile() {
        // --- STEP 1: Customer submits a report for the completed order ---
        Complaint complaint = complaintService.reportBooking(
                completedBooking.getId(),
                customer.getId(),
                "Terrible experience"
        );
        assertEquals(ComplaintStatus.PENDING, complaint.getStatus());

        // --- STEP 2: Admin reviews the case and bans the specialist ---
        // Note: Update 'banSpecialistByComplaint' if your service uses a different method name
        complaintService.banSpecialistByComplaint(complaint.getId());

        // --- STEP 3: Cross-table verification to ensure penalties are applied ---
        Complaint resolvedComplaint = complaintRepository.findById(complaint.getId()).orElseThrow();
        SpecialistProfile punishedSpecialist = profileRepository.findById(specialist.getId()).orElseThrow();

        // 1. Verify: The complaint ticket status is updated to BANNED
        assertEquals(ComplaintStatus.BANNED, resolvedComplaint.getStatus());

        // 2. CRITICAL VERIFICATION: The specialist's profile must be deactivated (INACTIVE)
        assertEquals(SpecialistStatus.INACTIVE, punishedSpecialist.getStatus());
    }
}