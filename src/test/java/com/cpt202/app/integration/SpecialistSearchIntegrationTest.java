package com.cpt202.app.integration;

import com.cpt202.app.model.*;
import com.cpt202.app.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional // Ensures test data is isolated and rolled back after each test case[cite: 1]
class SpecialistSearchIntegrationTest {

    @Autowired
    private SpecialistProfileRepository specialistRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpertiseCategoryRepository expertiseCategoryRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    private ExpertiseCategory techCategory;
    private ExpertiseCategory healthCategory;
    private SpecialistProfile lisaTech;
    private SpecialistProfile bobHealth;

    @BeforeEach
    void setUp() {
        // 1. Initialize distinct Expertise Categories[cite: 1]
        techCategory = new ExpertiseCategory();
        techCategory.setName("Technology");
        expertiseCategoryRepository.save(techCategory);

        healthCategory = new ExpertiseCategory();
        healthCategory.setName("Health");
        expertiseCategoryRepository.save(healthCategory);

        // 2. Create Specialist A: Lisa (Tech, Expert)[cite: 1]
        lisaTech = createSpecialist("lisa_tech", "lisa@test.com", "Lisa Wang", techCategory, SpecialistLevel.EXPERT);

        // 3. Create Specialist B: Bob (Health, Junior)[cite: 1]
        bobHealth = createSpecialist("bob_health", "bob@test.com", "Bob Smith", healthCategory, SpecialistLevel.JUNIOR);

        // 4. Create an AVAILABLE TimeSlot for Lisa on a specific date[cite: 1]
        createAvailableSlot(lisaTech, LocalDate.now().plusDays(1));
    }

    private SpecialistProfile createSpecialist(String username, String email, String realName, ExpertiseCategory category, SpecialistLevel level) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password123");
        user.setRole(UserRole.SPECIALIST);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        SpecialistProfile profile = new SpecialistProfile();
        profile.setUser(user);
        profile.setRealName(realName);
        profile.setExpertise(category); // Matches the join field 'expertise' in your Controller[cite: 1]
        profile.setLevel(level);
        profile.setStatus(SpecialistStatus.ACTIVE);
        profile.setHourlyFee(new BigDecimal("100.00"));
        return specialistRepository.save(profile);
    }

    private void createAvailableSlot(SpecialistProfile sp, LocalDate date) {
        TimeSlot slot = new TimeSlot();
        slot.setSpecialist(sp);
        slot.setSlotDate(date);
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(10, 0));
        slot.setStatus(TimeSlotStatus.AVAILABLE);
        timeSlotRepository.save(slot);
    }
    // --- SCENARIO 1: Combined Multi-Filter Search ---
    @Test
    void searchByAllCriteriaCombined_ShouldFindLisa() {
        // Simulating: Search "lisa" + "Tech category" + "Expert level" + "Tomorrow available"[cite: 1]
        Specification<SpecialistProfile> spec = buildBaseSpec();
        spec = addKeywordFilter(spec, "lisa");
        spec = addExpertiseFilter(spec, techCategory.getId());
        spec = addLevelFilter(spec, SpecialistLevel.EXPERT);
        spec = addDateFilter(spec, LocalDate.now().plusDays(1));
        Page<SpecialistProfile> result = specialistRepository.findAll(spec, PageRequest.of(0, 10));
        assertEquals(1, result.getTotalElements());
        assertEquals("lisa_tech", result.getContent().get(0).getUser().getUsername());
    }
    // --- SCENARIO 2: Single Dimension - Expertise Only ---
    @Test
    void searchByExpertiseOnly_ShouldReturnOnlyTechSpecialists() {
        Specification<SpecialistProfile> spec = buildBaseSpec();
        spec = addExpertiseFilter(spec, techCategory.getId());

        Page<SpecialistProfile> result = specialistRepository.findAll(spec, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Lisa Wang", result.getContent().get(0).getRealName());
    }
    // --- SCENARIO 3: Single Dimension - Level Only ---
    @Test
    void searchByLevelOnly_ShouldReturnOnlyJuniorSpecialists() {
        Specification<SpecialistProfile> spec = buildBaseSpec();
        spec = addLevelFilter(spec, SpecialistLevel.JUNIOR);

        Page<SpecialistProfile> result = specialistRepository.findAll(spec, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Bob Smith", result.getContent().get(0).getRealName());
    }
    // --- SCENARIO 4: Single Dimension - Keyword Only ---
    @Test
    void searchByKeywordOnly_ShouldReturnMatchingUsernames() {
        Specification<SpecialistProfile> spec = buildBaseSpec();
        spec = addKeywordFilter(spec, "bob");

        Page<SpecialistProfile> result = specialistRepository.findAll(spec, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("bob_health", result.getContent().get(0).getUser().getUsername());
    }
    // --- SCENARIO 5: Date Availability Check ---
    @Test
    void searchByDateOnly_ShouldReturnSpecialistsWithOpenSlots() {
        Specification<SpecialistProfile> spec = buildBaseSpec();
        spec = addDateFilter(spec, LocalDate.now().plusDays(1));

        Page<SpecialistProfile> result = specialistRepository.findAll(spec, PageRequest.of(0, 10));

        // Only Lisa has a slot tomorrow[cite: 1]
        assertEquals(1, result.getTotalElements());
        assertEquals("Lisa Wang", result.getContent().get(0).getRealName());
    }
    // --- Helper Methods replicating Controller logic[cite: 1] ---
    private Specification<SpecialistProfile> buildBaseSpec() {
        return (root, query, cb) -> cb.equal(root.get("status"), SpecialistStatus.ACTIVE);
    }
    private Specification<SpecialistProfile> addKeywordFilter(Specification<SpecialistProfile> spec, String k) {
        return spec.and((root, query, cb) ->
                cb.like(cb.lower(root.join("user").get("username")), "%" + k.toLowerCase() + "%"));
    }
    private Specification<SpecialistProfile> addExpertiseFilter(Specification<SpecialistProfile> spec, Long id) {
        return spec.and((root, query, cb) -> cb.equal(root.join("expertise").get("id"), id));
    }
    private Specification<SpecialistProfile> addLevelFilter(Specification<SpecialistProfile> spec, SpecialistLevel lv) {
        return spec.and((root, query, cb) -> cb.equal(root.get("level"), lv));
    }
    private Specification<SpecialistProfile> addDateFilter(Specification<SpecialistProfile> spec, LocalDate date) {
        return spec.and((root, query, cb) -> {
            var subquery = query.subquery(Long.class);
            var timeSlotRoot = subquery.from(TimeSlot.class);
            subquery.select(cb.literal(1L));
            subquery.where(
                    cb.equal(timeSlotRoot.get("specialist"), root),
                    cb.equal(timeSlotRoot.get("status"), TimeSlotStatus.AVAILABLE),
                    cb.equal(timeSlotRoot.get("slotDate"), date)
            );
            return cb.exists(subquery);
        });
    }
}