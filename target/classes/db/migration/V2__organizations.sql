-- ===========================================================================
-- V2 - Tenants, usuarios y membresias
-- ===========================================================================

CREATE TABLE organizations (
    id              uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    -- slug es el valor que viaja en el claim organization_id del JWT
    slug            text        NOT NULL,
    name            text        NOT NULL,
    plan            text        NOT NULL DEFAULT 'FREE',
    max_projects    integer     NOT NULL DEFAULT 1,
    max_users       integer     NOT NULL DEFAULT 3,
    status          text        NOT NULL DEFAULT 'ACTIVE',
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT organizations_slug_uk UNIQUE (slug),
    CONSTRAINT organizations_plan_ck
        CHECK (plan IN ('FREE', 'DEVELOPER', 'TEAM', 'BUSINESS')),
    CONSTRAINT organizations_status_ck
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED')),
    CONSTRAINT organizations_slug_format_ck
        CHECK (slug ~ '^[a-z0-9][a-z0-9-]{1,48}[a-z0-9]$')
);

CREATE TRIGGER organizations_updated_at
    BEFORE UPDATE ON organizations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Proyeccion local del usuario de Keycloak. Keycloak es la fuente de verdad
-- de credenciales; aqui solo guardamos lo necesario para relaciones y auditoria.
CREATE TABLE users (
    id              uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    -- claim "sub" del token
    subject         text        NOT NULL,
    email           text        NOT NULL,
    display_name    text,
    last_login_at   timestamptz,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT users_subject_uk UNIQUE (subject),
    CONSTRAINT users_email_uk   UNIQUE (email)
);

CREATE TRIGGER users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE memberships (
    id              uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid        NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id         uuid        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role            text        NOT NULL,
    created_at      timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT memberships_uk UNIQUE (organization_id, user_id),
    CONSTRAINT memberships_role_ck
        CHECK (role IN ('ADMIN', 'DEVELOPER', 'SECURITY_ANALYST'))
);

CREATE INDEX memberships_user_idx ON memberships (user_id);
CREATE INDEX memberships_org_idx  ON memberships (organization_id);
