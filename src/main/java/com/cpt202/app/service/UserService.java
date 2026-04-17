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
            throw new IllegalArgumentException("用户名、密码、邮箱不能为空");
        }
        String normalizedUsername = username.trim();
        String normalizedEmail = email.trim().toLowerCase();
        validateRegistrationFormat(normalizedUsername, rawPassword, normalizedEmail);

        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("用户名已存在");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("邮箱已被注册");
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

    //旧login逻辑，现已遗弃，使用Spring Security 无状态认证
    /* 1. 拦截接管：登录请求已交由 Controller 层的 AuthenticationManager.authenticate() 接管。
     * 2. 自动查档：底层会自动调用我们自定义的 CustomUserDetailsService 去数据库查询用户信息（含 BCrypt 密文）。
     * 3. 自动比对：底层会自动调用 BCryptPasswordEncoder.matches() 完成明文与密文的安全比对。*/
//    public User login(String usernameOrEmail, String rawPassword) {
//        if (isBlank(usernameOrEmail) || isBlank(rawPassword)) {
//            throw new IllegalArgumentException("账号和密码不能为空");
//        }
//        String key = usernameOrEmail.trim();
//        Optional<User> maybeUser = key.contains("@")
//                ? userRepository.findByEmail(key.toLowerCase())
//                : userRepository.findByUsername(key);
//
//        User user = maybeUser.orElseThrow(() -> new IllegalArgumentException("用户不存在"));
//        if (!user.getPassword().equals(encodePassword(rawPassword))) {
//            throw new IllegalArgumentException("密码错误");
//        }
//        return user;
//    }

    public User getByUsername(String username) {
        if (isBlank(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        return userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    public User getByEmail(String email) {
        if (isBlank(email)) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }


    public void sendVerifyCode(String email) {
        if (isBlank(email)) {
            throw new IllegalArgumentException("邮箱不能为空");
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
            throw new IllegalStateException("验证码发送失败，请稍后重试", e);
        }
    }

    @Transactional
    public void changePassword(String currentUsername, String oldPassword, String newPassword) {
        // 1. 基础校验
        if (isBlank(currentUsername) || isBlank(oldPassword) || isBlank(newPassword)) {
            throw new IllegalArgumentException("参数不能为空");
        }
        validatePasswordFormat(newPassword);

        // 2. 直接根据系统上下文中获取的用户名查出用户
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("用户状态异常，请重新登录"));

        // 3. 安全比对旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("旧密码错误");
        }

        // 4. 加密并保存新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void forgotPassword(String email, String verifyCode, String newPassword) {
        if (isBlank(email) || isBlank(newPassword)) {
            throw new IllegalArgumentException("邮箱和新密码不能为空");
        }
        String normalizedEmail = email.trim().toLowerCase();
        validateEmailFormat(normalizedEmail);
        validatePasswordFormat(newPassword);
        validateVerifyCode(normalizedEmail, verifyCode);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

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
            throw new IllegalArgumentException("验证码不能为空");
        }
        VerifyCodeInfo info = verifyCodeStore.get(email.trim().toLowerCase());
        if (info == null) {
            throw new IllegalArgumentException("请先发送验证码");
        }
        if (LocalDateTime.now().isAfter(info.expireAt())) {
            verifyCodeStore.remove(email.trim().toLowerCase());
            throw new IllegalArgumentException("验证码已过期");
        }
        if (!info.code().equals(code.trim())) {
            throw new IllegalArgumentException("验证码错误");
        }
    }
// 旧加密方式SHA-256，现已经遗弃，使用Security中的BCrypt加密
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
//            throw new IllegalStateException("密码加密失败", e);
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
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (username.length() > 10) {
            throw new IllegalArgumentException("用户名不能超过10个字符");
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("用户名格式不合法：只能包含中英文、数字、下划线");
        }
    }

    private void validatePasswordFormat(String password) {
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException("密码格式不合法：需为8-32位，且至少包含字母和数字");
        }
    }

    private void validateEmailFormat(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("邮箱格式不合法");
        }
    }

    private record VerifyCodeInfo(String code, LocalDateTime expireAt) {
    }
}
