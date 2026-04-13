package com.auth.authservice.service;

import com.auth.authservice.config.JwtConfig;
import com.auth.authservice.domain.entity.RefreshToken;
import com.auth.authservice.domain.entity.User;
import com.auth.authservice.domain.enums.Role;
import com.auth.authservice.dto.request.*;
import com.auth.authservice.dto.response.AuthResponse;
import com.auth.authservice.dto.response.TokenValidationResponse;
import com.auth.authservice.exception.*;
import com.auth.authservice.mapper.UserMapper;
import com.auth.authservice.repository.RefreshTokenRepository;
import com.auth.authservice.repository.UserRepository;
import com.auth.authservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do serviço de autenticação")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtService jwtService;
    @Mock private JwtConfig jwtConfig;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserMapper userMapper;
    @Mock private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthService authService;

    private User testUser;

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
    }

    @Test
    @DisplayName("Deve registrar usuário com sucesso")
    void shouldRegisterUserSuccessfully() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("Test@123");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateAccessToken(any())).thenReturn("accessToken");
        when(jwtService.generateRefreshToken()).thenReturn("refreshToken");
        when(jwtConfig.getAccessTokenExpiration()).thenReturn(900L);
        when(jwtConfig.getRefreshTokenExpiration()).thenReturn(604800L);
        when(refreshTokenRepository.save(any())).thenReturn(mock(RefreshToken.class));
        when(userMapper.toResponse(any())).thenReturn(null);

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("accessToken");
        assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao registrar email já existente")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("Test@123");

        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    @DisplayName("Deve fazer login com sucesso")
    void shouldLoginSuccessfully() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("Test@123");

        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtService.generateAccessToken(any())).thenReturn("accessToken");
        when(jwtService.generateRefreshToken()).thenReturn("refreshToken");
        when(jwtConfig.getAccessTokenExpiration()).thenReturn(900L);
        when(jwtConfig.getRefreshTokenExpiration()).thenReturn(604800L);
        when(refreshTokenRepository.save(any())).thenReturn(mock(RefreshToken.class));
        when(userMapper.toResponse(any())).thenReturn(null);

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        verify(loginAttemptService).loginSucceeded(request.getEmail());
    }

    @Test
    @DisplayName("Deve lançar exceção com credenciais inválidas")
    void shouldThrowExceptionWithInvalidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrong");

        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(loginAttemptService).loginFailed(request.getEmail());
    }

    @Test
    @DisplayName("Deve lançar exceção quando conta está bloqueada")
    void shouldThrowExceptionWhenAccountIsBlocked() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("Test@123");

        when(loginAttemptService.isBlocked(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("blocked");
    }

    @Test
    @DisplayName("Deve renovar token com sucesso")
    void shouldRefreshTokenSuccessfully() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("validRefreshToken");

        RefreshToken refreshToken = RefreshToken.builder()
                .token("validRefreshToken")
                .user(testUser)
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.of(refreshToken));
        when(refreshTokenRepository.save(any())).thenReturn(refreshToken);
        when(jwtService.generateAccessToken(any())).thenReturn("newAccessToken");
        when(jwtService.generateRefreshToken()).thenReturn("newRefreshToken");
        when(jwtConfig.getAccessTokenExpiration()).thenReturn(900L);
        when(jwtConfig.getRefreshTokenExpiration()).thenReturn(604800L);
        when(userMapper.toResponse(any())).thenReturn(null);

        AuthResponse response = authService.refresh(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("newAccessToken");
    }

    @Test
    @DisplayName("Deve lançar exceção com token de refresh inválido")
    void shouldThrowExceptionWithInvalidRefreshToken() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalidToken");

        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("Deve validar token de acesso com sucesso")
    void shouldValidateAccessTokenSuccessfully() {
        ValidateTokenRequest request = new ValidateTokenRequest();
        request.setToken("validToken");

        UUID userId = UUID.randomUUID();
        when(jwtService.isTokenValid(anyString())).thenReturn(true);
        when(jwtService.extractUserId(anyString())).thenReturn(userId);
        when(jwtService.extractEmail(anyString())).thenReturn("test@example.com");
        when(jwtService.extractRole(anyString())).thenReturn("USER");

        TokenValidationResponse response = authService.validate(request);

        assertThat(response.isValid()).isTrue();
        assertThat(response.getEmail()).isEqualTo("test@example.com");
    }
}
