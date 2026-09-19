package com.devguard.scan;

import com.devguard.project.ProjectRepository;
import com.devguard.repository.RepositoryRepository;
import com.devguard.scan.dto.RequestScanRequest;
import com.devguard.shared.audit.AuditAction;
import com.devguard.shared.audit.AuditService;
import com.devguard.shared.error.ConflicException;
import com.devguard.shared.error.ResourceNotFoundException;
import com.devguard.shared.event.DevGuardTopics;
import com.devguard.shared.event.EventPublisher;
import com.devguard.shared.event.ScanRequestedEvent;
import com.devguard.shared.tenant.TenantContext;
import com.devguard.shared.tenant.Tenants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class ScanService {

    private static final Logger log = LoggerFactory.getLogger(ScanService.class);

    private static final String RESOURCE = "SCAN";

    private final ScanRepository scans;
    private final ProjectRepository projects;
    private final RepositoryRepository repositories;
    private final EventPublisher events;
    private final AuditService audit;

    public ScanService(ScanRepository scans,
                       ProjectRepository projects,
                       RepositoryRepository repositories,
                       EventPublisher events,
                       AuditService audit) {
        this.scans = scans;
        this.projects = projects;
        this.repositories = repositories;
        this.events = events;
        this.audit = audit;
    }

    public Flux<Scan> listByProject(UUID projectId) {
        return Tenants.current()
                .flatMap(t -> projects.findByIdAndOrganizationId(projectId, t.organizationId())
                        .switchIfEmpty(Mono.error(
                                new ResourceNotFoundException("Proyecto", projectId)))
                        .thenReturn(t))
                .flatMapMany(t -> scans
                        .findByProjectIdAndOrganizationIdOrderByRequestedAtDesc(
                                projectId, t.organizationId()));
    }

    public Mono<Scan> get(UUID id) {
        return Tenants.current()
                .flatMap(t -> scans.findByIdAndOrganizationId(id, t.organizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Scan", id)));
    }
    /**
     * Guardar primero, publicar despues.
     *
     * El orden importa y no es intercambiable. PostgreSQL es la fuente de
     * verdad del estado del scan; Kafka solo transporta el aviso. Si se
     * publicara antes de guardar, un worker rapido podria buscar un scan que
     * todavia no existe.
     *
     * Y si la publicacion falla, el scan queda en REQUESTED con el error
     * anotado: es consultable, reconciliable y no se pierde. Lo contrario
     * (confiar el estado a Kafka) daria scans fantasma que el usuario no puede
     * ver ni reintentar.
     */
    public Mono<Scan> request(UUID projectId, RequestScanRequest request) {
        return Tenants.current().flatMap(tenant -> projects
                .findByIdAndOrganizationId(projectId, tenant.organizationId())
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("Proyecto", projectId)))
                .then(repositories.findByIdAndOrganizationIdAndAuthorizedIsTrue(
                        request.repositoryId(), tenant.organizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        "Repositorio autorizado", request.repositoryId())))
                .filter(repo -> projectId.equals(repo.getProjectId()))
                .switchIfEmpty(Mono.error(new ConflicException(
                        "El repositorio no pertenece a este proeycto")))
                .flatMap(repo -> scans.countActiveByRepository(repo.getId())
                        .defaultIfEmpty(0L)
                        .flatMap(activos -> activos > 0
                                ? Mono.error(new ConflicException(
                                "Ya hay un scan en curso para este repositorio"))
                                : persistAndPublish(tenant, projectId, repo.getId(),
                                branchOf(request, repo.getDefaultBranch())))));
    }

    private static String branchOf(RequestScanRequest request, String defaultBranch) {
        return (request.branch() == null || request.branch().isBlank())
                ? defaultBranch
                : request.branch();
    }

    private Mono<Scan> persistAndPublish(TenantContext tenant, UUID projectId,
                                         UUID repositoryId, String branch) {
        String idempotencyKey = UUID.randomUUID().toString();

        Scan scan = Scan.request(tenant.organizationId(), projectId, repositoryId,
                branch, tenant.userId(), "MANUAL", idempotencyKey);
        return scans.save(scan)
                .flatMap(saved -> audit.record(tenant, AuditAction.SCAN_REQUEST,
                                RESOURCE, saved.getId().toString(),
                                AuditService.Outcome.SUCCESS,
                                "{\"repositoryId\":\"" + repositoryId
                                        + "\",\"branch\":\"" + branch + "\"}")
                        .thenReturn(saved))
                .flatMap(saved -> publish(saved)
                        .then(Mono.defer(() -> {
                            saved.markQueued();
                            return scans.save(saved);
                        }))
                        // La publicacion fallo: el scan permanece en REQUESTED.
                        // Se devuelve al cliente con el error anotado en lugar
                        // de responder 500, porque el scan existe de verdad y
                        // se puede reintentar.
                        .onErrorResume(e -> {
                            log.error("No se pudo publicar {} para el scan {}: {}",
                                    DevGuardTopics.SCAN_REQUESTED, saved.getId(),
                                    e.getMessage());
                            saved.markPublishFailed(e.getMessage());
                            return scans.save(saved);
                        }))
                .doOnSuccess(s -> log.info("Scan {} en estado {} para el repositorio {}",
                        s.getId(), s.getStatus(), repositoryId));
    }

    private Mono<Void> publish(Scan scan) {
        ScanRequestedEvent event = new ScanRequestedEvent(
                scan.getId(),
                scan.getOrganizationId(),
                scan.getProjectId(),
                scan.getRepositoryId(),
                scan.getTriggerType(),
                scan.getBranch(),
                scan.getRequestedAt());
        // Clave de particion: la organizacion, para preservar el orden de los
        // eventos de un mismo tenant.
        return events.publish(DevGuardTopics.SCAN_REQUESTED,
                scan.getOrganizationId().toString(), event);
    }

}
