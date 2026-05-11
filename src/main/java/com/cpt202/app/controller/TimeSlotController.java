package com.cpt202.app.controller;

import com.cpt202.app.model.Booking;
import com.cpt202.app.model.BookingStatus;
import com.cpt202.app.model.SpecialistProfile;
import com.cpt202.app.model.TimeSlot;
import com.cpt202.app.model.TimeSlotStatus;
import com.cpt202.app.model.User;
import com.cpt202.app.model.UserRole;
import com.cpt202.app.repository.BookingRepository;
import com.cpt202.app.repository.SpecialistProfileRepository;
import com.cpt202.app.service.TimeSlotService;
import com.cpt202.app.service.TimeSlotService.TimeSlotBatchRequest;
import com.cpt202.app.service.TimeSlotService.TimeSlotWithBookingDTO;
import com.cpt202.app.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/timeslots")
@CrossOrigin
public class TimeSlotController {

    @Autowired
    private TimeSlotService timeSlotService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SpecialistProfileRepository specialistProfileRepository;

    @Autowired
    private UserService userService;

    /**Get a list of dates with available time slots for the specialist in the next two weeks (used to highlight the calendar*/
    @GetMapping("/specialist/{specialistId}/available-dates")
    public Map<String, Object> getAvailableDates(@PathVariable Long specialistId) {
        Map<String, Object> response = new HashMap<>();
        List<LocalDate> availableDates = timeSlotService.getAvailableDatesForSpecialist(specialistId);
        response.put("availableDates", availableDates);
        return response;
    }

    /**get all available time slots for that specialist on that day*/
    @GetMapping("/specialist/{specialistId}/available-times")
    public Map<String, Object> getAvailableTimeSlots(
            @PathVariable Long specialistId,
            @RequestParam String date) {
        Map<String, Object> response = new HashMap<>();
        LocalDate targetDate = LocalDate.parse(date);
        List<TimeSlot> timeSlots = timeSlotService.getAvailableTimeSlotsForDate(specialistId, targetDate);
        response.put("timeSlots", timeSlots);
        return response;
    }

    /**get the schedule for a specified week of the specialist including available time slots and detailed information of existing bookings*/
    @GetMapping("/my-schedule")
    public Map<String, Object> getMySchedule(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // 0. Get the current user from JWT, and join tables to find the real Profile ID of the specialist
        Long actualSpecialistId = getAuthenticatedSpecialistId(authentication);

        Map<String, Object> response = new HashMap<>();

        // 1. Smart calculation of the "current week" range: If the frontend does not pass dates, the backend automatically calculates the current Monday to Sunday
        if (startDate == null || endDate == null) {
            LocalDate today = LocalDate.now();
            startDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            endDate = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        }

        // 2. Get all time slots (TimeSlot) within these 7 days
        List<TimeSlot> timeSlots = timeSlotService.getSpecialistScheduleByDateRange(actualSpecialistId, startDate, endDate);

        // 3. Get related valid bookings within these 7 days
        // Query directly using booking.specialist_id
        List<Booking> bookings = bookingRepository.findBySpecialistIdAndStatusIn(
                actualSpecialistId,
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.COMPLETED)
        );

        // 4. Convert bookings to a dictionary (Map) for stitching
        Map<Long, Booking> bookingMap = bookings.stream()
                .collect(Collectors.toMap(b -> b.getTimeSlot().getId(), b -> b));

        // 5. Assemble into a DTO list containing complete status (using Record)
        List<TimeSlotWithBookingDTO> scheduleDTOs = timeSlots.stream()
                .map(timeSlot -> {
                    Booking booking = bookingMap.get(timeSlot.getId());
                    return new TimeSlotWithBookingDTO(
                            timeSlot.getId(),
                            timeSlot.getSlotDate(),
                            timeSlot.getStartTime(),
                            timeSlot.getEndTime(),
                            timeSlot.getStatus(),
                            booking != null ? booking.getStatus() : null,
                            booking != null ? booking.getCustomer().getUsername() : null,
                            booking != null ? booking.getNotes() : null,
                            booking != null ? booking.getId() : null
                    );
                })
                .collect(Collectors.toList());

