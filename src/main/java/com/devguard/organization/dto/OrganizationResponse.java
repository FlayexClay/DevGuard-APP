package com.devguard.organization.dto;

import com.devguard.organization.Organization;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String slug,
        String name,
        String plan,
        String status,
        Integer maxProjects,
        Integer maxUsers,
        OffsetDateTime createdAt) {

    public static OrganizationResponse from(Organization o) {
        return new OrganizationResponse(
                o.getId(),
                o.getSlug(),
                o.getName(),
                o.getPlan(),
                o.getStatus(),
                o.getMaxProjects(),
                o.getMaxUsers(),
                o.getCreatedAt());
    }
}