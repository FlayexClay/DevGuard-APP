package com.devguard.identity.dto;

import com.devguard.identity.Membership;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MembershipResponse(
        UUID id,
        UUID organizationId,
        String role,
        OffsetDateTime createdAt) {

    public static MembershipResponse from(Membership m) {
        return new MembershipResponse(
                m.getId(),
                m.getOrganizationId(),
                m.getRole(),
                m.getCreatedAt());
    }
}