        // 6. Group by date (alternative data structure)
        Map<LocalDate, List<TimeSlotWithBookingDTO>> scheduleByDate = scheduleDTOs.stream()
                .collect(Collectors.groupingBy(TimeSlotWithBookingDTO::slotDate));

        // 7. Return a flat array preferred by the calendar component, and also indicate the current rendered week range
        response.put("flatSchedule", scheduleDTOs);
        response.put("scheduleGrouped", scheduleByDate);
        response.put("currentWeekStart", startDate);
        response.put("currentWeekEnd", endDate);

        return response;
    }

    /**Get global data statistics of specialist schedules and bookings*/
    @GetMapping("/my-schedule-summary")
    public Map<String, Object> getMyScheduleSummary(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Long actualSpecialistId = getAuthenticatedSpecialistId(authentication);
        Map<String, Object> response = new HashMap<>();

        // Sync date calculation logic for the statistics panel
        if (startDate == null || endDate == null) {
            LocalDate today = LocalDate.now();
            startDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            endDate = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        }

        List<TimeSlot> timeSlots = timeSlotService.getSpecialistScheduleByDateRange(actualSpecialistId, startDate, endDate);
        List<Booking> bookings = bookingRepository.findBySpecialistIdAndStatusIn(
                actualSpecialistId,
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.COMPLETED)
        );

        long totalSlots = timeSlots.size();
        long availableSlots = timeSlots.stream().filter(ts -> ts.getStatus() == TimeSlotStatus.AVAILABLE).count();
        long bookedSlots = timeSlots.stream().filter(ts -> ts.getStatus() == TimeSlotStatus.BOOKED).count();
        long disabledSlots = timeSlots.stream().filter(ts -> ts.getStatus() == TimeSlotStatus.DISABLED).count();

        long pendingBookings = bookings.stream().filter(b -> b.getStatus() == BookingStatus.PENDING).count();
        long confirmedBookings = bookings.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
        long completedBookings = bookings.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();

        response.put("totalSlots", totalSlots);
        response.put("availableSlots", availableSlots);
        response.put("bookedSlots", bookedSlots);
        response.put("disabledSlots", disabledSlots);
        response.put("pendingBookings", pendingBookings);
        response.put("confirmedBookings", confirmedBookings);
        response.put("completedBookings", completedBookings);
        response.put("summaryRange", startDate + " to " + endDate);

        return response;
    }
    /**Specialist publishes/adds schedules (supports batch and single addition)*/
    @PostMapping("/publish")
    @PreAuthorize("hasRole('SPECIALIST')")
    public ResponseEntity<?> publishSchedule(@RequestBody TimeSlotBatchRequest request, Authentication auth) {
        try {
            timeSlotService.batchCreateSlots(auth.getName(), request);
            return ResponseEntity.ok(Map.of("message", "Schedule published successfully!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Publish failed: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "A server error occurred, please try again later"));
        }
    }

    /**Specialist deletes unbooked time slots*/
    @DeleteMapping("/{slotId}")
    @PreAuthorize("hasRole('SPECIALIST')")
    public ResponseEntity<?> deleteTimeSlot(@PathVariable Long slotId, Authentication auth) {
        try {
            timeSlotService.deleteTimeSlot(auth.getName(), slotId);
            return ResponseEntity.ok(Map.of("message", "Time slot deleted successfully!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Delete failed: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "A server error occurred, please try again later"));
        }
    }
    /**Extract identity from JWT, verify specialist role, and find the real Profile ID*/
    private Long getAuthenticatedSpecialistId(Authentication authentication) {
        String currentUsername = authentication.getName();
        User currentUser = userService.getByUsername(currentUsername);

        if (currentUser.getRole() != UserRole.SPECIALIST) {
            throw new AccessDeniedException("Access denied: Current user is not a specialist");
        }

        SpecialistProfile profile = specialistProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Data error: Specialist profile associated with this account not found"));

        return profile.getId();
    }
}