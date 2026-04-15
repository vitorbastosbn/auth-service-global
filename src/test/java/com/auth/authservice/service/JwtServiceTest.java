package com.auth.authservice.service;

import com.auth.authservice.config.JwtConfig;
import com.auth.authservice.domain.entity.User;
import com.auth.authservice.domain.enums.Role;
import com.auth.authservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JWT service tests")
class JwtServiceTest {

    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private JwtService jwtService;

    private User testUser;
    private static final String SECRET = "test-secret-key-for-testing-purposes-must-be-at-least-32chars";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .password("encodedPassword")
                .role(Role.USER)
                .active(true)
                .build();

        when(jwtConfig.getSecret()).thenReturn(SECRET);
        when(jwtConfig.getAccessTokenExpiration()).thenReturn(900L);
    }

    @Test
    @DisplayName("Should generate valid access token")
    void shouldGenerateValidAccessToken() {
        String token = jwtService.generateAccessToken(testUser);

        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("Should extract user ID from token")
    void shouldExtractUserIdFromToken() {
        String token = jwtService.generateAccessToken(testUser);
        UUID extractedId = jwtService.extractUserId(token);

        assertThat(extractedId).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Should extract email from token")
    void shouldExtractEmailFromToken() {
        String token = jwtService.generateAccessToken(testUser);
        String extractedEmail = jwtService.extractEmail(token);

        assertThat(extractedEmail).isEqualTo(testUser.getEmail());
    }

    @Test
    @DisplayName("Should extract role from token")
    void shouldExtractRoleFromToken() {
        String token = jwtService.generateAccessToken(testUser);
        String extractedRole = jwtService.extractRole(token);

        assertThat(extractedRole).isEqualTo("USER");
    }

    @Test
    @DisplayName("Should validate valid token")
    void shouldValidateValidToken() {
        String token = jwtService.generateAccessToken(testUser);

        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("Should reject invalid token")
    void shouldRejectInvalidToken() {
        assertThat(jwtService.isTokenValid("invalid.token.here")).isFalse();
    }

    @Test
    @DisplayName("Should generate refresh token as UUID")
    void shouldGenerateRefreshTokenAsUUID() {
        String refreshToken = jwtService.generateRefreshToken();

        assertThat(refreshToken).isNotNull();
        assertThatCode(() -> UUID.fromString(refreshToken)).doesNotThrowAnyException();
    }
}
