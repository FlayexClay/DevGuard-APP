-- ===========================================================================
-- V3 - Proyectos, repositorios y credenciales de integracion
-- ===========================================================================

CREATE TABLE projects (
    id              uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid        NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name            text        NOT NULL,
    slug            text        NOT NULL,
    description     text,
    status          text        NOT NULL DEFAULT 'ACTIVE',
    created_by      uuid        REFERENCES users(id) ON DELETE SET NULL,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),

    -- El slug es unico dentro del tenant, no globalmente.
    CONSTRAINT projects_slug_uk UNIQUE (organization_id, slug),
    CONSTRAINT projects_status_ck
        CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE INDEX projects_org_idx ON projects (organization_id);

CREATE TRIGGER projects_updated_at
    BEFORE UPDATE ON projects
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE repositories (
    id              uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid        NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    project_id      uuid        NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    provider        text        NOT NULL,
    -- owner/repo tal como lo expone el proveedor
    external_id     text,
    full_name       text        NOT NULL,
    clone_url       text        NOT NULL,
    default_branch  text        NOT NULL DEFAULT 'main',
    visibility      text        NOT NULL DEFAULT 'PRIVATE',
    -- Sin autorizacion explicita no se escanea. Regla de producto, no opcional.
    authorized      boolean     NOT NULL DEFAULT false,
    authorized_at   timestamptz,
    authorized_by   uuid        REFERENCES users(id) ON DELETE SET NULL,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT repositories_uk UNIQUE (project_id, provider, full_name),
    CONSTRAINT repositories_provider_ck
        CHECK (provider IN ('GITHUB', 'GITLAB')),
    CONSTRAINT repositories_visibility_ck
        CHECK (visibility IN ('PUBLIC', 'PRIVATE', 'INTERNAL')),
    CONSTRAINT repositories_authorized_ck
        CHECK (authorized = false OR authorized_at IS NOT NULL)
);

CREATE INDEX repositories_project_idx ON repositories (project_id);
CREATE INDEX repositories_org_idx     ON repositories (organization_id);

CREATE TRIGGER repositories_updated_at
    BEFORE UPDATE ON repositories
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Referencias a credenciales, NO credenciales.
-- secret_ref apunta al almacen externo (Secrets Manager, SSM, Vault).
-- Nunca guardar aqui el token en claro: el documento de diseno lo exige.
CREATE TABLE integration_credentials (
    id              uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid        NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    provider        text        NOT NULL,
    secret_ref      text        NOT NULL,
    account_login   text,
    scopes          text[],
    status          text        NOT NULL DEFAULT 'ACTIVE',
    expires_at      timestamptz,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT integration_credentials_uk UNIQUE (organization_id, provider),
    CONSTRAINT integration_credentials_provider_ck
        CHECK (provider IN ('GITHUB', 'GITLAB')),
    CONSTRAINT integration_credentials_status_ck
        CHECK (status IN ('ACTIVE', 'EXPIRED', 'REVOKED'))
);

CREATE TRIGGER integration_credentials_updated_at
    BEFORE UPDATE ON integration_credentials
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
