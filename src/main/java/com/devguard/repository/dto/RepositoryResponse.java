package com.devguard.repository.dto;

import com.devguard.repository.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RepositoryResponse(
        UUID id,
        UUID projectId,
        String provider,
        String fullName,
        String cloneUrl,
        String defaultBranch,
        String visibility,
        boolean authorized,
        OffsetDateTime authorizedAt,
        OffsetDateTime createdAt) {

    public static RepositoryResponse from(Repository r) {
        return new RepositoryResponse(
                r.getId(),
                r.getProjectId(),
                r.getProvider(),
                r.getFullName(),
                r.getCloneUrl(),
                r.getDefaultBranch(),
                r.getVisibility(),
                r.isAuthorized(),
                r.getAuthorizedAt(),
                r.getCreatedAt());
    }
}
