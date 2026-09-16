-- ===========================================================================
-- V7 - Datos semilla (solo desarrollo local)
-- ===========================================================================
-- Los slugs coinciden con el claim organization_id del realm de Keycloak:
-- "acme" para los usuarios legitimos y "globex" para probar que el
-- aislamiento entre tenants funciona.
--
-- En AWS esta migracion se excluye con
-- spring.flyway.locations=classpath:db/migration (sin db/seed).
-- Si prefieres mantenerla fuera del flujo, mueve el archivo a db/seed.

INSERT INTO organizations (slug, name, plan, max_projects, max_users)
VALUES ('acme',   'ACME Corp',      'DEVELOPER', 5, 10),
       ('globex', 'Globex Limited', 'FREE',      1,  3)
ON CONFLICT (slug) DO NOTHING;

INSERT INTO projects (organization_id, name, slug, description)
SELECT o.id, 'Checkout Service', 'checkout-service',
       'Proyecto de prueba para validar el flujo de scans'
FROM organizations o WHERE o.slug = 'acme'
ON CONFLICT (organization_id, slug) DO NOTHING;

INSERT INTO projects (organization_id, name, slug, description)
SELECT o.id, 'Legacy Portal', 'legacy-portal',
       'Proyecto del tenant vecino: no debe ser visible desde acme'
FROM organizations o WHERE o.slug = 'globex'
ON CONFLICT (organization_id, slug) DO NOTHING;
