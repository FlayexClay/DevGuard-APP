package com.devguard.repository;

import com.devguard.project.ProjectRepository;
import com.devguard.repository.dto.ConnectRepositoryRequest;
import com.devguard.shared.audit.AuditAction;
import com.devguard.shared.audit.AuditService;
import com.devguard.shared.error.ConflicException;
import com.devguard.shared.error.ResourceNotFoundException;
import com.devguard.shared.tenant.Tenants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

@Service
public class RepositoryService {

    private static final Logger log = LoggerFactory.getLogger(RepositoryService.class);
    private static final String RESOURCE = "REPOSITORY";

    private static final Set<String> ALLOWED_SCHEMES = Set.of("https", "ssh");

    private final RepositoryRepository repositories;
    private final ProjectRepository projects;
    private final AuditService audit;

    public RepositoryService(RepositoryRepository repositories,
                             ProjectRepository projects,
                             AuditService audit) {
        this.repositories = repositories;
        this.projects = projects;
        this.audit = audit;
    }

    public Flux<Repository> listByProject(UUID projectId) {
        return Tenants.current()
                .flatMap(t -> projects.findByIdAndOrganizationId(projectId, t.organizationId())
                        .switchIfEmpty(Mono.error(
                                new ResourceNotFoundException("Proyecto", projectId)))
                        .thenReturn(t))
                .flatMapMany(t -> repositories
                        .findByProjectIdAndOrganizationId(projectId, t.organizationId()));
    }

    public Mono<Repository> connect(UUID projectId, ConnectRepositoryRequest request) {
        validateCloneUrl(request.cloneUrl());

        return Tenants.current().flatMap(tenant -> projects.findByIdAndOrganizationId(projectId, tenant.organizationId())
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("Proyecto", projectId)))
                .flatMap(project -> repositories
                        .existsByProjectIdAndProviderAndFullName(
                                projectId, request.provider(), request.fullName())
                        .flatMap(exists -> exists
                                ? Mono.<Repository>error(new ConflicException(
                                "El repositorio " + request.fullName()
                                        + " ya esta conectado a este proyecto"))
                                : repositories.save(Repository.connect(
                                tenant.organizationId(),
                                projectId,
                                request.provider(),
                                request.fullName(),
                                request.cloneUrl(),
                                request.defaultBranch(),
                                request.visibility(),
                                tenant.userId()))))
                .flatMap(saved -> audit.record(tenant, AuditAction.REPOSITORY_CONNECT,
                                RESOURCE, saved.getId().toString(),
                                AuditService.Outcome.SUCCESS,
                                "{\"fullName\":\"" + saved.getFullName()
                                        + "\",\"provider\":\"" + saved.getProvider() + "\"}")
                        .thenReturn(saved))
                .doOnSuccess(r -> log.info("Repositorio {} conectado al proyecto {} por {}",
                        r.getFullName(), projectId, tenant.subject())));
    }

    public Mono<Repository> revokeAuthorization(UUID repositoryId) {
        return Tenants.current().flatMap(tenant -> repositories
                .findByIdAndOrganizationId(repositoryId, tenant.organizationId())
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("Repositorio", repositoryId)))
                .flatMap(repo -> {
                    repo.revokeAuthorization();
                    return repositories.save(repo);
                })
                .flatMap(saved -> audit.record(tenant, AuditAction.REPOSITORY_REVOKE,
                        RESOURCE, saved.getId().toString(),
                        AuditService.Outcome.SUCCESS, null)
                        .thenReturn(saved)));
    }

    /**
     * Valida la URL antes de guardarla, no antes de clonar.
     *
     * Si la comprobacion viviera en el worker, una URL maliciosa quedaria
     * persistida esperando su turno, y bastaria con que alguien cambiara la
     * validacion del worker para activarla. Rechazarla en el alta es una
     * barrera; rechazarla al usarla es un filtro que se puede olvidar.
     */
    private static void validateCloneUrl(String cloneUrl) {
        String scheme;
        try {
            if (cloneUrl.startsWith("git@")) {
                scheme = "ssh";
            } else {
                URI uri = URI.create(cloneUrl);
                scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("La URL de clonado no es valido");
        }

        if (!ALLOWED_SCHEMES.contains(scheme)) {
            throw new IllegalArgumentException(
                    "La URL de clonado debe usar https o ssh"
            );
        }
    }

}
