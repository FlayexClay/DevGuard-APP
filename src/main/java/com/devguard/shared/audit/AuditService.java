package com.devguard.shared.audit;

import com.devguard.shared.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.Parameter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private static final String INSERT = """
            INSERT INTO audit_events (
                organization_id, actor_subject, actor_email, action,
                resource_type, resource_id, outcome, ip_address, user_agent, metadata)
            VALUES (
                :organizationId, :actorSubject, :actorEmail, :action,
                :resourceType, :resourceId, :outcome,
                CAST(:ipAddress AS inet), :userAgent, CAST(:metadata AS jsonb))
            """;

    private final DatabaseClient db;

    public AuditService(DatabaseClient db) { this.db = db; }

    public Mono<Void> record(TenantContext tenant, AuditAction action, String resourceType,
                             String resourceId, Outcome outcome, String metadataJson) {
        return db.sql(INSERT)
                .bind("organizationId", tenant != null
                        ? Parameter.from(tenant.organizationId())
                        : Parameter.empty(UUID.class))
                .bind("actorSubject", text(tenant != null ? tenant.subject() : null))
                .bind("actorEmail", text(tenant != null ? tenant.email() : null))
                .bind("action", action.name())
                .bind("resourceType", resourceType)
                .bind("resourceId", text(resourceId))
                .bind("outcome", outcome.name())
                .bind("ipAddress", Parameter.empty(String.class))
                .bind("userAgent", Parameter.empty(String.class))
                .bind("metadata", metadataJson != null ? metadataJson : "{}")
                .fetch()
                .rowsUpdated()
                .doOnError(e -> log.error("No se pudo registrar el evneto de auditoria {}: {}",
                        action, e.getMessage()))
                .onErrorComplete()
                .then();
    }

    public Mono<Void> recordTenantFailure(String subject, String attemptedOrg, String reason) {
        return db.sql(INSERT)
                .bind("organizationId", Parameter.empty(UUID.class))
                .bind("actorSubject", text(subject))
                .bind("actorEmail", Parameter.empty(String.class))
                .bind("action", AuditAction.TENANT_RESOLUTION_FAILED.name())
                .bind("resourceType", "ORGANIZATION")
                .bind("resourceId", text(attemptedOrg))
                .bind("outcome", Outcome.DENIED.name())
                .bind("ipAddress", Parameter.empty(String.class))
                .bind("userAgent", Parameter.empty(String.class))
                .bind("metadata", "{\"reason\":\"" + escape(reason) + "\"}")
                .fetch()
                .rowsUpdated()
                .doOnError(e -> log.error("No se pudo auditar el fallo de tenant: {}",
                        e.getMessage()))
                .onErrorComplete()
                .then();
    }

    private static Object text(String value) {
        return value != null ? Parameter.from(value) : Parameter.empty(String.class);
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public enum Outcome {
        SUCCESS, FAILURE, DENIED
    }
}
