-- ===========================================================================
-- V6 - Security Score y auditoria
-- ===========================================================================

-- Serie historica, no un valor mutable: la tendencia del score es parte del
-- producto. Un registro por scan que produjo un score.
CREATE TABLE security_scores (
    id                  uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     uuid        NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    project_id          uuid        NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    scan_id             uuid        REFERENCES scans(id) ON DELETE SET NULL,

    score               integer     NOT NULL,
    grade               text        NOT NULL,
    previous_score      integer,

    critical_count      integer     NOT NULL DEFAULT 0,
    high_count          integer     NOT NULL DEFAULT 0,
    medium_count        integer     NOT NULL DEFAULT 0,
    low_count           integer     NOT NULL DEFAULT 0,
    info_count          integer     NOT NULL DEFAULT 0,

    new_findings        integer     NOT NULL DEFAULT 0,
    resolved_findings   integer     NOT NULL DEFAULT 0,

    -- Desglose del calculo. El documento de diseno exige que el score sea
    -- explicable: aqui se guarda el aporte de cada factor para poder
    -- responder "por que 82 y no 90".
    breakdown           jsonb       NOT NULL DEFAULT '{}'::jsonb,
    algorithm_version   text        NOT NULL DEFAULT 'v1',

    calculated_at       timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT scores_uk UNIQUE (scan_id),
    CONSTRAINT scores_range_ck CHECK (score >= 0 AND score <= 100),
    CONSTRAINT scores_grade_ck CHECK (grade IN ('A', 'B', 'C', 'D', 'E', 'F'))
);

CREATE INDEX scores_project_time_idx ON security_scores (project_id, calculated_at DESC);
CREATE INDEX scores_org_idx          ON security_scores (organization_id);

-- Auditoria append-only. Sin FK a users para que el borrado de un usuario
-- no destruya el rastro; se conserva subject y email tal como estaban.
CREATE TABLE audit_events (
    id              uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid,
    actor_subject   text,
    actor_email     text,
    action          text        NOT NULL,
    resource_type   text        NOT NULL,
    resource_id     text,
    outcome         text        NOT NULL DEFAULT 'SUCCESS',
    ip_address      inet,
    user_agent      text,
    metadata        jsonb       NOT NULL DEFAULT '{}'::jsonb,
    occurred_at     timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT audit_outcome_ck CHECK (outcome IN ('SUCCESS', 'FAILURE', 'DENIED'))
);

CREATE INDEX audit_org_time_idx  ON audit_events (organization_id, occurred_at DESC);
CREATE INDEX audit_actor_idx     ON audit_events (actor_subject, occurred_at DESC);
-- Intentos de acceso cruzado entre tenants: la alerta de seguridad que
-- realmente importa vigilar en un SaaS multi-tenant.
CREATE INDEX audit_denied_idx ON audit_events (occurred_at DESC)
    WHERE outcome = 'DENIED';

-- Vista de apoyo para el dashboard: ultimo score vigente por proyecto.
CREATE VIEW current_security_scores AS
SELECT DISTINCT ON (project_id)
       project_id,
       organization_id,
       scan_id,
       score,
       grade,
       previous_score,
       critical_count,
       high_count,
       medium_count,
       low_count,
       calculated_at
FROM security_scores
ORDER BY project_id, calculated_at DESC;
