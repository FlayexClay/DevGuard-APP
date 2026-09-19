package com.devguard.scan;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ScanRepository extends Repository<Scan, UUID> {

    Mono<Scan> save(Scan scan);
    Mono<Scan> findByIdAndOrganizationId(UUID id, UUID organizationId);d
    Flux<Scan> findByProjectIdAndOrganizationIdOrderByRequestedAtDesc(UUID projectId, UUID organizationId);
    Mono<Scan> findByIdempotencyKey(String idempotencyKey);
    @Query("""
            SELECT count(*) FROM scans
            WHERE repository_id = :repositoryId
              AND status IN ('REQUESTED', 'QUEUED', 'RUNNING')
            """)
    Mono<Long> countActiveByRepository(UUID repositoryId);
}
