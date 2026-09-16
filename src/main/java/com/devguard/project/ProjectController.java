package com.devguard.project;

import com.devguard.project.dto.CreateProjectRequest;
import com.devguard.project.dto.ProjectResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService service;

    public ProjectController(ProjectService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER', 'SECURITY_ANALYST')")
    public Flux<ProjectResponse> list() { return service.list().map(ProjectResponse::from); }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER', 'SECURITY_ANALYST')")
    public Mono<ProjectResponse> get(@PathVariable UUID id) { return service.get(id).map(ProjectResponse::from); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request) {
        return service.create(request).map(ProjectResponse::from);
    }
}
