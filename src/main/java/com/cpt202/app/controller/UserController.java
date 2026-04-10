package com.cpt202.app.controller;

import com.cpt202.app.model.User;
import com.cpt202.app.model.UserRole;
import com.cpt202.app.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            User created = userService.register(
                    request.username(),
                    request.password(),
                    request.email(),
                    request.role(),
                    request.verifyCode()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "注册成功",
                    "userId", created.getId()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            User user = userService.login(request.usernameOrEmail(), request.password());
            return ResponseEntity.ok(Map.of(
                    "message", "登录成功",
                    "userId", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "role", user.getRole().name()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/verify-code")
    public ResponseEntity<?> sendVerifyCode(@RequestBody SendCodeRequest request) {
        try {
            userService.sendVerifyCode(request.email());
            return ResponseEntity.ok(Map.of("message", "验证码已发送到邮箱"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        try {
            userService.changePassword(request.email(), request.oldPassword(), request.newPassword());
            return ResponseEntity.ok(Map.of("message", "密码修改成功"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            userService.forgotPassword(request.email(), request.verifyCode(), request.newPassword());
            return ResponseEntity.ok(Map.of("message", "忘记密码重置成功"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    public record RegisterRequest(
            String username,
            String password,
            String email,
            UserRole role,
            String verifyCode
    ) {}

    public record LoginRequest(String usernameOrEmail, String password) {}

    public record SendCodeRequest(String email) {}

    public record ForgotPasswordRequest(String email, String verifyCode, String newPassword) {}

    public record ChangePasswordRequest(String email, String oldPassword, String newPassword) {}
}
