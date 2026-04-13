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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final JwtConfig jwtConfig;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final LoginAttemptService loginAttemptService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User already exists with email: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .active(true)
                .build();

        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail();

        if (loginAttemptService.isBlocked(email)) {
            throw new InvalidCredentialsException("Account temporarily blocked due to too many failed attempts");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    loginAttemptService.loginFailed(email);
                    return new InvalidCredentialsException("Credenciais inválidas");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.loginFailed(email);
            throw new InvalidCredentialsException("Credenciais inválidas");
        }

        if (!user.isActive()) {
            throw new UserInactiveException("User account is inactive");
        }

        loginAttemptService.loginSucceeded(email);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        if (refreshToken.isExpired()) {
            throw new TokenExpiredException("Refresh token has expired");
        }

        User user = refreshToken.getUser();

        if (!user.isActive()) {
            throw new UserInactiveException("User account is inactive");
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenRepository.findByToken(request.getRefreshToken())
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    public TokenValidationResponse validate(ValidateTokenRequest request) {
        String token = request.getToken();

        if (!jwtService.isTokenValid(token)) {
            if (jwtService.isTokenExpired(token)) {
                throw new TokenExpiredException("Access token has expired");
            }
            throw new InvalidTokenException("Invalid access token");
        }

        return TokenValidationResponse.builder()
                .valid(true)
                .userId(jwtService.extractUserId(token))
                .email(jwtService.extractEmail(token))
                .role(jwtService.extractRole(token))
                .build();
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenValue = jwtService.generateRefreshToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .user(user)
                .expiresAt(OffsetDateTime.now().plusSeconds(jwtConfig.getRefreshTokenExpiration()))
                .build();

        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(jwtConfig.getAccessTokenExpiration())
                .user(userMapper.toResponse(user))
                .build();
    }
}
