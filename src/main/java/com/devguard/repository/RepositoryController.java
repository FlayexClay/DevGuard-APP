package com.devguard.repository;

import com.devguard.repository.dto.ConnectRepositoryRequest;
import com.devguard.repository.dto.RepositoryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class RepositoryController {

    private final RepositoryService service;

    public RepositoryController(RepositoryService service) {
        this.service = service;
    }

    @GetMapping("/projects/{projectId}/repositories")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER', 'SECURITY_ANALYST')")
    public Flux<RepositoryResponse> list(@PathVariable UUID projectId) {
        return service.listByProject(projectId).map(RepositoryResponse::from);
    }

    /**
     * Conectar un repositorio es declarar autorizacion para escanearlo, asi que
     * queda restringido a ADMIN. Un DEVELOPER puede pedir scans de lo que ya
     * esta conectado, pero no ampliar el alcance.
     */
    @PostMapping("/projects/{projectId}/repositories")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<RepositoryResponse> connect(
            @PathVariable UUID projectId,
            @Valid @RequestBody ConnectRepositoryRequest request) {
        return service.connect(projectId, request).map(RepositoryResponse::from);
    }

    @DeleteMapping("/repositories/{id}/authorization")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<RepositoryResponse> revoke(@PathVariable UUID id) {
        return service.revokeAuthorization(id).map(RepositoryResponse::from);
    }

}
