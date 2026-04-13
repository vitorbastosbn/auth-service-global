package com.auth.authservice.mapper;

import com.auth.authservice.domain.entity.User;
import com.auth.authservice.dto.response.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
}
