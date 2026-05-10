package com.parkease.auth.service;

import com.parkease.auth.entity.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    JwtService jwtService;
    User testUser;

    // Must be at least 32 bytes for HS256
    static final String SECRET = "testSecretKeyMustBeAtLeast32Chars!!";
    static final long EXPIRATION_MS = 86_400_000L; // 24 h

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", EXPIRATION_MS);

        testUser = User.builder()
                .userId(42L)
                .email("driver@test.com")
                .fullName("Test Driver")
                .role(User.Role.DRIVER)
                .build();
    }

    // ── generateToken ────────────────────────────────────────────────────────

    @Test
    void generateToken_returnsNonBlankToken() {
        String token = jwtService.generateToken(testUser);
        assertThat(token).isNotBlank();
    }

    @Test
    void generateToken_producesThreePartJwt() {
        String token = jwtService.generateToken(testUser);
        assertThat(token.split("\\.")).hasSize(3);
    }

    // ── validateAndExtract ───────────────────────────────────────────────────

    @Test
    void validateAndExtract_subjectIsUserId() {
        String token = jwtService.generateToken(testUser);
        Claims claims = jwtService.validateAndExtract(token);
        assertThat(claims.getSubject()).isEqualTo("42");
    }

    @Test
    void validateAndExtract_containsEmailClaim() {
        String token = jwtService.generateToken(testUser);
        Claims claims = jwtService.validateAndExtract(token);
        assertThat(claims.get("email", String.class)).isEqualTo("driver@test.com");
    }

    @Test
    void validateAndExtract_containsRoleClaim() {
        String token = jwtService.generateToken(testUser);
        Claims claims = jwtService.validateAndExtract(token);
        assertThat(claims.get("role", String.class)).isEqualTo("DRIVER");
    }

    @Test
    void validateAndExtract_containsFullNameClaim() {
        String token = jwtService.generateToken(testUser);
        Claims claims = jwtService.validateAndExtract(token);
        assertThat(claims.get("fullName", String.class)).isEqualTo("Test Driver");
    }

    @Test
    void validateAndExtract_throwsException_forGarbageInput() {
        assertThatThrownBy(() -> jwtService.validateAndExtract("not.a.real.token"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void validateAndExtract_throwsException_forTokenSignedWithWrongSecret() {
        // Token signed with a different secret
        JwtService other = new JwtService();
        ReflectionTestUtils.setField(other, "secret", "DifferentSecretKeyAtLeast32Chars!!");
        ReflectionTestUtils.setField(other, "expirationMs", EXPIRATION_MS);

        String foreignToken = other.generateToken(testUser);

        assertThatThrownBy(() -> jwtService.validateAndExtract(foreignToken))
                .isInstanceOf(Exception.class);
    }

    // ── isValid ──────────────────────────────────────────────────────────────

    @Test
    void isValid_returnsTrueForFreshToken() {
        String token = jwtService.generateToken(testUser);
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void isValid_returnsFalseForGarbageString() {
        assertThat(jwtService.isValid("garbage.token.value")).isFalse();
    }

    @Test
    void isValid_returnsFalseForExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L); // already expired
        String expiredToken = jwtService.generateToken(testUser);
        assertThat(jwtService.isValid(expiredToken)).isFalse();
    }

    @Test
    void isValid_returnsFalseForEmptyString() {
        assertThat(jwtService.isValid("")).isFalse();
    }

    @Test
    void tokenRoundTrip_managerRole() {
        User manager = User.builder().userId(5L).email("mgr@test.com")
                .fullName("Manager").role(User.Role.MANAGER).build();
        String token = jwtService.generateToken(manager);
        Claims claims = jwtService.validateAndExtract(token);

        assertThat(claims.get("role", String.class)).isEqualTo("MANAGER");
        assertThat(claims.getSubject()).isEqualTo("5");
    }
}
