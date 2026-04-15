package com.auth.authservice.controller;

import com.auth.authservice.domain.entity.User;
import com.auth.authservice.dto.request.ChangePasswordRequest;
import com.auth.authservice.dto.request.UpdateUserRequest;
import com.auth.authservice.dto.response.UserResponse;
import com.auth.authservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.getMe(currentUser));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(@AuthenticationPrincipal User currentUser,
                                                  @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateMe(currentUser, request));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal User currentUser,
                                                @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(currentUser, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal User currentUser) {
        userService.deleteMe(currentUser);
        return ResponseEntity.noContent().build();
    }
}
