package com.devguard.project;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

// RANDOM_PORT: servidor Netty real. Evita el problema de propagacion de
// Reactor context entre JwtMutator y TenantContextFilter que ocurre en MOCK.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
class ProjectTenantIsolationIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    // Reemplaza el NimbusReactiveJwtDecoder real. El flujo completo de
    // seguridad corre (AuthenticationWebFilter → jwtAuthenticationConverter
    // → TenantContextFilter), pero sin necesitar Keycloak en marcha.
    @MockitoBean
    ReactiveJwtDecoder jwtDecoder;

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
        registry.add("devguard.keycloak.internal-url", () -> "http://localhost:9999");
        registry.add("devguard.keycloak.realm", () -> "devguard");
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> "http://localhost:9999/realms/devguard");
    }

    // -----------------------------------------------------------------------
    // Autenticacion y resolucion de tenant
    // -----------------------------------------------------------------------

    @Test
    void sin_token_recibe_401() {
        client.get().uri("/api/projects")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void token_sin_claim_organization_id_recibe_403() {
        client(null, "sin-org", "DEVELOPER")
                .get().uri("/api/projects")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void token_con_organizacion_inexistente_recibe_403() {
        client("ghost-corp", "ghost-user", "DEVELOPER")
                .get().uri("/api/projects")
                .exchange()
                .expectStatus().isForbidden();
    }

    // -----------------------------------------------------------------------
    // Aislamiento multi-tenant (lectura)
    // -----------------------------------------------------------------------

    @Test
    void acme_ve_solo_su_proyecto() {
        client("acme", "owner", "DEVELOPER")
                .get().uri("/api/projects")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(new ParameterizedTypeReference<Map<String, Object>>() {})
                .value(list -> assertThat(list)
                        .extracting(m -> m.get("slug"))
                        .contains("checkout-service")
                        .doesNotContain("legacy-portal"));
    }

    @Test
    void globex_ve_solo_su_proyecto() {
        client("globex", "vecino", "DEVELOPER")
                .get().uri("/api/projects")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(new ParameterizedTypeReference<Map<String, Object>>() {})
                .value(list -> assertThat(list)
                        .extracting(m -> m.get("slug"))
                        .contains("legacy-portal")
                        .doesNotContain("checkout-service"));
    }

    @Test
    void intruder_no_puede_ver_proyecto_ajeno_recibe_404_no_403() {
        String projectId = resolveId("acme", "checkout-service");

        client("globex", "intruder", "ADMIN", "DEVELOPER")
                .get().uri("/api/projects/" + projectId)
                .exchange()
                .expectStatus().isNotFound();
    }

    // -----------------------------------------------------------------------
    // Control de acceso por rol
    // -----------------------------------------------------------------------

    @Test
    void security_analyst_puede_listar_proyectos() {
        client("acme", "analyst", "SECURITY_ANALYST")
                .get().uri("/api/projects")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void security_analyst_no_puede_crear_proyecto() {
        client("acme", "analyst", "SECURITY_ANALYST")
                .post().uri("/api/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Nuevo Proyecto"))
                .exchange()
                .expectStatus().isForbidden();
    }

    // -----------------------------------------------------------------------
    // Creacion de proyectos
    // -----------------------------------------------------------------------

    @Test
    void proyecto_creado_pertenece_al_tenant_del_token() {
        client("acme", "owner", "ADMIN")
                .post().uri("/api/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Servicio de Pagos"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.slug").isEqualTo("servicio-de-pagos")
                .jsonPath("$.status").isEqualTo("ACTIVE");
    }

    @Test
    void slug_duplicado_produce_409() {
        client("acme", "owner", "ADMIN")
                .post().uri("/api/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Checkout Service"))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void nombre_invalido_produce_400() {
        client("acme", "owner", "ADMIN")
                .post().uri("/api/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "@@@"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Construye un WebTestClient con un JWT de prueba ya configurado en el
     * mock del decoder. orgSlug puede ser null para simular un token sin el
     * claim organization_id.
     */
    private WebTestClient client(String orgSlug, String subject, String... roles) {
        String token = "tok-" + subject;
        var builder = Jwt.withTokenValue(token)
                .header("alg", "none")
                .subject(subject)
                .claim("realm_access", Map.of("roles", Arrays.asList(roles)))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));
        if (orgSlug != null) {
            builder.claim("organization_id", orgSlug);
        }
        given(jwtDecoder.decode(token)).willReturn(Mono.just(builder.build()));
        return client.mutate()
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
    }

    private String resolveId(String orgSlug, String projectSlug) {
        var body = client(orgSlug, "resolver", "DEVELOPER")
                .get().uri("/api/projects")
                .exchange()
                .expectBodyList(new ParameterizedTypeReference<Map<String, Object>>() {})
                .returnResult()
                .getResponseBody();

        assertThat(body).isNotNull();
        return body.stream()
                .filter(m -> projectSlug.equals(m.get("slug")))
                .map(m -> (String) m.get("id"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Proyecto '" + projectSlug + "' no encontrado en " + orgSlug));
    }
}