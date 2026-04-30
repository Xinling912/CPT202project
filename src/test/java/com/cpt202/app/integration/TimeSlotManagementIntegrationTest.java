package com.cpt202.app.integration;

import com.cpt202.app.model.*;
import com.cpt202.app.repository.*;
import com.cpt202.app.service.TimeSlotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class TimeSlotManagementIntegrationTest {

    @Autowired
    private TimeSlotService timeSlotService;
    @Autowired
    private TimeSlotRepository timeSlotRepository;
    @Autowired
    private SpecialistProfileRepository profileRepository;
    @Autowired
    private UserRepository userRepository;

    private User specUser;
    private SpecialistProfile specialist;

    @BeforeEach
    void setUp() {
        // Initialize the specialist with all required NOT NULL fields
        specUser = new User();
        specUser.setUsername("schedule_doctor");
        specUser.setEmail("schedule@test.com");
        specUser.setPassword("password123");
        specUser.setRole(UserRole.SPECIALIST);
        specUser.setCreatedAt(LocalDateTime.now());
        userRepository.save(specUser);

        specialist = new SpecialistProfile();
        specialist.setUser(specUser);
        specialist.setStatus(SpecialistStatus.ACTIVE);
        specialist.setHourlyFee(new BigDecimal("150.00"));
        specialist.setLevel(SpecialistLevel.EXPERT);
        specialist.setRealName("Dr. Schedule");
        profileRepository.save(specialist);
    }

    @Test
    void publishAndRemoveScheduleFlow() {
        // --- STEP 1: Specialist publishes a batch of new schedules ---
        TimeSlotService.DailySlot slot1 = new TimeSlotService.DailySlot(
                LocalDate.now().plusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0));
        TimeSlotService.DailySlot slot2 = new TimeSlotService.DailySlot(
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(11, 0));

        TimeSlotService.TimeSlotBatchRequest request = new TimeSlotService.TimeSlotBatchRequest(List.of(slot1, slot2));

        timeSlotService.batchCreateSlots(specUser.getUsername(), request);

        // Verify: Schedules should be saved in the database with AVAILABLE status
        List<TimeSlot> savedSlots = timeSlotRepository.findBySpecialistId(specialist.getId());
        assertEquals(2, savedSlots.size());
        assertEquals(TimeSlotStatus.AVAILABLE, savedSlots.get(0).getStatus());

        // --- STEP 2: Specialist deletes an available schedule ---
        Long slotIdToDelete = savedSlots.get(0).getId();
        timeSlotService.deleteTimeSlot(specUser.getUsername(), slotIdToDelete);

        // Verify: The schedule should be permanently removed from the database
        assertTrue(timeSlotRepository.findById(slotIdToDelete).isEmpty());
        assertEquals(1, timeSlotRepository.findBySpecialistId(specialist.getId()).size());
    }
}