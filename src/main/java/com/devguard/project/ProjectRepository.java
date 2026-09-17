package com.devguard.project;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ProjectRepository extends Repository<Project, UUID> {

    Mono<Project> save(Project project);
    Mono<Project> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Flux<Project> findByOrganizationIdAndStatusOrderByCreatedAtDesc(UUID organizationId, String status);
    Mono<Boolean> existsByOrganizationIdAndSlug(UUID organizationId, String slug);
    Mono<Long> countByOrganizationIdAndStatus(UUID organizationId, String status);
    @Query("SELECT EXISTS(SELECT 1 FROM projects WHERE id = :id)")
    Mono<Boolean> existsAnywhereForAuditOnly(UUID id);
}
