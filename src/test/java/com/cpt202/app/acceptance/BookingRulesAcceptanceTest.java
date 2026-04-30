package com.cpt202.app.acceptance;

import com.cpt202.app.model.*;
import com.cpt202.app.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Feature: Booking Business Rules and Constraints
 * Scenario: Validates complex rules like automatic price calculation,
 * 24-hour cancellation policy, and repeat booking restrictions.
 */
public class BookingRulesAcceptanceTest extends BaseAcceptanceTest {

    @Autowired
    private BookingService bookingService;

    @Test
    void testBookingBusinessRulesAndFinancials() {
        // 1. [Setup] Create an active specialist with a specific hourly fee
        SpecialistProfile specialist = createActiveSpecialist("price_expert", "Technology");
        specialist.setHourlyFee(new BigDecimal("200.00")); // $200 per hour
        specialistRepository.save(specialist);

        // Create an available time slot (2 hours duration: 14:00 - 16:00)
        TimeSlot slot = new TimeSlot();
        slot.setSpecialist(specialist);
        slot.setSlotDate(LocalDate.now().plusDays(3)); // 3 days in future to bypass 24h rule
        slot.setStartTime(LocalTime.of(14, 0));
        slot.setEndTime(LocalTime.of(16, 0));
        slot.setStatus(TimeSlotStatus.AVAILABLE);
        timeSlotRepository.save(slot);

        User customer = createTestUser("wealthy_client", UserRole.CUSTOMER);

        // 2. [Scenario: Pricing] Verify Automatic Charge Calculation
        // Rule: Total Amount = Hourly Fee * Hours
        BookingService.BookingResponse response = bookingService.createBooking(
                customer.getUsername(), specialist.getId(), slot.getId(), "Test notes");

        // Expectation: 200.00 * 2 hours = 400.00
        assertEquals(new BigDecimal("400.00"), response.totalAmount(),
                "Acceptance Criterion Failed: Booking total amount calculation is incorrect");

        // 3. [Scenario: Repeat Booking] Verify protection against recently cancelled slots
        // First, cancel the booking
        bookingService.cancelBooking(response.id(), "Change of mind", customer.getUsername());

        // Rule: If customer cancelled this specific slot before, they shouldn't re-book it immediately
        assertThrows(IllegalStateException.class, () -> {
            bookingService.createBooking(customer.getUsername(), specialist.getId(), slot.getId(), "Trying again");
        }, "Acceptance Criterion Passed: System blocks re-booking of a previously cancelled slot by the same user");

        // 4. [Scenario: 24h Cancellation] Verify that customers cannot cancel within 24 hours of start
        // Create a new slot that starts in only 5 hours
        TimeSlot urgentSlot = new TimeSlot();
        urgentSlot.setSpecialist(specialist);
        urgentSlot.setSlotDate(LocalDate.now().plusDays(1)); // 改为明天
        urgentSlot.setStartTime(LocalTime.now()); // 如果现在是10:00，这时段就是明天10:00
        urgentSlot.setEndTime(LocalTime.now().plusHours(1));
        urgentSlot.setStatus(TimeSlotStatus.AVAILABLE);
        timeSlotRepository.save(urgentSlot);

        BookingService.BookingResponse urgentBooking = bookingService.createBooking(
                customer.getUsername(), specialist.getId(), urgentSlot.getId(), "Urgent");

        // Expectation: System should throw error because it's within the 24-hour limit
        assertThrows(IllegalStateException.class, () -> {
            bookingService.cancelBooking(urgentBooking.id(), "Last minute emergency", customer.getUsername());
        }, "Acceptance Criterion Passed: System enforces the 24-hour advance cancellation rule for customers");
    }
}