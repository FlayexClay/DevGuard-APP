package com.devguard.identity;

import org.springframework.data.repository.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface MembershipRepository extends Repository<Membership, UUID> {
    Mono<Membership> save(Membership membership);
    Mono<Membership> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);
}
