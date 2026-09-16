package com.devguard.organization;

import org.springframework.data.repository.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface OrganizationRepository extends Repository<Organization, UUID> {

    Mono<Organization> findBySlugAndStatus(String slug, String status);
    Mono<Organization> findByIdAndStatus(UUID id, String status);
}
