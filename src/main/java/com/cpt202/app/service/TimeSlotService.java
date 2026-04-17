package com.cpt202.app.service;

import com.cpt202.app.model.BookingStatus;
import com.cpt202.app.model.TimeSlot;
import com.cpt202.app.model.TimeSlotStatus;
import com.cpt202.app.repository.TimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalTime;


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

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    /**
     * Get available dates for a specialist within the next 2 weeks
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
        LocalDate endDate = startDate.plusWeeks(1);
        
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
        // 调用底层 Repository 去查这 7 天内的数据，并按日期和时间排序
        return timeSlotRepository.findBySpecialistIdAndSlotDateBetweenOrderBySlotDateAscStartTimeAsc(specialistId, startDate, endDate);
    }
}
