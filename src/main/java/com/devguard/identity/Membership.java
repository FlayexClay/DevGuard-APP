package com.devguard.identity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table("memberships")
public class Membership {

    @Id
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Column("user_id")
    private UUID userId;

    private String role;

    @CreatedDate
    @Column("created_at")
    private OffsetDateTime createdAt;

    public static Membership of(UUID organizationId, UUID userId, String role) {
        Membership m = new Membership();
        m.organizationId = organizationId;
        m.userId = userId;
        m.role = role;
        return m;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
