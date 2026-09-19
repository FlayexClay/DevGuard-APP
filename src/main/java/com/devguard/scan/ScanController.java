package com.devguard.scan;

import com.devguard.scan.dto.RequestScanRequest;
import com.devguard.scan.dto.ScanResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ScanController {

    private final ScanService service;

    public ScanController(ScanService service) { this.service = service; }

    @GetMapping("/projects/{projectId}/scans")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER', 'SECURITY_ANALYST')")
    public Flux<ScanResponse> list(@PathVariable UUID projectId) {
        return service.listByProject(projectId).map(ScanResponse::from);
    }

    @GetMapping("/scans/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER', 'SECURITY_ANALYST')")
    public Mono<ScanResponse> get(@PathVariable UUID id) {
        return service.get(id).map(ScanResponse::from);
    }

    /**
     * 202 y no 201: el recurso queda creado, pero el trabajo que representa no
     * ha ocurrido. El cliente debe consultar /api/scans/{id} para ver el
     * progreso.
     *
     * Un DEVELOPER puede pedir scans de lo que ya esta conectado; ampliar el
     * alcance conectando repositorios sigue siendo cosa de ADMIN.
     */
    @PostMapping("/projects/{projectId}/scans")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER', 'SECURITY_ANALYST')")
    public Mono<ScanResponse> request(
            @PathVariable UUID projectId,
            @Valid @RequestBody RequestScanRequest request) {
        return service.request(projectId, request).map(ScanResponse::from);
    }

}
