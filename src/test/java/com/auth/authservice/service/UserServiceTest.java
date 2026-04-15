package com.auth.authservice.service;

import com.auth.authservice.domain.entity.User;
import com.auth.authservice.domain.enums.Role;
import com.auth.authservice.dto.request.ChangePasswordRequest;
import com.auth.authservice.dto.request.UpdateUserRequest;
import com.auth.authservice.dto.response.UserResponse;
import com.auth.authservice.exception.InvalidCredentialsException;
import com.auth.authservice.exception.UserAlreadyExistsException;
import com.auth.authservice.mapper.UserMapper;
import com.auth.authservice.repository.RefreshTokenRepository;
import com.auth.authservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("User service tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserResponse testUserResponse;

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

        testUserResponse = UserResponse.builder()
                .id(testUser.getId())
                .name(testUser.getName())
                .email(testUser.getEmail())
                .role(testUser.getRole())
                .active(testUser.isActive())
                .build();
    }

    @Test
    @DisplayName("Should return current user data")
    void shouldReturnCurrentUserData() {
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        UserResponse response = userService.getMe(testUser);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo(testUser.getEmail());
    }

    @Test
    @DisplayName("Should update user name successfully")
    void shouldUpdateUserNameSuccessfully() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("Updated Name");

        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(any())).thenReturn(testUserResponse);

        UserResponse response = userService.updateMe(testUser, request);

        assertThat(response).isNotNull();
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should throw exception when updating to existing email")
    void shouldThrowExceptionWhenUpdatingToExistingEmail() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("other@example.com");

        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> userService.updateMe(testUser, request))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    @DisplayName("Should change password successfully")
    void shouldChangePasswordSuccessfully() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("NewPass@123");

        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("newEncodedPassword");
        when(userRepository.save(any())).thenReturn(testUser);

        assertThatCode(() -> userService.changePassword(testUser, request))
                .doesNotThrowAnyException();
        verify(refreshTokenRepository).revokeAllByUser(testUser);
    }

    @Test
    @DisplayName("Should throw exception with wrong current password")
    void shouldThrowExceptionWithWrongCurrentPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongPassword");
        request.setNewPassword("NewPass@123");

        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(testUser, request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Should delete user successfully")
    void shouldDeleteUserSuccessfully() {
        assertThatCode(() -> userService.deleteMe(testUser))
                .doesNotThrowAnyException();
        verify(refreshTokenRepository).deleteAllByUser(testUser);
        verify(userRepository).delete(testUser);
    }
}
