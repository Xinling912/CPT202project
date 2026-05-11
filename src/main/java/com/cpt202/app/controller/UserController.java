package com.cpt202.app.controller;

import com.cpt202.app.model.User;
import com.cpt202.app.model.UserRole;
import com.cpt202.app.security.JwtUtils;
import com.cpt202.app.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public UserController(UserService userService, AuthenticationManager authenticationManager, JwtUtils jwtUtils) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
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
                    "message", "Registration successful",
                    "userId", created.getId()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            // 1. Check for null (using Record's method name)
            if (request.usernameOrEmail() == null || request.password() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Username or password cannot be empty"));
            }

            String loginInput = request.usernameOrEmail().trim();
            String username = loginInput;

            // 2. If the input is an email, we need to get the real username, because AuthenticationManager uses username for comparison by default
            if (loginInput.contains("@")) {
                try {
                    User userByEmail = userService.getByEmail(loginInput);
                    username = userByEmail.getUsername();
                } catch (Exception e) {
                    // User not found by email, throw authentication exception directly, do not let the system crash
                    throw new BadCredentialsException("Incorrect username or password");
                }
            }

            // 3. Call the standard Spring Security authentication process
            // Note: This will call your CustomUserDetailsService.loadUserByUsername
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.password())
            );

            // 4. Authentication successful, generate Token
            String authenticatedUsername = authentication.getName();
            String token = jwtUtils.generateToken(authenticatedUsername);

            // 5. At this time, get user information again and return it to the frontend
            User user = userService.getByUsername(authenticatedUsername);

            return ResponseEntity.ok(Map.of(
                    "message", "Login successful",
                    "token", token,
                    "tokenType", "Bearer",
                    "userId", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "role", user.getRole().name()
            ));

        } catch (AuthenticationException e) {
            // Catch all authentication-related exceptions (account does not exist, incorrect password, locked, etc.)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Incorrect username or password"));
        } catch (Exception e) {
            // [Core] Catch all unexpected errors to prevent Socket Hang Up
            e.printStackTrace(); // Print specific stack trace information in the console
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Internal server error"));
        }
    }

    @PostMapping("/verify-code")
    public ResponseEntity<?> sendVerifyCode(@RequestBody SendCodeRequest request) {
        try {
            userService.sendVerifyCode(request.email());
            return ResponseEntity.ok(Map.of("message", "Verification code has been sent to the email"));
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Must carry token
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            Authentication authentication,
            @RequestBody Map<String, String> request) { // You can also use a dedicated Request DTO
        try {
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");

            // Extract the current username from JWT (Security context)
            String currentUsername = authentication.getName();

            userService.changePassword(currentUsername, oldPassword, newPassword);

            return ResponseEntity.ok(Map.of("message", "Password changed successfully, please log in again with the new password"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            userService.forgotPassword(request.email(), request.verifyCode(), request.newPassword());
            return ResponseEntity.ok(Map.of("message", "Forgot password reset successful"));
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

    public record ChangePasswordRequest(String oldPassword, String newPassword) {}
}