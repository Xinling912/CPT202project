package com.cpt202.app.acceptance;

import com.cpt202.app.model.*;
import com.cpt202.app.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@SpringBootTest
@Transactional // Ensures database atomicity: all data is rolled back after each test to keep the DB clean
public abstract class BaseAcceptanceTest {

    @Autowired protected UserRepository userRepository;
    @Autowired protected SpecialistProfileRepository specialistRepository;
    @Autowired protected ExpertiseCategoryRepository expertiseRepository;
    @Autowired protected TimeSlotRepository timeSlotRepository;
    @Autowired protected BookingRepository bookingRepository;
    @Autowired protected SpecialistProfileEditRequestRepository editRequestRepository;

    /**
     * Data Factory: Creates a basic User entity
     * @param username Unique identifier for the user
     * @param role UserRole (CUSTOMER, SPECIALIST, or ADMIN)
     * @return The persisted User entity
     */
    protected User createTestUser(String username, UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("encoded_password"); // Placeholder password for authentication simulation
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    /**
     * Data Factory: Creates a Specialist with an ACTIVE status profile
     * @param username Unique identifier for the specialist user
     * @param categoryName Name of the expertise category to create
     * @return The persisted SpecialistProfile entity
     */
    protected SpecialistProfile createActiveSpecialist(String username, String categoryName) {
        User user = createTestUser(username, UserRole.SPECIALIST);

        ExpertiseCategory category = new ExpertiseCategory();
        category.setName(categoryName);
        expertiseRepository.save(category);

        SpecialistProfile profile = new SpecialistProfile();
        profile.setUser(user);
        profile.setRealName(username + "_RealName");
        profile.setExpertise(category); // Links to the expertise field used in Controller logic
        profile.setLevel(SpecialistLevel.EXPERT);
        profile.setStatus(SpecialistStatus.ACTIVE);
        profile.setHourlyFee(new BigDecimal("100.00"));
        return specialistRepository.save(profile);
    }
}