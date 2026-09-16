package com.devguard.organization;

import com.devguard.organization.dto.OrganizationResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService service;

    public OrganizationController(OrganizationService service) {
        this.service = service;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER', 'SECURITY_ANALYST')")
    public Mono<OrganizationResponse> getCurrent() {
        return service.getCurrent().map(OrganizationResponse::from);
    }
}