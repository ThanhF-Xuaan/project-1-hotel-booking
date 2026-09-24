-- Run after 04-08 demo/reference seeds on a fresh development database.
INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'HOUSEKEEPING')
  AND p.action = 'USE' AND p.resource = 'AI_ASSISTANT'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')
  AND p.resource = 'KNOWLEDGE'
  AND p.action IN ('VIEW', 'CREATE', 'PUBLISH', 'REVOKE')
ON CONFLICT DO NOTHING;
