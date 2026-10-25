import { test, expect } from '@playwright/test';
import { BACKEND_URL } from '../helpers/auth.helper';
import { getAcceptanceDatabaseDigest } from '../helpers/acceptance-db.helper';

test.describe('T12 Zero Business Writes — Local Integration', () => {
  test('an actual advisor chat request leaves persisted database data unchanged', async ({ request }) => {
    const databaseBefore = getAcceptanceDatabaseDigest();
    const response = await request.post(`${BACKEND_URL}/api/v1/public/ai/room-advisor/chat`, {
      data: {
        contractVersion: '2.0',
        message: 'Tìm phòng ở Hà Nội cho 2 người lớn từ 2026-11-01 đến 2026-11-03',
      },
      timeout: 30_000,
    });

    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.result).toBeDefined();
    expect(getAcceptanceDatabaseDigest()).toBe(databaseBefore);
  });
});
