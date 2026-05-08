package com.cpt202.app.service;

import com.cpt202.app.model.BookingStatus;
import com.cpt202.app.model.SpecialistProfile;
import com.cpt202.app.model.TimeSlot;
import com.cpt202.app.model.TimeSlotStatus;
import com.cpt202.app.repository.SpecialistProfileRepository;
import com.cpt202.app.repository.TimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Added transaction annotation

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.annotation.JsonFormat;

@Service
public class TimeSlotService {

    public record TimeSlotWithBookingDTO(
            Long id,
            @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8") LocalDate slotDate,
            @JsonFormat(pattern = "HH:mm:ss", timezone = "GMT+8") LocalTime startTime,
            @JsonFormat(pattern = "HH:mm:ss", timezone = "GMT+8") LocalTime endTime,
            TimeSlotStatus timeSlotStatus,
            BookingStatus bookingStatus,
            String customerUsername,
            String customerNotes,
            Long bookingId
    ) {}

    public record TimeSlotBatchRequest(List<DailySlot> slots) {}

    public record DailySlot(LocalDate date, LocalTime startTime, LocalTime endTime) {}

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private SpecialistProfileRepository specialistRepository;

    /**
     * Get available dates for a specialist within the next two weeks
     */
    public List<LocalDate> getAvailableDatesForSpecialist(Long specialistId) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusWeeks(2);

        List<TimeSlot> allSlots = timeSlotRepository.findBySpecialistId(specialistId);

        return allSlots.stream()
                .filter(slot -> slot.getStatus() == TimeSlotStatus.AVAILABLE)
                .filter(slot -> !slot.getSlotDate().isBefore(startDate) && !slot.getSlotDate().isAfter(endDate))
                .map(TimeSlot::getSlotDate)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get available time slots for a specialist on a specific date
     */
    public List<TimeSlot> getAvailableTimeSlotsForDate(Long specialistId, LocalDate date) {
        List<TimeSlot> allSlots = timeSlotRepository.findBySpecialistId(specialistId);

        return allSlots.stream()
                .filter(slot -> slot.getStatus() == TimeSlotStatus.AVAILABLE)
                .filter(slot -> slot.getSlotDate().equals(date))
                .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                .collect(Collectors.toList());
    }

    /**
     * Get all time slots for a specialist within current week and next week
     */
    public List<TimeSlot> getAllTimeSlotsForSpecialistSchedule(Long specialistId) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusWeeks(2);

        List<TimeSlot> allSlots = timeSlotRepository.findBySpecialistId(specialistId);

        return allSlots.stream()
                .filter(slot -> !slot.getSlotDate().isBefore(startDate) && !slot.getSlotDate().isAfter(endDate))
                .sorted((a, b) -> {
                    int dateCompare = a.getSlotDate().compareTo(b.getSlotDate());
                    if (dateCompare != 0) return dateCompare;
                    return a.getStartTime().compareTo(b.getStartTime());
                })
                .collect(Collectors.toList());
    }

    /**
     * Get all dates with time slots for a specialist within current week or target week
     */
    public List<TimeSlot> getSpecialistScheduleByDateRange(Long specialistId, LocalDate startDate, LocalDate endDate) {
        // Call the underlying Repository to query data within these 7 days, sorted by date and time
        return timeSlotRepository.findBySpecialistIdAndSlotDateBetweenOrderBySlotDateAscStartTimeAsc(specialistId, startDate, endDate);
    }



    /**
     * Batch publish schedules (selected weekly data from the frontend)
     */
    public void batchCreateSlots(String username, TimeSlotBatchRequest request) {
        SpecialistProfile profile = specialistRepository.findByUserUsername(username)
                .orElseThrow(() -> new RuntimeException("Specialist profile does not exist"));

        Long specId = profile.getId();
        List<TimeSlot> slotsToSave = new ArrayList<>();

        for (DailySlot dailySlot : request.slots()) {
            if (dailySlot.date().isBefore(LocalDate.now())) {
                throw new RuntimeException("Cannot publish past schedules: " + dailySlot.date());
            }

            // Overlap validation (requires existOverlappingSlot method in Repository)
            boolean isOverlapping = timeSlotRepository.existsOverlappingSlot(
                    specId, dailySlot.date(), dailySlot.startTime(), dailySlot.endTime());

            if (isOverlapping) {
                throw new RuntimeException("Time slots overlap, please check: " + dailySlot.date() + " " + dailySlot.startTime());
            }

            TimeSlot timeSlot = new TimeSlot();
            timeSlot.setSpecialist(profile);
            timeSlot.setSlotDate(dailySlot.date());
            timeSlot.setStartTime(dailySlot.startTime());
            timeSlot.setEndTime(dailySlot.endTime());
            timeSlot.setStatus(TimeSlotStatus.AVAILABLE);

            slotsToSave.add(timeSlot);
        }

        timeSlotRepository.saveAll(slotsToSave);
    }

    /**
     * Specialist management: Delete a time slot that hasn't been booked
     */
    public void deleteTimeSlot(String username, Long slotId) {
        TimeSlot slot = timeSlotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Time slot does not exist"));

        // Security check: Confirm the schedule belongs to the current user
        if (!slot.getSpecialist().getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized to operate on others' schedules");
        }

        // Business rule: Only AVAILABLE (unbooked) time slots can be deleted
        if (slot.getStatus() != TimeSlotStatus.AVAILABLE) {
            throw new RuntimeException("This time slot has been booked or locked and cannot be deleted! Please contact the customer to cancel the booking first.");
        }

        timeSlotRepository.delete(slot);
    }
}