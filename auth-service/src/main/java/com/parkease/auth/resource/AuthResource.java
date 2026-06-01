package com.parkease.auth.resource;

import com.parkease.auth.dto.*;
import com.parkease.auth.entity.User;
import com.parkease.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration, login and profile management")
public class AuthResource {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/profile")
    @Operation(summary = "Get authenticated user profile")
    public ResponseEntity<User> getProfile(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(authService.getProfile(userId));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update profile details")
    public ResponseEntity<User> updateProfile(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.updateProfile(userId, request));
    }

    @PutMapping("/password")
    @Operation(summary = "Change password")
    public ResponseEntity<Void> changePassword(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, String> body) {
        authService.changePassword(userId, body.get("oldPassword"), body.get("newPassword"));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/deactivate")
    @Operation(summary = "Deactivate account")
    public ResponseEntity<Void> deactivate(@RequestHeader("X-User-Id") Long userId) {
        authService.deactivateAccount(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    @Operation(summary = "Get all users by role (Admin only)")
    public ResponseEntity<?> getUsersByRole(
            @RequestParam User.Role role,
            @RequestHeader("X-User-Role") String callerRole) {
        if (!"ADMIN".equals(callerRole)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(authService.getUsersByRole(role));
    }
}
