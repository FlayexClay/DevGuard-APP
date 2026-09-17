package com.devguard.shared.tenant;

import com.devguard.identity.UserProvisioningService;
import com.devguard.organization.OrganizationRepository;
import com.devguard.shared.audit.AuditService;
import com.devguard.shared.error.TenantResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class TenantContextFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantContextFilter.class);
    private static final String ORG_CLAIM = "organization_id";

    private final OrganizationRepository organizations;
    private final UserProvisioningService userProvisioning;
    private final AuditService audit;
    private final Map<String, UUID> slugToId = new ConcurrentHashMap<>();

    public TenantContextFilter(OrganizationRepository organizations,
                               UserProvisioningService userProvisioning,
                               AuditService audit) {
        this.organizations = organizations;
        this.userProvisioning = userProvisioning;
        this.audit = audit;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(sc -> sc.getAuthentication())
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .flatMap(token -> buildContext(token)
                        .flatMap(tenant -> chain.filter(exchange)
                                .contextWrite(ctx -> Tenants.write(ctx, tenant))))
                .switchIfEmpty(Mono.defer(() -> chain.filter(exchange)))
                .onErrorResume(TenantResolutionException.class, ex -> deny(exchange, ex));
    }


    private Mono<TenantContext> buildContext(JwtAuthenticationToken token) {
        Jwt jwt = token.getToken();
        String slug = jwt.getClaimAsString(ORG_CLAIM);
        String subject = jwt.getSubject();

        Set<String> roles = token.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .collect(Collectors.toUnmodifiableSet());

        if (slug == null || slug.isBlank()) {
            // El token es valido pero el usuario no tiene tenant asignado.
            // Sin organizacion no hay nada que pueda consultar.
            log.warn("Token sin claim {} para subject {}", ORG_CLAIM, subject);
            return audit.recordTenantFailure(subject, null, "token sin organization_id")
                    .then(Mono.error(new TenantResolutionException(
                            "El token no incluye organizacion")));
        }

        UUID cachedOrg = slugToId.get(slug);
        Mono<UUID> orgId = cachedOrg != null
                ? Mono.just(cachedOrg)
                : organizations.findBySlugAndStatus(slug, "ACTIVE")
                .switchIfEmpty(Mono.defer(() -> audit
                        .recordTenantFailure(subject, slug,
                                "organizacion no encontrada o inactiva")
                        .then(Mono.error(new TenantResolutionException(
                                "Organizacion no encontrada o inactiva: " + slug)))))
                .map(org -> {
                    slugToId.put(slug, org.getId());
                    return org.getId();
                });

        return orgId.flatMap(id -> userProvisioning
                .resolver(subject, jwt.getClaimAsString("email"),
                        jwt.getClaimAsString("name"), id, roles)
                .map(userId -> new TenantContext(
                        id, slug, userId, subject, jwt.getClaimAsString("email"), roles)));
    }

    private Mono<Void> deny(ServerWebExchange exchange, TenantResolutionException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }

}