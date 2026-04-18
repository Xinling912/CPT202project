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
            // 1. 判空 (使用 Record 的方法名)
            if (request.usernameOrEmail() == null || request.password() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "账号或密码不能为空"));
            }

            String loginInput = request.usernameOrEmail().trim();
            String username = loginInput;

            // 2. 如果输入的是邮箱，我们需要拿到真正的 username，因为 AuthenticationManager 默认是用 username 比对的
            if (loginInput.contains("@")) {
                try {
                    User userByEmail = userService.getByEmail(loginInput);
                    username = userByEmail.getUsername();
                } catch (Exception e) {
                    // 邮箱查不到用户，直接抛出认证异常，不要让系统挂掉
                    throw new BadCredentialsException("账号或密码错误");
                }
            }

            // 3. 调用 Spring Security 标准认证流程
            // 注意：这里会去调用你的 CustomUserDetailsService.loadUserByUsername
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.password())
            );

            // 4. 认证成功，生成 Token
            String authenticatedUsername = authentication.getName();
            String token = jwtUtils.generateToken(authenticatedUsername);

            // 5. 此时再获取用户信息返回给前端
            User user = userService.getByUsername(authenticatedUsername);

            return ResponseEntity.ok(Map.of(
                    "message", "登录成功",
                    "token", token,
                    "tokenType", "Bearer",
                    "userId", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "role", user.getRole().name()
            ));

        } catch (AuthenticationException e) {
            // 捕获所有认证相关的异常（账号不存在、密码错误、被锁定等）
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "账号或密码错误"));
        } catch (Exception e) {
            // 【核心】捕获所有意料之外的错误，防止 Socket Hang Up
            e.printStackTrace(); // 在控制台打印具体的堆栈信息
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "服务器内部错误"));
        }
    }

    @PostMapping("/verify-code")
    public ResponseEntity<?> sendVerifyCode(@RequestBody SendCodeRequest request) {
        try {
            userService.sendVerifyCode(request.email());
            return ResponseEntity.ok(Map.of("message", "验证码已发送到邮箱"));
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    //必须携带token
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            Authentication authentication,
            @RequestBody Map<String, String> request) { // 你也可以用专门的 Request DTO
        try {
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");

            // 从 JWT (Security 上下文) 中提取当前用户名
            String currentUsername = authentication.getName();

            userService.changePassword(currentUsername, oldPassword, newPassword);

            return ResponseEntity.ok(Map.of("message", "密码修改成功，请使用新密码重新登录"));
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

    public record ChangePasswordRequest(String oldPassword, String newPassword) {}
}
