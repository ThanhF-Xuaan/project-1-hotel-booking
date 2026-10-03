-- Run after Flyway migrations (V004) and role seeds (database/04-seed-data-global-master-data.sql) on a fresh development database.
-- This file is NOT mounted by Docker Compose; run manually like 09-seed-ai-permissions.sql.

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('CHAIN_ADMIN', 'PROPERTY_MANAGER')
  AND p.resource = 'UTILITY'
  AND p.action IN ('VIEW', 'CREATE', 'UPDATE')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'REGION_MANAGER'
  AND p.resource = 'UTILITY' AND p.action = 'VIEW'
ON CONFLICT DO NOTHING;
