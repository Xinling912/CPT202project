package com.cpt202.app.service;

import com.cpt202.app.model.*;
import com.cpt202.app.repository.BookingRepository;
import com.cpt202.app.repository.SpecialistProfileRepository;
import com.cpt202.app.repository.TimeSlotRepository;
import com.cpt202.app.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {


    // 1. Use final keyword to ensure dependencies are immutable
    private final BookingRepository bookingRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final UserRepository userRepository;
    private final SpecialistProfileRepository specialistRepository;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BookingService.class);

    // 2. Constructor injection
    public BookingService(BookingRepository bookingRepository, TimeSlotRepository timeSlotRepository, UserRepository userRepository, SpecialistProfileRepository specialistRepository) {
        this.bookingRepository = bookingRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.userRepository = userRepository;
        this.specialistRepository = specialistRepository;

    }


    public record BookingResponse(
            Long id,
            String customerName,
            String specialistName,
            String date,
            String startTime,
            String endTime,
            String status,
            String notes,
            BigDecimal totalAmount
    ) {}

    @Transactional(rollbackFor = Exception.class)
    public BookingResponse createBooking(String email, Long specialistId, Long slotId, String notes) {
        // 1. [Security Check] Identity confirmation completed inside Service
        User customer = userRepository.findByUsername(email)
                .orElseThrow(() -> new IllegalArgumentException("ERROR_USER_NOT_FOUND"));

        SpecialistProfile specialist = specialistRepository.findById(specialistId)
                .orElseThrow(() -> new IllegalArgumentException("ERROR_SPECIALIST_NOT_FOUND"));

        // 2. Get time slot (locked with pessimistic lock)
        TimeSlot slot = timeSlotRepository.findByIdWithLock(slotId)
                .orElseThrow(() -> new IllegalArgumentException("ERROR_SLOT_NOT_FOUND"));

        // 3. Business validation
        LocalDateTime appointmentDateTime = LocalDateTime.of(slot.getSlotDate(), slot.getStartTime());
        if (appointmentDateTime.isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("ERROR_SLOT_EXPIRED");
        }

        // 4. Ensure only slots with status "AVAILABLE" can be booked
        if (slot.getStatus() != TimeSlotStatus.AVAILABLE) {
            throw new IllegalStateException("ERROR_SLOT_TAKEN");
        }

        // 5. Customers who have cancelled more than three times within a month cannot book
        long cancelCount = countMonthlyCancellations(customer.getId());
        if (cancelCount >= 3) {
            throw new IllegalStateException("ERROR_MONTHLY_LIMIT_REACHED");
        }

        // 6. Customer duplicate booking validation
        // 6.5 Prevent malicious spamming: If the customer has cancelled this time slot before, prohibit immediate re-booking!
        // (Note: This only intercepts the current customer, not other customers, leaving opportunities for others)
        boolean hasCancelledBefore = bookingRepository.existsByCustomerIdAndTimeSlotIdAndStatus(
                customer.getId(), slotId, BookingStatus.CANCELLED);

        if (hasCancelledBefore) {
            // Throw exclusive custom error code for frontend!
            throw new IllegalStateException("ERROR_RECENTLY_CANCELLED");
        }
        List<BookingStatus> activeStatuses = Arrays.asList(BookingStatus.PENDING, BookingStatus.CONFIRMED);
        boolean alreadyBooked = bookingRepository.existsByCustomerIdAndTimeSlotIdAndStatusIn(
                customer.getId(), slotId, activeStatuses);

        if (alreadyBooked) {
            throw new IllegalStateException("ERROR_DUPLICATE_BOOKING_AT_SAME_TIME");
        }


        // 7. Prevent specialists from booking their own service
        if (customer.getId().equals(specialist.getUser().getId())) {
            throw new IllegalStateException("You cannot book your own service.");
        }

        // 8. Execute status synchronization update
        slot.setStatus(TimeSlotStatus.BOOKED);
        timeSlotRepository.save(slot);

        // 9. Construct booking entity
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setSpecialist(specialist);
        booking.setTimeSlot(slot);
        booking.setStatus(BookingStatus.PENDING);
        booking.setNotes(notes);

        // Calculate total amount
        // Calculate hours of the time slot
        long hours = java.time.Duration.between(slot.getStartTime(), slot.getEndTime()).toHours();
        // If hours is 0 (e.g., slot is less than 1 hour), calculate as at least 1 hour
        if (hours <= 0) {
            hours = 1;
        }
        // Calculate total amount = hourly fee × hours
        BigDecimal totalAmount = specialist.getHourlyFee()
                .multiply(BigDecimal.valueOf(hours))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        booking.setTotalAmount(totalAmount);

        // 10. Add log
        log.info("Booking created successfully. BookingId: {}, Customer: {}", booking.getId(), customer.getUsername());

        // [Modification Point]: No longer return Entity directly; save first, then convert to DTO
        Booking savedBooking = bookingRepository.save(booking);
        return convertToResponse(savedBooking);
    }


    // pbi5
    // Cancel booking functionality
    @Transactional
    public void cancelBooking(Long bookingId, String reason, String email) {
        // 1. Get user and booking information
        User user = userRepository.findByUsername(email)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("ERROR_BOOKING_NOT_FOUND"));

        // 2. Authentication: Use internal helper method to determine role and ownership
        boolean isSpecialist = user.getRole().equals(UserRole.SPECIALIST);
        validateAuthorization(booking, user, isSpecialist);

        // 3. Business rule validation: If customer cancels, check 24-hour limit
        if (!isSpecialist) {
            LocalDateTime appointmentTime = LocalDateTime.of(booking.getTimeSlot().getSlotDate(), booking.getTimeSlot().getStartTime());
            if (appointmentTime.isBefore(LocalDateTime.now().plusHours(24))) {
                throw new IllegalStateException("ERROR_CANCEL_LIMIT_EXCEEDED: Must cancel at least 24 hours in advance");
            }
        }
        // 4. Execute business logic (status transition); completed or finalized bookings cannot be cancelled
        if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("ERROR_CANNOT_CANCEL_FINALIZED_ORDER");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        String operator = isSpecialist ? "Specialist" : "Customer";
        String currentNotes = (booking.getNotes() == null) ? "" : booking.getNotes();
        booking.setNotes(currentNotes + " | Cancelled by " + operator + ". Reason: " + reason);

        // 5. Update Slot status
        TimeSlot slot = booking.getTimeSlot();
        if (slot != null) {
            slot.setStatus(isSpecialist ? TimeSlotStatus.DISABLED : TimeSlotStatus.AVAILABLE);
            timeSlotRepository.save(slot);
        }


    }

    @Scheduled(cron = "0 * * * * *") // Changed to execute once per minute
    @Transactional(rollbackFor = Exception.class)
    public void processAutoStatusTransitions() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> pendingBookings = bookingRepository.findByStatus(BookingStatus.PENDING);

        for (Booking b : pendingBookings) {
            LocalDateTime startTime = LocalDateTime.of(b.getTimeSlot().getSlotDate(), b.getTimeSlot().getStartTime());

            // --- Priority 1: 5-minute hard deadline (DISABLED immediately regardless of urgency) ---
            if (now.isAfter(startTime.minusMinutes(5))) {
                cancelBookingAsDisabled(b, "EXPIRED: Less than 5 mins to start");
                continue; // Skip subsequent logic and move to next iteration
            }

            // --- Priority 2: Confirmation deadline (determined by urgency) ---
            boolean isUrgent = startTime.isBefore(now.plusHours(24));
            LocalDateTime deadline = isUrgent ? b.getCreatedAt().plusHours(1) : b.getCreatedAt().plusHours(24);

            if (now.isAfter(deadline)) {
                cancelBookingAsAvailable(b, "TIMEOUT: Confirmation deadline exceeded");
            }
        }

        // 3. Auto-complete (Confirmed -> Completed)
        List<Booking> confirmedBookings = bookingRepository.findByStatus(BookingStatus.CONFIRMED);
        for (Booking b : confirmedBookings) {
            LocalDateTime end = LocalDateTime.of(b.getTimeSlot().getSlotDate(), b.getTimeSlot().getEndTime());
            if (now.isAfter(end.plusHours(24))) {
                b.setStatus(BookingStatus.COMPLETED);
                bookingRepository.save(b);
            }
        }
    }

    // 1. Entry method: Controller calls this method to start a transaction
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrder(Long orderId, String email) {
        Long specialistId = getProfileByEmail(email).getId();
        // Call internal core logic directly
        confirmOrderInternal(orderId, specialistId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void completeOrder(Long orderId, String email) {
        Long specialistId = getProfileByEmail(email).getId();
        // Call internal core logic directly
        completeOrderInternal(orderId, specialistId);
    }


    // 2. Core business logic: private method, does not need @Transactional
    // because it is called by the entry method above, which has already started a transaction.
    // Interception added in confirmOrderInternal.
    private void confirmOrderInternal(Long orderId, Long specialistId) {
        Booking booking = getVerifiedBookingForSpecialist(orderId, specialistId);

        // [Correction: Should not limit to 5 hours ahead]
        LocalDateTime startTime = LocalDateTime.of(booking.getTimeSlot().getSlotDate(), booking.getTimeSlot().getStartTime());
        if (LocalDateTime.now().isAfter(startTime.minusMinutes(5))) {
            throw new IllegalStateException("ERROR_CONFIRMATION_EXPIRED");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("ERROR_INVALID_STATUS");
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
    }

    private void completeOrderInternal(Long orderId, Long specialistId) {
        Booking booking = getVerifiedBookingForSpecialist(orderId, specialistId);

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("ERROR_ONLY_CONFIRMED_CAN_BE_COMPLETED");
        }

        LocalDateTime startTime = LocalDateTime.of(
                booking.getTimeSlot().getSlotDate(),
                booking.getTimeSlot().getStartTime()
        );
        // Ensure the booking has actually started
        if (LocalDateTime.now().isBefore(startTime)) {
            throw new IllegalStateException("ERROR_BOOKING_NOT_STARTED_YET");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);
    }

    // Helper method: Set status to CANCELLED, set resource to DISABLED (banned)
    private void cancelBookingAsDisabled(Booking b, String reason) {
        b.setStatus(BookingStatus.CANCELLED);
        b.setNotes((b.getNotes() == null ? "" : b.getNotes()) + " | " + reason);

        TimeSlot slot = b.getTimeSlot();
        slot.setStatus(TimeSlotStatus.DISABLED); // Ban the resource

        timeSlotRepository.save(slot);
        bookingRepository.save(b);
        log.warn("Booking {} cancelled. Slot {} DISABLED due to: {}", b.getId(), slot.getId(), reason);
    }

    // Helper method: Set status to CANCELLED, set resource to AVAILABLE (released)
    private void cancelBookingAsAvailable(Booking b, String reason) {
        b.setStatus(BookingStatus.CANCELLED);
        b.setNotes((b.getNotes() == null ? "" : b.getNotes()) + " | " + reason);

        TimeSlot slot = b.getTimeSlot();
        slot.setStatus(TimeSlotStatus.AVAILABLE); // Release the resource

        timeSlotRepository.save(slot);
        bookingRepository.save(b);
        log.info("Booking {} cancelled. Slot {} released.", b.getId(), slot.getId());
    }





    private long countMonthlyCancellations(Long customerId) {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        return bookingRepository.countByCustomerIdAndStatusAndCreatedAtAfter(customerId, BookingStatus.CANCELLED, startOfMonth);
    }

    // Implementation for specialist querying database
    public List<BookingResponse> getSpecialistOrders(String username) {
        // 1. Query user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 2. Validate role (logic moved)
        if (!user.getRole().equals(UserRole.SPECIALIST)) {
            throw new IllegalStateException("Access denied: You are not a specialist");
        }

        // 3. Query profile
        SpecialistProfile profile = specialistRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Specialist profile not found"));

        // 4. Query bookings and convert to DTO (Resolve Entity leakage)
        return bookingRepository.findBySpecialistId(profile.getId())
                .stream()
                .map(this::convertToResponse) // This step turns Booking into the DTO required by the frontend
                .collect(Collectors.toList());
    }

    // Provide personal booking list for general users
    public List<BookingResponse> getOrdersByCustomerResponse(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return bookingRepository.findByCustomerId(user.getId())
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // Private helper method: Specially used to "slim down" data
    private BookingResponse convertToResponse(Booking b) {
        return new BookingResponse(
                b.getId(),
                b.getCustomer().getUsername(),
                b.getSpecialist().getUser().getUsername(),
                b.getTimeSlot().getSlotDate().toString(),
                b.getTimeSlot().getStartTime().toString(),
                b.getTimeSlot().getEndTime().toString(),
                b.getStatus().toString(),
                b.getNotes(),
                b.getTotalAmount() // <--- Ensure this is added
        );
    }


    // Convert "email provided by user (external credential)" into "system internal business object (specialist profile)"
    private SpecialistProfile getProfileByEmail(String email) {
        // You need to inject userRepository and specialistRepository
        User user = userRepository.findByUsername(email)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
        return specialistRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("SPECIALIST_NOT_FOUND"));
    }

    // Get data + check ownership (ensure if the booking belongs to the current id)
    private Booking getVerifiedBookingForSpecialist(Long orderId, Long currentSpecialistId) {
        Booking booking = bookingRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("ERROR_BOOKING_NOT_FOUND"));

        if (!booking.getSpecialist().getId().equals(currentSpecialistId)) {
            throw new IllegalStateException("ERROR_NOT_AUTHORIZED_TO_OPERATE_THIS_ORDER");
        }
        return booking;
    }



    // Execute different validation rules based on role (Specialist/Customer/Admin)
    private void validateAuthorization(Booking booking, User user, boolean isSpecialist) {
        // 1. Admin has the highest authority, skip validation directly (or record audit log)
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }
        // 2. If it's a specialist, check if they are the provider for this booking
        if (isSpecialist) {
            if (!booking.getSpecialist().getUser().getId().equals(user.getId())) {
                throw new IllegalStateException("ERROR_NOT_AUTHORIZED: You are not the specialist for this booking.");
            }
        }
        // 3. If it's a customer, check if they are the booker for this booking
        else {
            if (!booking.getCustomer().getId().equals(user.getId())) {
                throw new IllegalStateException("ERROR_NOT_AUTHORIZED: This booking does not belong to you.");
            }
        }
    }
}