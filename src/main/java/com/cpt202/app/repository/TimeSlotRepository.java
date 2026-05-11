package com.cpt202.app.repository;

import com.cpt202.app.model.TimeSlot;
import com.cpt202.app.model.SpecialistProfile;
import com.cpt202.app.model.TimeSlotStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    // Find all available time slots for a specialist
    List<TimeSlot> findBySpecialistIdAndStatus(Long specialistId, TimeSlotStatus status);
    // Find all schedules for a specialist (regardless of whether they are booked)
    List<TimeSlot> findBySpecialistId(Long specialistId);
    // Find all schedules for a specialist within a specified date range, sorted chronologically
    List<TimeSlot> findBySpecialistIdAndSlotDateBetweenOrderBySlotDateAscStartTimeAsc(
            Long specialistId, LocalDate startDate, LocalDate endDate);
    // Check if overlapping time slots exist
    // Overlap condition: Start time of existing record < End time of new request AND End time of existing record > Start time of new request
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END " +
            "FROM TimeSlot t WHERE t.specialist.id = :specialistId " +
            "AND t.slotDate = :date " +
            "AND t.startTime < :endTime " +
            "AND t.endTime > :startTime")
    boolean existsOverlappingSlot(@Param("specialistId") Long specialistId,
                                  @Param("date") LocalDate date,
                                  @Param("startTime") LocalTime startTime,
                                  @Param("endTime") LocalTime endTime);

    // Dedicated for concurrent booking: Core of PBI 4.2, prevents two users from reading AVAILABLE simultaneously
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TimeSlot t WHERE t.id = :id")
    Optional<TimeSlot> findByIdWithLock(@Param("id") Long id);
}