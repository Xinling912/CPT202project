package com.cpt202.app.integration;

import com.cpt202.app.model.User;
import com.cpt202.app.model.UserRole;
import com.cpt202.app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UserAuthIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void userRegistrationAndLoginSimulation() {
        // --- STEP 1: Simulate User Registration ---
        String targetEmail = "new_user@example.com";
        String targetUsername = "new_customer";

        User newUser = new User();
        newUser.setUsername(targetUsername);
        newUser.setEmail(targetEmail);
        // In a real scenario, this would be an encoded password (e.g., BCrypt)
        newUser.setPassword("SecurePassword123!");
        newUser.setRole(UserRole.CUSTOMER);
        newUser.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(newUser);

        // Verify Registration: User should be persisted with an ID
        assertNotNull(savedUser.getId());
        assertEquals(UserRole.CUSTOMER, savedUser.getRole());

        // --- STEP 2: Simulate User Login (Query by Username or Email) ---
        // Simulating the backend lookup during a login attempt
        Optional<User> loginAttemptByUsername = userRepository.findByUsername(targetUsername);

        // Verify Login: System should successfully retrieve the user details
        assertTrue(loginAttemptByUsername.isPresent());
        assertEquals(targetEmail, loginAttemptByUsername.get().getEmail());

        // Ensure the password hash is retrievable for security validation
        assertNotNull(loginAttemptByUsername.get().getPassword());
    }
}