package com.devguard.identity.dto;

import com.devguard.identity.User;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String displayName,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt) {

    public static UserResponse from(User u) {
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getDisplayName(),
                u.getLastLoginAt(),
                u.getCreatedAt());
    }
}