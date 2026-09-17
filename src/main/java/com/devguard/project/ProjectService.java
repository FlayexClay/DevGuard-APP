package com.devguard.project;

import com.devguard.organization.OrganizationRepository;
import com.devguard.project.dto.CreateProjectRequest;
import com.devguard.shared.audit.AuditAction;
import com.devguard.shared.audit.AuditService;
import com.devguard.shared.error.ConflicException;
import com.devguard.shared.error.QuotaExceededException;
import com.devguard.shared.error.ResourceNotFoundException;
import com.devguard.shared.error.TenantResolutionException;
import com.devguard.shared.tenant.TenantContext;
import com.devguard.shared.tenant.Tenants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);
    private static final String ACTIVE = "ACTIVE";
    private static final String RESOURCE = "PROJECT";

    private final ProjectRepository projects;
    private final OrganizationRepository organizations;
    private final AuditService audit;

    public ProjectService(ProjectRepository projects, OrganizationRepository organizations, AuditService audit) {
        this.projects = projects;
        this.organizations = organizations;
        this.audit = audit;
    }

    public Flux<Project> list() {
        return Tenants.current()
                .flatMapMany(t -> projects
                        .findByOrganizationIdAndStatusOrderByCreatedAtDesc(
                                t.organizationId(), ACTIVE));
    }

    public Mono<Project> get(UUID id) {
        return Tenants.current()
                .flatMap(t -> projects.findByIdAndOrganizationId(id, t.organizationId())
                        .switchIfEmpty(Mono.defer(() -> auditMiss(t, id)
                                .then(Mono.error(new ResourceNotFoundException("Proyecto", id))))));
    }

    private Mono<Void> auditMiss(TenantContext tenant, UUID id) {
        return projects.existsAnywhereForAuditOnly(id)
                .defaultIfEmpty(false)
                .flatMap(existsElsewhere -> {
                    if (Boolean.TRUE.equals(existsElsewhere)) {
                        log.warn("Intento de acceso cruzado: {} pidio el proyecto {} "
                                        + "desde la organizacion {}",
                                tenant.subject(), id, tenant.organizationSlug());
                        return audit.record(tenant, AuditAction.CROSS_TENANT_ACCESS_ATTEMPT,
                                RESOURCE, id.toString(), AuditService.Outcome.DENIED, null);
                    }
                    return audit.record(tenant, AuditAction.PROJECT_READ,
                            RESOURCE, id.toString(), AuditService.Outcome.FAILURE,
                            "{\"reason\":\"no existe\"}");
                });
    }

    public Mono<Project> create(CreateProjectRequest request) {
        return Tenants.current().flatMap(tenant -> {
            String slug = SlugGenerator.from(request.name());

            return enforceQuota(tenant)
                    .then(projects.existsByOrganizationIdAndSlug(tenant.organizationId(), slug))
                    .flatMap(exists -> exists
                            ? Mono.<Project>error(new ConflicException(
                            "Ya existe un proyecto con el identificador " + slug))
                            : projects.save(Project.create(
                            tenant.organizationId(),
                            request.name().trim(),
                            slug,
                            request.description(),
                            tenant.userId())))
                    .flatMap(saved -> audit.record(tenant, AuditAction.PROJECT_CREATE,
                                    RESOURCE, saved.getId().toString(),
                                    AuditService.Outcome.SUCCESS,
                                    "{\"slug\":\"" + saved.getSlug() + "\"}")
                            .thenReturn(saved))
                    .doOnSuccess(p -> log.info("Proyecto {} creado en organizacion {} por {}",
                            p.getSlug(), tenant.organizationSlug(), tenant.subject()));
        });
    }
    private Mono<Void> enforceQuota(TenantContext tenant) {
        return organizations.findByIdAndStatus(tenant.organizationId(), ACTIVE)
                .switchIfEmpty(Mono.error(new TenantResolutionException(
                        "Organizacion inactiva: " + tenant.organizationSlug())))
                .flatMap(org -> projects
                        .countByOrganizationIdAndStatus(tenant.organizationId(), ACTIVE)
                        .flatMap(count -> count >= org.getMaxProjects()
                            ? Mono.error(new QuotaExceededException(
                                    "El plan " + org.getPlan() + " permite hasta "
                                    + org.getMaxProjects() + " proyectos"))
                            : Mono.empty()))
                .then();
    }

}
