package com.parkease.auth.service;

import com.parkease.auth.dto.AuthResponse;
import com.parkease.auth.dto.LoginRequest;
import com.parkease.auth.dto.RegisterRequest;
import com.parkease.auth.dto.UpdateProfileRequest;
import com.parkease.auth.entity.User;
import com.parkease.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;

    @InjectMocks AuthService authService;

    User savedUser;

    @BeforeEach
    void setUp() {
        savedUser = User.builder()
                .userId(1L)
                .fullName("Jane Doe")
                .email("jane@test.com")
                .passwordHash("hashed_password")
                .role(User.Role.DRIVER)
                .isActive(true)
                .build();
    }

    // ── register ────────────────────────────────────────────────────────────

    @Test
    void register_success_returnsAuthResponseWithToken() {
        RegisterRequest req = RegisterRequest.builder()
                .fullName("Jane Doe").email("jane@test.com")
                .password("password123").role(User.Role.DRIVER).build();

        given(userRepository.existsByEmail("jane@test.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("hashed_password");
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(jwtService.generateToken(savedUser)).willReturn("jwt-token");

        AuthResponse response = authService.register(req);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo("jane@test.com");
        assertThat(response.getRole()).isEqualTo(User.Role.DRIVER);
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        then(userRepository).should().save(any(User.class));
    }

    @Test
    void register_encodesPassword_beforeSaving() {
        RegisterRequest req = RegisterRequest.builder()
                .fullName("Jane Doe").email("jane@test.com")
                .password("password123").role(User.Role.DRIVER).build();

        given(userRepository.existsByEmail(any())).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("hashed_password");
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(jwtService.generateToken(any())).willReturn("token");

        authService.register(req);

        then(userRepository).should().save(argThat(u -> u.getPasswordHash().equals("hashed_password")));
    }

    @Test
    void register_throwsConflict_whenEmailAlreadyExists() {
        RegisterRequest req = RegisterRequest.builder()
                .fullName("Jane").email("jane@test.com")
                .password("password123").role(User.Role.DRIVER).build();

        given(userRepository.existsByEmail("jane@test.com")).willReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));

        then(userRepository).should(never()).save(any());
    }

    // ── login ────────────────────────────────────────────────────────────────

    @Test
    void login_success_returnsTokenAndUserDetails() {
        LoginRequest req = new LoginRequest("jane@test.com", "password123");

        given(userRepository.findByEmail("jane@test.com")).willReturn(Optional.of(savedUser));
        given(passwordEncoder.matches("password123", "hashed_password")).willReturn(true);
        given(jwtService.generateToken(savedUser)).willReturn("jwt-token");

        AuthResponse response = authService.login(req);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getFullName()).isEqualTo("Jane Doe");
    }

    @Test
    void login_throwsUnauthorized_whenUserNotFound() {
        LoginRequest req = new LoginRequest("ghost@test.com", "pass");

        given(userRepository.findByEmail("ghost@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void login_throwsForbidden_whenAccountIsInactive() {
        savedUser.setActive(false);
        LoginRequest req = new LoginRequest("jane@test.com", "password123");

        given(userRepository.findByEmail("jane@test.com")).willReturn(Optional.of(savedUser));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void login_throwsUnauthorized_whenPasswordDoesNotMatch() {
        LoginRequest req = new LoginRequest("jane@test.com", "wrongpass");

        given(userRepository.findByEmail("jane@test.com")).willReturn(Optional.of(savedUser));
        given(passwordEncoder.matches("wrongpass", "hashed_password")).willReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    // ── getProfile ───────────────────────────────────────────────────────────

    @Test
    void getProfile_returnsUser_whenFound() {
        given(userRepository.findById(1L)).willReturn(Optional.of(savedUser));

        User result = authService.getProfile(1L);

        assertThat(result.getEmail()).isEqualTo("jane@test.com");
    }

    @Test
    void getProfile_throwsRuntimeException_whenNotFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getProfile(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ── updateProfile ────────────────────────────────────────────────────────

    @Test
    void updateProfile_updatesOnlyProvidedFields() {
        UpdateProfileRequest req = new UpdateProfileRequest("New Name", null, null);

        given(userRepository.findById(1L)).willReturn(Optional.of(savedUser));
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        User result = authService.updateProfile(1L, req);

        assertThat(result.getFullName()).isEqualTo("New Name");
        assertThat(result.getEmail()).isEqualTo("jane@test.com"); // unchanged
        then(userRepository).should().save(savedUser);
    }

    @Test
    void updateProfile_doesNotOverwriteNullFields() {
        UpdateProfileRequest req = new UpdateProfileRequest(null, "9876543210", null); // fullName is null

        given(userRepository.findById(1L)).willReturn(Optional.of(savedUser));
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        User result = authService.updateProfile(1L, req);

        assertThat(result.getFullName()).isEqualTo("Jane Doe"); // unchanged
        assertThat(result.getPhone()).isEqualTo("9876543210");
    }

    // ── changePassword ───────────────────────────────────────────────────────

    @Test
    void changePassword_success_updatesHash() {
        given(userRepository.findById(1L)).willReturn(Optional.of(savedUser));
        given(passwordEncoder.matches("oldpass", "hashed_password")).willReturn(true);
        given(passwordEncoder.encode("newpass")).willReturn("new_hashed");

        authService.changePassword(1L, "oldpass", "newpass");

        assertThat(savedUser.getPasswordHash()).isEqualTo("new_hashed");
        then(userRepository).should().save(savedUser);
    }

    @Test
    void changePassword_throwsIllegalArgument_whenOldPasswordWrong() {
        given(userRepository.findById(1L)).willReturn(Optional.of(savedUser));
        given(passwordEncoder.matches("wrongold", "hashed_password")).willReturn(false);

        assertThatThrownBy(() -> authService.changePassword(1L, "wrongold", "newpass"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Incorrect current password");

        then(userRepository).should(never()).save(any());
    }

    // ── deactivateAccount ────────────────────────────────────────────────────

    @Test
    void deactivateAccount_setsIsActiveFalse() {
        given(userRepository.findById(1L)).willReturn(Optional.of(savedUser));
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        authService.deactivateAccount(1L);

        assertThat(savedUser.isActive()).isFalse();
        then(userRepository).should().save(savedUser);
    }

    // ── getUsersByRole ───────────────────────────────────────────────────────

    @Test
    void getUsersByRole_returnsListFromRepository() {
        given(userRepository.findAllByRole(User.Role.DRIVER)).willReturn(List.of(savedUser));

        List<User> result = authService.getUsersByRole(User.Role.DRIVER);

        assertThat(result).hasSize(1).contains(savedUser);
    }

    @Test
    void getUsersByRole_returnsEmptyList_whenNoUsersWithRole() {
        given(userRepository.findAllByRole(User.Role.ADMIN)).willReturn(List.of());

        assertThat(authService.getUsersByRole(User.Role.ADMIN)).isEmpty();
    }
}
