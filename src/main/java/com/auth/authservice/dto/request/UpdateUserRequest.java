package com.auth.authservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Size(max = 120, message = "Name must be at most 120 characters")
    private String name;

    @Email(message = "Email must be valid")
    private String email;
}
