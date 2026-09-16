package com.devguard.organization;

import com.devguard.shared.error.ResourceNotFoundException;
import com.devguard.shared.tenant.Tenants;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class OrganizationService {

    private static final String ACTIVE = "ACTIVE";

    private final OrganizationRepository organizations;

    public OrganizationService(OrganizationRepository organizations) {
        this.organizations = organizations;
    }

    public Mono<Organization> getCurrent() {
        return Tenants.current()
                .flatMap(t -> organizations.findByIdAndStatus(t.organizationId(), ACTIVE)
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("Organización", t.organizationId()))));
    }
}