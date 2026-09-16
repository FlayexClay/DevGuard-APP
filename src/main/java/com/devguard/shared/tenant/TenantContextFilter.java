package com.devguard.shared.tenant;

import com.devguard.organization.OrganizationRepository;
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
    private final Map<String, UUID> slugToId = new ConcurrentHashMap<>();

    public TenantContextFilter(OrganizationRepository organizations) {
        this.organizations = organizations;
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

        if (slug == null || slug.isBlank()) {
            log.warn("Token sin claim {} para subject {}", ORG_CLAIM, jwt.getSubject());
            return Mono.error(new TenantResolutionException(
                    "El token no incluye organizacion"));
        }

        Set<String> roles = token.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .collect(Collectors.toUnmodifiableSet());

        UUID cached = slugToId.get(slug);
        if (cached != null) {
            return Mono.just(toContext(jwt, slug, cached, roles));
        }

        return organizations.findBySlugAndStatus(slug, "ACTIVE")
                .switchIfEmpty(Mono.error(new TenantResolutionException(
                        "Organizacion no encontrada o inactiva: " + slug)))
                .map(org -> {
                    slugToId.put(slug, org.getId());
                    return toContext(jwt, slug, org.getId(), roles);
                });
    }

    private Mono<Void> deny(ServerWebExchange exchange, TenantResolutionException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }

    private static TenantContext toContext(Jwt jwt, String slug, UUID id, Set<String> roles) {
        return new TenantContext(
                id,
                slug,
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                roles
        );
    }

}