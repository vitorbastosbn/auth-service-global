package com.auth.authservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TokenValidationResponse {
    private boolean valid;
    private UUID userId;
    private String email;
    private String role;
}
