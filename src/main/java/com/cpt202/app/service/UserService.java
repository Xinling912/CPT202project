package com.cpt202.app.service;

import com.cpt202.app.model.User;
import com.cpt202.app.model.UserRole;
import com.cpt202.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final Map<String, VerifyCodeInfo> verifyCodeStore = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private static final int EXPIRE_MINUTES = 5;
    private final String fromAddress;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[\\w\\u4e00-\\u9fa5]{1,10}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,32}$");

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String fromAddress
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Transactional
    public User register(String username, String rawPassword, String email, UserRole role, String verifyCode) {
        if (isBlank(username) || isBlank(rawPassword) || isBlank(email)) {
            throw new IllegalArgumentException("Username, password, and email cannot be empty");
        }
        String normalizedUsername = username.trim();
        String normalizedEmail = email.trim().toLowerCase();
        validateRegistrationFormat(normalizedUsername, rawPassword, normalizedEmail);

        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email is already registered");
        }
        validateVerifyCode(normalizedEmail, verifyCode);

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEmail(normalizedEmail);
        user.setRole(role == null ? UserRole.CUSTOMER : role);
        user.setCreatedAt(LocalDateTime.now());

        verifyCodeStore.remove(normalizedEmail);
        return userRepository.save(user);
    }

    /**Legacy login logic, now deprecated, using Spring Security stateless authentication */
//    public User login(String usernameOrEmail, String rawPassword) {
//        if (isBlank(usernameOrEmail) || isBlank(rawPassword)) {
//            throw new IllegalArgumentException("Account and password cannot be empty");
//        }
//        String key = usernameOrEmail.trim();
//        Optional<User> maybeUser = key.contains("@")
//                ? userRepository.findByEmail(key.toLowerCase())
//                : userRepository.findByUsername(key);
//
//        User user = maybeUser.orElseThrow(() -> new IllegalArgumentException("User not found"));
//        if (!user.getPassword().equals(encodePassword(rawPassword))) {
//            throw new IllegalArgumentException("Incorrect password");
//        }
//        return user;
//    }

    public User getByUsername(String username) {
        if (isBlank(username)) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        return userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public User getByEmail(String email) {
        if (isBlank(email)) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }


    public void sendVerifyCode(String email) {
        if (isBlank(email)) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        String normalizedEmail = email.trim().toLowerCase();
        validateEmailFormat(normalizedEmail);
        String code = String.format("%06d", random.nextInt(1_000_000));
        verifyCodeStore.put(normalizedEmail, new VerifyCodeInfo(code, LocalDateTime.now().plusMinutes(EXPIRE_MINUTES)));

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(normalizedEmail);
            message.setSubject("Your verification code");
            message.setText("Your verification code is: " + code + "\nIt will expire in " + EXPIRE_MINUTES + " minutes.");
            mailSender.send(message);
        } catch (MailException e) {
            verifyCodeStore.remove(normalizedEmail);
            throw new IllegalStateException("Failed to send verification code, please try again later", e);
        }
    }

    @Transactional
    public void changePassword(String currentUsername, String oldPassword, String newPassword) {
        // 1. Basic validation
        if (isBlank(currentUsername) || isBlank(oldPassword) || isBlank(newPassword)) {
            throw new IllegalArgumentException("Parameters cannot be empty");
        }
        validatePasswordFormat(newPassword);

        // 2. Query user directly based on the username obtained from the system context
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Abnormal user status, please log in again"));

        // 3. Securely compare the old password
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Incorrect old password");
        }

        // 4. Encrypt and save the new password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void forgotPassword(String email, String verifyCode, String newPassword) {
        if (isBlank(email) || isBlank(newPassword)) {
            throw new IllegalArgumentException("Email and new password cannot be empty");
        }
        String normalizedEmail = email.trim().toLowerCase();
        validateEmailFormat(normalizedEmail);
        validatePasswordFormat(newPassword);
        validateVerifyCode(normalizedEmail, verifyCode);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        verifyCodeStore.remove(normalizedEmail);
    }

    @Scheduled(fixedRate = 600000)
    public void cleanUpExpiredCodes() {
        LocalDateTime now = LocalDateTime.now();
        verifyCodeStore.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expireAt()));
    }

    private void validateVerifyCode(String email, String code) {
        if (isBlank(code)) {
            throw new IllegalArgumentException("Verification code cannot be empty");
        }
        VerifyCodeInfo info = verifyCodeStore.get(email.trim().toLowerCase());
        if (info == null) {
            throw new IllegalArgumentException("Please send a verification code first");
        }
        if (LocalDateTime.now().isAfter(info.expireAt())) {
            verifyCodeStore.remove(email.trim().toLowerCase());
            throw new IllegalArgumentException("Verification code has expired");
        }
        if (!info.code().equals(code.trim())) {
            throw new IllegalArgumentException("Incorrect verification code");
        }
    }

    /** Legacy encryption method SHA-256, now deprecated, using BCrypt in Security*/
//    private String encodePassword(String rawPassword) {
//        try {
//            MessageDigest digest = MessageDigest.getInstance("SHA-256");
//            byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
//            StringBuilder sb = new StringBuilder();
//            for (byte b : hash) {
//                sb.append(String.format("%02x", b));
//            }
//            return sb.toString();
//        } catch (NoSuchAlgorithmException e) {
//            throw new IllegalStateException("Password encryption failed", e);
//        }
//    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    private void validateRegistrationFormat(String username, String password, String email) {
        validateUsernameFormat(username);
        validatePasswordFormat(password);
        validateEmailFormat(email);
    }

    private void validateUsernameFormat(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (username.length() > 10) {
            throw new IllegalArgumentException("Username cannot exceed 10 characters");
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("Invalid username format: only Chinese/English letters, numbers, and underscores are allowed");
        }
    }

    private void validatePasswordFormat(String password) {
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException("Invalid password format: must be 8-32 characters and contain at least letters and numbers");
        }
    }

    private void validateEmailFormat(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }

    private record VerifyCodeInfo(String code, LocalDateTime expireAt) {
    }
}