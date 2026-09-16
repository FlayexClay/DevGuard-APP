-- ===========================================================================
-- V5 - Hallazgos y validaciones
-- ===========================================================================

CREATE TABLE security_findings (
    id                  uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     uuid        NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    project_id          uuid        NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    -- Scan que lo detecto por primera vez y el ultimo que lo vio.
    first_seen_scan_id  uuid        REFERENCES scans(id) ON DELETE SET NULL,
    last_seen_scan_id   uuid        REFERENCES scans(id) ON DELETE SET NULL,

    category            text        NOT NULL,
    severity            text        NOT NULL,
    -- Severidad cruda del analizador, antes de que el Risk Engine la ajuste.
    raw_severity        text,
    title               text        NOT NULL,
    description         text,
    recommendation      text,

    -- Identidad estable del hallazgo. Es lo que permite deduplicar entre
    -- scans y distinguir "nuevo" de "sigue abierto". Se calcula como hash de
    -- (analizador, regla, ruta, simbolo), no de la linea: si el codigo se
    -- desplaza, el hallazgo no debe reaparecer como nuevo.
    fingerprint         text        NOT NULL,

    analyzer            text        NOT NULL,
    rule_id             text,
    cwe                 text,
    cve                 text,
    cvss_score          numeric(3,1),
    epss_score          numeric(5,4),

    file_path           text,
    line_start          integer,
    line_end            integer,
    -- Evidencia recortada y enmascarada. Un secreto nunca se guarda completo.
    evidence            text,

    package_name        text,
    installed_version   text,
    fixed_version       text,

    status              text        NOT NULL DEFAULT 'OPEN',
    validation_state    text        NOT NULL DEFAULT 'UNVALIDATED',
    -- Prioridad calculada por el Risk Engine (0 = maxima).
    priority            integer,

    first_seen_at       timestamptz NOT NULL DEFAULT now(),
    last_seen_at        timestamptz NOT NULL DEFAULT now(),
    resolved_at         timestamptz,
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT findings_fingerprint_uk UNIQUE (project_id, fingerprint),
    CONSTRAINT findings_category_ck CHECK (category IN (
        'VULNERABILITY', 'DEPENDENCY', 'SECRET', 'MISCONFIGURATION',
        'CODE_QUALITY', 'EXPOSURE')),
    CONSTRAINT findings_severity_ck CHECK (severity IN (
        'CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO')),
    CONSTRAINT findings_status_ck CHECK (status IN (
        'OPEN', 'CONFIRMED', 'FALSE_POSITIVE', 'ACCEPTED_RISK',
        'RESOLVED', 'REGRESSED')),
    CONSTRAINT findings_validation_ck CHECK (validation_state IN (
        'UNVALIDATED', 'VALIDATING', 'VALIDATED', 'NOT_VALIDABLE')),
    CONSTRAINT findings_cvss_ck CHECK (cvss_score IS NULL
        OR (cvss_score >= 0 AND cvss_score <= 10)),
    CONSTRAINT findings_lines_ck CHECK (line_end IS NULL
        OR line_start IS NULL OR line_end >= line_start),
    CONSTRAINT findings_resolved_ck CHECK (status <> 'RESOLVED'
        OR resolved_at IS NOT NULL)
);

CREATE INDEX findings_org_idx        ON security_findings (organization_id);
-- Consulta principal del dashboard: hallazgos abiertos por severidad.
CREATE INDEX findings_open_idx ON security_findings (project_id, severity, priority)
    WHERE status IN ('OPEN', 'CONFIRMED', 'REGRESSED');
CREATE INDEX findings_cve_idx       ON security_findings (cve) WHERE cve IS NOT NULL;
CREATE INDEX findings_last_seen_idx ON security_findings (last_seen_scan_id);

CREATE TRIGGER findings_updated_at
    BEFORE UPDATE ON security_findings
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Historial de validaciones: automaticas del Validation Engine y manuales
-- del analista. Es la base para medir y reducir falsos positivos.
CREATE TABLE finding_validations (
    id              uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid        NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    finding_id      uuid        NOT NULL REFERENCES security_findings(id) ON DELETE CASCADE,
    method          text        NOT NULL,
    outcome         text        NOT NULL,
    confidence      numeric(3,2),
    notes           text,
    -- NULL cuando la validacion fue automatica.
    validated_by    uuid        REFERENCES users(id) ON DELETE SET NULL,
    validated_at    timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT validations_method_ck CHECK (method IN (
        'MANUAL', 'REACHABILITY', 'SAFE_PROBE', 'PATTERN_REVIEW', 'ENTROPY')),
    CONSTRAINT validations_outcome_ck CHECK (outcome IN (
        'CONFIRMED', 'REJECTED', 'INCONCLUSIVE')),
    CONSTRAINT validations_confidence_ck CHECK (confidence IS NULL
        OR (confidence >= 0 AND confidence <= 1)),
    CONSTRAINT validations_manual_ck CHECK (method <> 'MANUAL'
        OR validated_by IS NOT NULL)
);

CREATE INDEX validations_finding_idx ON finding_validations (finding_id, validated_at DESC);
