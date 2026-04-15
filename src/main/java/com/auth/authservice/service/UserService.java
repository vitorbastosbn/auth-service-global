package com.auth.authservice.service;

import com.auth.authservice.domain.entity.User;
import com.auth.authservice.dto.request.ChangePasswordRequest;
import com.auth.authservice.dto.request.UpdateRoleRequest;
import com.auth.authservice.dto.request.UpdateStatusRequest;
import com.auth.authservice.dto.request.UpdateUserRequest;
import com.auth.authservice.dto.response.UserResponse;
import com.auth.authservice.exception.InvalidCredentialsException;
import com.auth.authservice.exception.UserAlreadyExistsException;
import com.auth.authservice.exception.UserNotFoundException;
import com.auth.authservice.mapper.UserMapper;
import com.auth.authservice.repository.RefreshTokenRepository;
import com.auth.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserResponse getMe(User currentUser) {
        return userMapper.toResponse(currentUser);
    }

    @Transactional
    public UserResponse updateMe(User currentUser, UpdateUserRequest request) {
        if (StringUtils.hasText(request.getName())) {
            currentUser.setName(request.getName());
        }

        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equals(currentUser.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new UserAlreadyExistsException("Email already in use: " + request.getEmail());
            }
            currentUser.setEmail(request.getEmail());
        }

        return userMapper.toResponse(userRepository.save(currentUser));
    }

    @Transactional
    public void changePassword(User currentUser, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);
        refreshTokenRepository.revokeAllByUser(currentUser);
    }

    @Transactional
    public void deleteMe(User currentUser) {
        refreshTokenRepository.deleteAllByUser(currentUser);
        userRepository.delete(currentUser);
    }

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Transactional
    public UserResponse updateRole(UUID userId, UpdateRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        user.setRole(request.getRole());
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateStatus(UUID userId, UpdateStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        user.setActive(request.getActive());
        return userMapper.toResponse(userRepository.save(user));
    }
}
