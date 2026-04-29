package com.cpt202.app.service;

import com.cpt202.app.model.SpecialistProfile;
import com.cpt202.app.model.TimeSlot;
import com.cpt202.app.model.TimeSlotStatus;
import com.cpt202.app.model.User;
import com.cpt202.app.repository.SpecialistProfileRepository;
import com.cpt202.app.repository.TimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeSlotServiceTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private SpecialistProfileRepository specialistRepository;

    @InjectMocks
    private TimeSlotService timeSlotService;

    private User testUser;
    private SpecialistProfile testProfile;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testSpecialist");

        testProfile = new SpecialistProfile();
        testProfile.setId(10L);
        testProfile.setUser(testUser);
    }

    // ==========================================
    // 1. Query Methods Tests (Fetching Schedule)
    // ==========================================

    @Test
    void getAvailableDatesForSpecialist_ShouldReturnFutureAvailableDatesOnly() {
        // Arrange
        LocalDate today = LocalDate.now();

        TimeSlot pastSlot = createSlot(today.minusDays(1), TimeSlotStatus.AVAILABLE); // Should be filtered out
        TimeSlot futureSlot1 = createSlot(today.plusDays(2), TimeSlotStatus.AVAILABLE); // Should be included
        TimeSlot futureSlot2 = createSlot(today.plusDays(2), TimeSlotStatus.AVAILABLE); // Duplicate date, should be distinct
        TimeSlot bookedSlot = createSlot(today.plusDays(3), TimeSlotStatus.BOOKED); // Not available, should be filtered out
        TimeSlot farFutureSlot = createSlot(today.plusWeeks(3), TimeSlotStatus.AVAILABLE); // Beyond 2 weeks, should be filtered out

        when(timeSlotRepository.findBySpecialistId(10L)).thenReturn(List.of(pastSlot, futureSlot1, futureSlot2, bookedSlot, farFutureSlot));

        // Act
        List<LocalDate> availableDates = timeSlotService.getAvailableDatesForSpecialist(10L);

        // Assert
        assertEquals(1, availableDates.size()); // Only today.plusDays(2) should remain
        assertEquals(today.plusDays(2), availableDates.get(0));
    }

    @Test
    void getAvailableTimeSlotsForDate_ShouldReturnSortedAvailableSlots() {
        // Arrange
        LocalDate targetDate = LocalDate.now().plusDays(1);

        TimeSlot slot9AM = createSlot(targetDate, LocalTime.of(9, 0), TimeSlotStatus.AVAILABLE);
        TimeSlot slot8AM = createSlot(targetDate, LocalTime.of(8, 0), TimeSlotStatus.AVAILABLE); // Earlier, should be sorted first
        TimeSlot bookedSlot = createSlot(targetDate, LocalTime.of(10, 0), TimeSlotStatus.BOOKED); // Should be filtered
        TimeSlot wrongDateSlot = createSlot(targetDate.plusDays(1), LocalTime.of(8, 0), TimeSlotStatus.AVAILABLE);

        when(timeSlotRepository.findBySpecialistId(10L)).thenReturn(List.of(slot9AM, slot8AM, bookedSlot, wrongDateSlot));

        // Act
        List<TimeSlot> result = timeSlotService.getAvailableTimeSlotsForDate(10L, targetDate);

        // Assert
        assertEquals(2, result.size());
        assertEquals(LocalTime.of(8, 0), result.get(0).getStartTime()); // Verify sorting
        assertEquals(LocalTime.of(9, 0), result.get(1).getStartTime());
    }

    // ==========================================
    // 2. Batch Create Slots Tests
    // ==========================================

    @Test
    void batchCreateSlots_HappyPath_ShouldSaveAllSlots() {
        // Arrange
        LocalDate futureDate = LocalDate.now().plusDays(1);
        TimeSlotService.DailySlot dailySlot = new TimeSlotService.DailySlot(futureDate, LocalTime.of(9, 0), LocalTime.of(10, 0));
        TimeSlotService.TimeSlotBatchRequest request = new TimeSlotService.TimeSlotBatchRequest(List.of(dailySlot));

        when(specialistRepository.findByUserUsername(testUser.getUsername())).thenReturn(Optional.of(testProfile));
        // Mock overlap check to return false (no overlap)
        when(timeSlotRepository.existsOverlappingSlot(eq(10L), eq(futureDate), eq(LocalTime.of(9, 0)), eq(LocalTime.of(10, 0)))).thenReturn(false);

        // Act
        timeSlotService.batchCreateSlots(testUser.getUsername(), request);

        // Assert
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TimeSlot>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(timeSlotRepository, times(1)).saveAll(listCaptor.capture());

        List<TimeSlot> savedSlots = listCaptor.getValue();
        assertEquals(1, savedSlots.size());
        assertEquals(TimeSlotStatus.AVAILABLE, savedSlots.get(0).getStatus());
    }

    @Test
    void batchCreateSlots_PastDate_ShouldThrowException() {
        // Arrange
        LocalDate pastDate = LocalDate.now().minusDays(1); // Invalid past date
        TimeSlotService.DailySlot dailySlot = new TimeSlotService.DailySlot(pastDate, LocalTime.of(9, 0), LocalTime.of(10, 0));
        TimeSlotService.TimeSlotBatchRequest request = new TimeSlotService.TimeSlotBatchRequest(List.of(dailySlot));

        when(specialistRepository.findByUserUsername(testUser.getUsername())).thenReturn(Optional.of(testProfile));

        // Act & Assert
        Exception e = assertThrows(RuntimeException.class, () -> {
            timeSlotService.batchCreateSlots(testUser.getUsername(), request);
        });
        assertTrue(e.getMessage().contains("不能发布过去的排班"));
        verify(timeSlotRepository, never()).saveAll(any());
    }

    @Test
    void batchCreateSlots_OverlappingSlot_ShouldThrowException() {
        // Arrange
        LocalDate futureDate = LocalDate.now().plusDays(1);
        TimeSlotService.DailySlot dailySlot = new TimeSlotService.DailySlot(futureDate, LocalTime.of(9, 0), LocalTime.of(10, 0));
        TimeSlotService.TimeSlotBatchRequest request = new TimeSlotService.TimeSlotBatchRequest(List.of(dailySlot));

        when(specialistRepository.findByUserUsername(testUser.getUsername())).thenReturn(Optional.of(testProfile));
        // Mock overlap check to return TRUE (overlap detected!)
        when(timeSlotRepository.existsOverlappingSlot(anyLong(), any(), any(), any())).thenReturn(true);

        // Act & Assert
        Exception e = assertThrows(RuntimeException.class, () -> {
            timeSlotService.batchCreateSlots(testUser.getUsername(), request);
        });
        assertTrue(e.getMessage().contains("时间段发生重叠"));
    }

    // ==========================================
    // 3. Delete Time Slot Tests
    // ==========================================

    @Test
    void deleteTimeSlot_HappyPath_ShouldDeleteSlot() {
        // Arrange
        TimeSlot slot = new TimeSlot();
        slot.setId(1L);
        slot.setSpecialist(testProfile); // Owned by the test user
        slot.setStatus(TimeSlotStatus.AVAILABLE); // Status is safely AVAILABLE

        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(slot));

        // Act
        timeSlotService.deleteTimeSlot(testUser.getUsername(), 1L);

        // Assert
        verify(timeSlotRepository, times(1)).delete(slot);
    }

    @Test
    void deleteTimeSlot_WrongOwner_ShouldThrowException() {
        // Arrange
        User anotherUser = new User();
        anotherUser.setUsername("hacker_user"); // Different username
        SpecialistProfile anotherProfile = new SpecialistProfile();
        anotherProfile.setUser(anotherUser);

        TimeSlot slot = new TimeSlot();
        slot.setId(1L);
        slot.setSpecialist(anotherProfile); // Owned by someone else

        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(slot));

        // Act & Assert
        Exception e = assertThrows(RuntimeException.class, () -> {
            timeSlotService.deleteTimeSlot(testUser.getUsername(), 1L);
        });
        assertEquals("无权操作他人的排班", e.getMessage());
        verify(timeSlotRepository, never()).delete(any());
    }

    @Test
    void deleteTimeSlot_SlotIsBooked_ShouldThrowException() {
        // Arrange
        TimeSlot slot = new TimeSlot();
        slot.setId(1L);
        slot.setSpecialist(testProfile);
        slot.setStatus(TimeSlotStatus.BOOKED); // Status is NOT available

        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(slot));

        // Act & Assert
        Exception e = assertThrows(RuntimeException.class, () -> {
            timeSlotService.deleteTimeSlot(testUser.getUsername(), 1L);
        });
        assertTrue(e.getMessage().contains("已被预约或锁定"));
        verify(timeSlotRepository, never()).delete(any());
    }

    // ==========================================
    // Helper Methods for Mock Data
    // ==========================================

    private TimeSlot createSlot(LocalDate date, TimeSlotStatus status) {
        return createSlot(date, LocalTime.of(9, 0), status);
    }

    private TimeSlot createSlot(LocalDate date, LocalTime startTime, TimeSlotStatus status) {
        TimeSlot slot = new TimeSlot();
        slot.setSlotDate(date);
        slot.setStartTime(startTime);
        slot.setStatus(status);
        return slot;
    }

    // ==========================================
    // 4. Schedule Query Tests (Missing Parts)
    // ==========================================

    @Test
    void getAllTimeSlotsForSpecialistSchedule_ShouldReturnCurrentAndNextWeekSlots() {
        // Arrange: Prepare slots across different weeks
        LocalDate today = LocalDate.now();
        TimeSlot pastSlot = createSlot(today.minusDays(1), TimeSlotStatus.AVAILABLE); // Should be filtered out
        TimeSlot thisWeekSlot = createSlot(today.plusDays(2), LocalTime.of(10, 0), TimeSlotStatus.AVAILABLE); // Included

        TimeSlot nextWeekSlot = createSlot(today.plusDays(6), LocalTime.of(9, 0), TimeSlotStatus.BOOKED); // Included

        TimeSlot farFutureSlot = createSlot(today.plusWeeks(3), TimeSlotStatus.AVAILABLE); // Should be filtered out

        when(timeSlotRepository.findBySpecialistId(10L))
                .thenReturn(List.of(pastSlot, thisWeekSlot, nextWeekSlot, farFutureSlot));

        // Act
        List<TimeSlot> result = timeSlotService.getAllTimeSlotsForSpecialistSchedule(10L);

        // Assert: Now both slots will pass the 7-day filter (size = 2)
        assertEquals(2, result.size());
        assertEquals(thisWeekSlot, result.get(0)); // Also verifies sorting by date/time
        assertEquals(nextWeekSlot, result.get(1));
    }

    @Test
    void getSpecialistScheduleByDateRange_ShouldReturnSlotsFromRepository() {
        // Arrange: This method directly calls the repository, so we just mock the repository call
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(7);
        TimeSlot mockSlot = createSlot(startDate.plusDays(1), TimeSlotStatus.AVAILABLE);

        when(timeSlotRepository.findBySpecialistIdAndSlotDateBetweenOrderBySlotDateAscStartTimeAsc(10L, startDate, endDate))
                .thenReturn(List.of(mockSlot));

        // Act
        List<TimeSlot> result = timeSlotService.getSpecialistScheduleByDateRange(10L, startDate, endDate);

        // Assert
        assertEquals(1, result.size());
        assertEquals(mockSlot, result.get(0));
    }
}