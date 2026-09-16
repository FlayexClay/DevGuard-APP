-- ===========================================================================
-- V1 - Baseline: extensiones y convenciones
-- ===========================================================================
-- Convenciones del esquema:
--   * PK uuid con gen_random_uuid() (pgcrypto).
--   * timestamptz siempre, nunca timestamp.
--   * Los enums se modelan como text + CHECK. Razon: los enums nativos de
--     Postgres requieren mapeo manual en R2DBC y su ALTER es incomodo.
--   * Toda tabla del tenant lleva organization_id NOT NULL. El aislamiento
--     se aplica en la capa de repositorio, siempre filtrando por esa columna.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
