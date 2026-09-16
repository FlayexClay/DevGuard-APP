-- ===========================================================================
-- V4 - Scans y resultados crudos por analizador
-- ===========================================================================

CREATE TABLE scans (
    id                  uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     uuid        NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    project_id          uuid        NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    repository_id       uuid        REFERENCES repositories(id) ON DELETE SET NULL,
    status              text        NOT NULL DEFAULT 'REQUESTED',
    trigger_type        text        NOT NULL DEFAULT 'MANUAL',
    commit_sha          text,
    branch              text,
    requested_by        uuid        REFERENCES users(id) ON DELETE SET NULL,
    requested_at        timestamptz NOT NULL DEFAULT now(),
    started_at          timestamptz,
    finished_at         timestamptz,
    duration_ms         bigint,
    error_code          text,
    error_message       text,
    -- Idempotencia del consumidor de Kafka: el worker descarta un evento
    -- repetido si la clave ya fue procesada.
    idempotency_key     text,
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT scans_status_ck CHECK (status IN (
        'REQUESTED', 'QUEUED', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT scans_trigger_ck CHECK (trigger_type IN (
        'MANUAL', 'SCHEDULED', 'WEBHOOK', 'API')),
    CONSTRAINT scans_idempotency_uk UNIQUE (idempotency_key)
);

CREATE INDEX scans_project_requested_idx ON scans (project_id, requested_at DESC);
CREATE INDEX scans_org_idx               ON scans (organization_id);
-- Cola de trabajo: solo los scans que aun no terminan.
CREATE INDEX scans_pending_idx ON scans (status, requested_at)
    WHERE status IN ('REQUESTED', 'QUEUED', 'RUNNING');

CREATE TRIGGER scans_updated_at
    BEFORE UPDATE ON scans
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Un registro por analizador ejecutado dentro de un scan.
-- Permite que un scan sea parcialmente exitoso: si Trivy falla, Semgrep
-- y el escaneo de secretos siguen aportando hallazgos.
CREATE TABLE scan_results (
    id              uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid        NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    scan_id         uuid        NOT NULL REFERENCES scans(id) ON DELETE CASCADE,
    analyzer        text        NOT NULL,
    analyzer_version text,
    status          text        NOT NULL DEFAULT 'PENDING',
    findings_count  integer     NOT NULL DEFAULT 0,
    started_at      timestamptz,
    finished_at     timestamptz,
    duration_ms     bigint,
    -- Salida normalizada del analizador. El crudo completo no se guarda en BD.
    summary         jsonb       NOT NULL DEFAULT '{}'::jsonb,
    error_message   text,
    created_at      timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT scan_results_uk UNIQUE (scan_id, analyzer),
    CONSTRAINT scan_results_analyzer_ck CHECK (analyzer IN (
        'SAST', 'DEPENDENCY', 'SECRET', 'DOCKERFILE', 'CONFIG', 'API', 'TLS')),
    CONSTRAINT scan_results_status_ck CHECK (status IN (
        'PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'SKIPPED'))
);

CREATE INDEX scan_results_scan_idx ON scan_results (scan_id);
