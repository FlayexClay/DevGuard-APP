package com.devguard.repository;


import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RepositoryRepository extends org.springframework.data.repository.Repository<Repository, UUID> {

    Mono<Repository> save(Repository repository);
    Mono<Repository> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Flux<Repository> findByProjectIdAndOrganizationId(UUID id, UUID organizationId);
    Mono<Boolean> existsByProjectIdAndProviderAndFullName(UUID projectId, String provider, String fullName);
    Mono<Repository> findByIdAndOrganizationIdAndAuthorizedIsTrue(UUID id, UUID organizationId);
    Mono<Long> countByProjectIdAndOrganizationId(UUID projectId, UUID organizationId);
}
