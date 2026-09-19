package com.devguard.scan.dto;

import com.devguard.scan.Scan;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ScanResponse(
        UUID id,
        UUID projectId,
        UUID repositoryId,
        String status,
        String triggerType,
        String branch,
        String commitSha,
        OffsetDateTime requestedAt,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        Long durationMs,
        String errorCode) {

    public static ScanResponse from(Scan s){
        return new ScanResponse(
                s.getId(),
                s.getProjectId(),
                s.getRepositoryId(),
                s.getStatus(),
                s.getTriggerType(),
                s.getBranch(),
                s.getCommitSha(),
                s.getRequestedAt(),
                s.getStartedAt(),
                s.getFinishedAt(),
                s.getDurationMs(),
                s.getErrorCode());
    }
}
