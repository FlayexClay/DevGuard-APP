package com.devguard.project.dto;

import com.devguard.project.Project;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String slug,
        String description,
        String status,
        OffsetDateTime createdAt) {

    public static ProjectResponse from(Project p) {
        return new ProjectResponse(
                p.getId(),
                p.getName(),
                p.getSlug(),
                p.getDescription(),
                p.getStatus(),
                p.getCreatedAt());
    }
}
