package com.devguard.repository;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table("repositories")
public class Repository {

    @Id
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Column("project_id")
    private UUID projectId;

    private String provider;

    @Column("external_id")
    private String externalId;

    @Column("full_name")
    private String fullName;

    @Column("clone_url")
    private String cloneUrl;

    @Column("default_branch")
    private String defaultBranch;

    private String visibility;

    private boolean authorized;

    @Column("authorized_at")
    private OffsetDateTime authorizedAt;

    @Column("authorized_by")
    private UUID authorizedBy;

    @CreatedDate
    @Column("created_at")
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private OffsetDateTime updatedAt;

    public static Repository connect(UUID organizationId, UUID projectId, String provider,
                                     String fullName, String cloneUrl, String defaultBranch,
                                     String visibility, UUID authorizedBy) {
        Repository r = new Repository();
        r.organizationId = organizationId;
        r.projectId = projectId;
        r.provider = provider;
        r.fullName = fullName;
        r.cloneUrl = cloneUrl;
        r.defaultBranch = (defaultBranch == null || defaultBranch.isBlank())
                ? "main" : defaultBranch;
        r.visibility = visibility == null ? "PRIVATE" : visibility;
        r.authorized = true;
        r.authorizedAt = OffsetDateTime.now();
        r.authorizedBy = authorizedBy;
        return r;
    }

    public void revokeAuthorization() {
        this.authorized = false;
        // authorized_at se conserva: es el registro de cuando se autorizo, no
        // un indicador de si sigue vigente. La restriccion del esquema exige
        // que exista cuando authorized es true, no que desaparezca al revocar.
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getCloneUrl() { return cloneUrl; }
    public void setCloneUrl(String cloneUrl) { this.cloneUrl = cloneUrl; }

    public String getDefaultBranch() { return defaultBranch; }
    public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public boolean isAuthorized() { return authorized; }
    public void setAuthorized(boolean authorized) { this.authorized = authorized; }

    public OffsetDateTime getAuthorizedAt() { return authorizedAt; }
    public void setAuthorizedAt(OffsetDateTime authorizedAt) { this.authorizedAt = authorizedAt; }

    public UUID getAuthorizedBy() { return authorizedBy; }
    public void setAuthorizedBy(UUID authorizedBy) { this.authorizedBy = authorizedBy; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

}
