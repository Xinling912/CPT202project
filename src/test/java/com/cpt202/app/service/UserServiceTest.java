package com.cpt202.app.service;

import com.cpt202.app.model.User;
import com.cpt202.app.model.UserRole;
import com.cpt202.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JavaMailSender mailSender;

    // We don't use @InjectMocks here because of the @Value("${app.mail.from}") primitive string injection.
    // Manual instantiation is safer and cleaner for testing.
    private UserService userService;

    private final String testEmail = "test@example.com";
    private final String testUsername = "Justin_123";
    private final String validPassword = "Password123"; // Meets regex: letters + numbers, 8-32 chars

    @BeforeEach
    void setUp() {
        // Instantiate the service manually, providing a dummy "from" address
        userService = new UserService(userRepository, passwordEncoder, mailSender, "noreply@selene.com");
    }

    // ==========================================
    // 1. Send Verification Code Tests
    // ==========================================
    @Test
    void sendVerifyCode_HappyPath_ShouldSendEmail() {
        // Act
        userService.sendVerifyCode(testEmail);

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        // Verify that mailSender.send() was called EXACTLY once, and capture the email object
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentEmail = messageCaptor.getValue();
        assertEquals(testEmail, sentEmail.getTo()[0]);
        assertTrue(sentEmail.getText().contains("Your verification code is:"));
    }

    @Test
    void sendVerifyCode_InvalidEmail_ShouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> userService.sendVerifyCode("invalid-email-format"));
    }


    // ==========================================
    // 2. Register Tests
    // ==========================================
    @Test
    void register_HappyPath_ShouldSucceed() {
        // Arrange
        when(userRepository.existsByUsername(testUsername)).thenReturn(false);
        when(userRepository.existsByEmail(testEmail)).thenReturn(false);
        when(passwordEncoder.encode(validPassword)).thenReturn("encoded_password");

        // Intercept save to return a user with an ID
        User mockSavedUser = new User();
        mockSavedUser.setId(1L);
        mockSavedUser.setUsername(testUsername);
        when(userRepository.save(any(User.class))).thenReturn(mockSavedUser);

        // --- THE MAGIC TRICK: Generate and capture a valid verify code ---
        userService.sendVerifyCode(testEmail);
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        String emailText = captor.getValue().getText();
        // Extract the 6-digit code using regex (keeps only digits, takes first 6)
        String validCode = emailText.replaceAll("\\D+", "").substring(0, 6);

        // Act
        User result = userService.register(testUsername, validPassword, testEmail, UserRole.CUSTOMER, validCode);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(testUsername, result.getUsername());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_DuplicateUsername_ShouldThrowException() {
        // Arrange
        when(userRepository.existsByUsername(testUsername)).thenReturn(true);

        // Act & Assert
        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            userService.register(testUsername, validPassword, testEmail, UserRole.CUSTOMER, "123456");
        });
        assertEquals("用户名已存在", e.getMessage());
    }

    @Test
    void register_InvalidPasswordFormat_ShouldThrowException() {
        // Act & Assert
        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            // Password missing letters
            userService.register(testUsername, "12345678", testEmail, UserRole.CUSTOMER, "123456");
        });
        assertTrue(e.getMessage().contains("密码格式不合法"));
    }


    // ==========================================
    // 3. Change Password Tests
    // ==========================================
    @Test
    void changePassword_HappyPath_ShouldSucceed() {
        // Arrange
        String oldPassword = "OldPassword123";
        String newPassword = "NewPassword456";

        User existingUser = new User();
        existingUser.setUsername(testUsername);
        existingUser.setPassword("encoded_old_password");

        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(existingUser));
        // Mock the password matching logic to return true (old password is correct)
        when(passwordEncoder.matches(oldPassword, existingUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn("encoded_new_password");

        // Act
        userService.changePassword(testUsername, oldPassword, newPassword);

        // Assert
        assertEquals("encoded_new_password", existingUser.getPassword()); // Verify memory object updated
        verify(userRepository, times(1)).save(existingUser); // Verify DB save triggered
    }

    @Test
    void changePassword_WrongOldPassword_ShouldThrowException() {
        // Arrange
        User existingUser = new User();
        existingUser.setUsername(testUsername);
        existingUser.setPassword("encoded_old_password");

        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(existingUser));
        // Mock the password matching logic to return FALSE (wrong old password)
        when(passwordEncoder.matches("WrongOldPass1", existingUser.getPassword())).thenReturn(false);

        // Act & Assert
        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            userService.changePassword(testUsername, "WrongOldPass1", "NewPassword456");
        });
        assertEquals("旧密码错误", e.getMessage());
    }


    // ==========================================
    // 4. Forgot Password Tests
    // ==========================================
    @Test
    void forgotPassword_HappyPath_ShouldSucceed() {
        // Arrange
        String newPassword = "ResetPassword123";
        User existingUser = new User();
        existingUser.setEmail(testEmail);

        // Generate and capture valid code
        userService.sendVerifyCode(testEmail);
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        String validCode = captor.getValue().getText().replaceAll("\\D+", "").substring(0, 6);

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode(newPassword)).thenReturn("encoded_reset_password");

        // Act
        userService.forgotPassword(testEmail, validCode, newPassword);

        // Assert
        assertEquals("encoded_reset_password", existingUser.getPassword());
        verify(userRepository, times(1)).save(existingUser);
    }

    // ==========================================
    // 5. Query Methods Tests
    // ==========================================
    @Test
    void getByUsername_Exists_ShouldReturnUser() {
        // Arrange
        User dummyUser = new User();
        dummyUser.setUsername(testUsername);
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(dummyUser));

        // Act
        User result = userService.getByUsername(testUsername);

        // Assert
        assertEquals(testUsername, result.getUsername());
    }

    @Test
    void getByEmail_NotExists_ShouldThrowException() {
        // Arrange
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> userService.getByEmail(testEmail));
    }

    // ==========================================
    // 6. Scheduled Task Test: Clean Up Expired Codes
    // ==========================================
    @Test
    void cleanUpExpiredCodes_ShouldRemoveExpiredCodes() throws Exception {
        // Arrange
        // 1. Advanced technique: Use Java Reflection to access the private verifyCodeStore inside UserService.
        java.lang.reflect.Field storeField = UserService.class.getDeclaredField("verifyCodeStore");
        storeField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> store = (java.util.Map<String, Object>) storeField.get(userService);

        // 2. Use reflection to instantiate the private record VerifyCodeInfo with an EXPIRED time
        // Since the record is private to UserService, we must construct it reflectively
        Class<?> recordClass = Class.forName("com.cpt202.app.service.UserService$VerifyCodeInfo");
        java.lang.reflect.Constructor<?> constructor = recordClass.getDeclaredConstructor(String.class, java.time.LocalDateTime.class);
        constructor.setAccessible(true);
        // Create a record that expired 10 minutes ago
        Object expiredRecord = constructor.newInstance("123456", java.time.LocalDateTime.now().minusMinutes(10));

        // 3. Put this expired record directly into the store
        store.put(testEmail, expiredRecord);

        // Confirm there is exactly 1 code in the cache right now.
        assertEquals(1, store.size());

        // Act
        // Manually invoke the scheduled cleanup task.
        userService.cleanUpExpiredCodes();

        // Assert
        // The code has expired, so the cache should be cleared completely (size becomes 0).
        assertEquals(0, store.size());
    }
}