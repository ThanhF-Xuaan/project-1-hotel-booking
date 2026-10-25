import { test, expect } from '@playwright/test';
import { getKeycloakToken, BACKEND_URL } from '../helpers/auth.helper';

test.describe('Sandbox Authentication & Role-Based Access Control', () => {
  test('should acquire valid JWT tokens for all sandbox roles', async () => {
    const roles = [
      { user: 'admin', expectedRole: 'CHAIN_ADMIN' },
      { user: 'region_mgr', expectedRole: 'REGION_MANAGER' },
      { user: 'manager_h1', expectedRole: 'PROPERTY_MANAGER' },
      { user: 'manager_h2', expectedRole: 'PROPERTY_MANAGER' },
      { user: 'reception_h1', expectedRole: 'RECEPTIONIST' },
      { user: 'test_customer', expectedRole: 'CUSTOMER' },
    ];

    for (const { user, expectedRole } of roles) {
      const token = await getKeycloakToken(user);
      expect(token).toBeTruthy();

      const payloadBase64 = token.split('.')[1];
      const payloadJson = Buffer.from(payloadBase64, 'base64').toString('utf-8');
      const payload = JSON.parse(payloadJson);

      expect(payload.preferred_username).toBe(user);
      const realmRoles: string[] = payload.realm_access?.roles || [];
      expect(realmRoles).toContain(expectedRole);
    }
  });

  test('should enforce RBAC on backend API: admin access granted vs unauthorized/forbidden', async ({ request }) => {
    // 1. Unauthenticated request -> 401
    const unauthRes = await request.post(`${BACKEND_URL}/api/v1/regions/filter`, {
      data: { page: 0, pageSize: 10 },
    });
    expect(unauthRes.status()).toBe(401);

    // 2. Receptionist access to region management -> 403 (needs CHAIN_ADMIN or REGION_MANAGER)
    const recepToken = await getKeycloakToken('reception_h1');
    const recepRes = await request.post(`${BACKEND_URL}/api/v1/regions/filter`, {
      headers: {
        Authorization: `Bearer ${recepToken}`,
      },
      data: { page: 0, pageSize: 10 },
    });
    expect(recepRes.status()).toBe(403);

    // 3. Chain admin access -> 200 OK
    const adminToken = await getKeycloakToken('admin');
    const adminRes = await request.post(`${BACKEND_URL}/api/v1/regions/filter`, {
      headers: {
        Authorization: `Bearer ${adminToken}`,
      },
      data: { page: 0, pageSize: 10 },
    });
    expect(adminRes.status()).toBe(200);
    const body = await adminRes.json();
    expect(body.code).toBe(200);
    expect(body.result).toBeDefined();
  });
});
