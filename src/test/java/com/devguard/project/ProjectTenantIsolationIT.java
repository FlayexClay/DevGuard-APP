package com.devguard.project;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@SpringBootTest
@AutoConfigureWebTestClient
@Testcontainers
class ProjectTenantIsolationIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    WebTestClient client;

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
                "r2dbc:postgresql://" + POSTGRES.getHost() + ":"
                + POSTGRES.getMappedPort(5432) + "/" + POSTGRES.getDatabaseName());
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);
        registry.add("spring.r2dbc.pool.initial-size", () -> "2");
        registry.add("spring.r2dbc.pool.max-size", () -> "5");
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
        // Keycloak ficticio — mockJwt() nunca llama al decoder
        registry.add("devguard.keycloak.internal-url", () -> "http://localhost:9999");
        registry.add("devguard.keycloak.realm", () -> "devguard");
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> "http://localhost:9999/realms/devguard");
    }

    // -----------------------------------------------------------------------
    // Aislamiento multi-tenant
    // -----------------------------------------------------------------------

    @Test
    void owner_lista_solo_sus_proyectos() {
        client.mutateWith(acmeJwt())
                .get().uri("/api/projects")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Map.class)
                .value(list -> assertThat(list)
                        .hasSize(1)
                        .first()
                        .extracting(m -> m.get("slug"))
                        .isEqualTo("checkout-service"));
    }

    @Test
    void intruder_no_puede_ver_proyecto_ajeno() {
        String projectId = obtenerPrimerProyectoDe("acme");

        client.mutateWith(globexJwt())
                .get().uri("/api/projects/" + projectId)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void cada_tenant_ve_solo_sus_propios_proyectos() {
        client.mutateWith(globexJwt())
                .get().uri("/api/projects")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Map.class)
                .value(list -> assertThat(list)
                        .hasSize(1)
                        .first()
                        .extracting(m -> m.get("slug"))
                        .isEqualTo("legacy-portal"));
    }

    // -----------------------------------------------------------------------
    // Autenticación
    // -----------------------------------------------------------------------

    @Test
    void sin_token_recibe_401() {
        client.get().uri("/api/projects")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String obtenerPrimerProyectoDe(String orgSlug) {
        var jwt = orgSlug.equals("acme") ? acmeJwt() : globexJwt();
        var body = client.mutateWith(jwt)
                .get().uri("/api/projects")
                .exchange()
                .expectBodyList(new ParameterizedTypeReference<Map<String, Object>>() {})
                .returnResult()
                .getResponseBody();

        assertThat(body).isNotEmpty();
        return (String) body.get(0).get("id");
    }

    private static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.JwtMutator acmeJwt() {
        return mockJwt().jwt(j -> j
                .subject("owner-001")
                .claim("organization_id", "acme")
                .claim("realm_access", Map.of("roles", List.of("DEVELOPER"))));
    }

    private static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.JwtMutator globexJwt() {
        return mockJwt().jwt(j -> j
                .subject("intruder-001")
                .claim("organization_id", "globex")
                .claim("realm_access", Map.of("roles", List.of("DEVELOPER"))));
    }
}