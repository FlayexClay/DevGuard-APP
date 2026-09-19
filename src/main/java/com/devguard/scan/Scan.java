package com.devguard.scan;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Table("scans")
public class Scan {

    @Id
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Column("project_id")
    private UUID projectId;

    @Column("repository_id")
    private UUID repositoryId;

    private String status;

    @Column("trigger_type")
    private String triggerType;

    @Column("commit_sha")
    private String commitSha;

    private String branch;

    @Column("requested_by")
    private UUID requestedBy;

    @Column("requested_at")
    private OffsetDateTime requestedAt;

    @Column("started_at")
    private OffsetDateTime startedAt;

    @Column("finished_at")
    private OffsetDateTime finishedAt;

    @Column("duration_ms")
    private Long durationMs;

    @Column("error_code")
    private String errorCode;

    @Column("error_message")
    private String errorMessage;

    /* Clave de idempotencia */
    @Column("idempotency_key")
    private String idempotencyKey;

    @CreatedDate
    @Column("created_at")
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private OffsetDateTime updatedAt;

    public static Scan request(UUID organizationId, UUID projectId, UUID repositoryId,
                               String branch, UUID requestedBy, String triggerType,
                               String idempotencyKey) {
        Scan s = new Scan();
        s.organizationId = organizationId;
        s.projectId = projectId;
        s.repositoryId = repositoryId;
        s.branch = branch;
        s.requestedBy = requestedBy;
        s.triggerType = triggerType == null ? "MANUAL" : triggerType;
        s.status = ScanStatus.REQUESTED.name();
        s.requestedAt = OffsetDateTime.now();
        s.idempotencyKey = idempotencyKey;
        return s;
    }

    /** El scan quedo publicado en Kafka y espera a un worker. */
    public void markQueued() {
        this.status = ScanStatus.QUEUED.name();
    }

    public void markPublishFailed(String reason) {
        this.errorCode = "PUBLISH_FAILED";
        this.errorMessage = reason;
    }

    public void markFinished(OffsetDateTime at) {
        this.finishedAt = at;
        if (startedAt != null) {
            this.durationMs = Duration.between(startedAt, at).toMillis();
        }
    }

    public boolean isTerminal() {
        return ScanStatus.valueOf(status).isTerminal();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public UUID getRepositoryId() { return repositoryId; }
    public void setRepositoryId(UUID repositoryId) { this.repositoryId = repositoryId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }

    public String getCommitSha() { return commitSha; }
    public void setCommitSha(String commitSha) { this.commitSha = commitSha; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public UUID getRequestedBy() { return requestedBy; }
    public void setRequestedBy(UUID requestedBy) { this.requestedBy = requestedBy; }

    public OffsetDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(OffsetDateTime requestedAt) { this.requestedAt = requestedAt; }

    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }

    public OffsetDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(OffsetDateTime finishedAt) { this.finishedAt = finishedAt; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
