import { test, expect } from '@playwright/test';
import { getKeycloakToken, BACKEND_URL } from '../helpers/auth.helper';
import { queryAcceptanceDatabase } from '../helpers/acceptance-db.helper';

test.describe('Operations & Finance Modules: T04 Utilities & T05 Lodging/Payroll', () => {
  let managerH1Token: string;

  test.beforeAll(async () => {
    managerH1Token = await getKeycloakToken('manager_h1');
  });

  test('T04: should query utility meters and readings scoped to hotel 1', async ({ request }) => {
    // 1. Query meters for hotel 1
    const metersRes = await request.post(`${BACKEND_URL}/api/v1/utility-meters/filter`, {
      headers: { Authorization: `Bearer ${managerH1Token}` },
      data: {
        hotelId: 1,
        page: 0,
        pageSize: 10,
      },
    });

    expect(metersRes.status()).toBe(200);
    const metersBody = await metersRes.json();
    expect([200, 1000]).toContain(metersBody.code);
    expect(metersBody.result.content.length).toBeGreaterThan(0);
    expect(metersBody.result.content.every((meter: { hotelId: number }) => meter.hotelId === 1)).toBe(true);

    const crossHotelMetersRes = await request.post(`${BACKEND_URL}/api/v1/utility-meters/filter`, {
      headers: { Authorization: `Bearer ${managerH1Token}` },
      data: {
        hotelId: 2,
        page: 0,
        pageSize: 10,
      },
    });
    expect(crossHotelMetersRes.status()).toBe(403);

    const meterId = metersBody.result.content[0].id;

    // 2. Query readings for that meter
    const readingsRes = await request.post(`${BACKEND_URL}/api/v1/utility-readings/filter`, {
      headers: { Authorization: `Bearer ${managerH1Token}` },
      data: {
        meterId,
        page: 0,
        pageSize: 10,
      },
    });

    expect(readingsRes.status()).toBe(200);
    const readingsBody = await readingsRes.json();
    expect([200, 1000]).toContain(readingsBody.code);
    expect(readingsBody.result.content.length).toBeGreaterThan(0);
  });

  test('T05: should download BCA lodging report for hotel 1', async ({ request }) => {
    const resetQueueRes = await request.post(`${BACKEND_URL}/api/v1/lodging-queues/retry/1`, {
      headers: { Authorization: `Bearer ${managerH1Token}` },
    });
    expect(resetQueueRes.status()).toBe(200);
    const resetQueueBody = await resetQueueRes.json();
    expect(resetQueueBody.result.status).toBe('PENDING');

    const today = new Date().toISOString().split('T')[0];
    const exportRes = await request.post(`${BACKEND_URL}/api/v1/lodging-export/download`, {
      headers: { Authorization: `Bearer ${managerH1Token}` },
      data: {
        hotelId: 1,
        date: today,
        category: 'VIETNAMESE',
      },
    });

    expect(exportRes.status()).toBe(200);
    const contentType = exportRes.headers()['content-type'];
    expect(contentType).toContain('vnd.openxmlformats-officedocument.spreadsheetml.sheet');
    const buffer = await exportRes.body();
    expect(buffer.length).toBeGreaterThan(4);
    expect(buffer.subarray(0, 2).toString()).toBe('PK');
  });

  test('T05: should import synthetic payroll CSV and return completion status', async ({ request }) => {
    const periodMonth = new Date().getUTCMonth() + 1;
    const periodYear = new Date().getUTCFullYear();
    const csvContent = [
      'staff_id,period_month,period_year,base_salary,bonus,deductions,net_salary',
      `1,${periodMonth},${periodYear},15000000,2000000,500000,16500000`,
      `3,${periodMonth},${periodYear},20000000,3000000,1000000,22000000`,
    ].join('\n');

    const importRes = await request.post(`${BACKEND_URL}/api/v1/payroll/import`, {
      headers: { Authorization: `Bearer ${managerH1Token}` },
      multipart: {
        file: {
          name: 'synthetic-payroll.csv',
          mimeType: 'text/csv',
          buffer: Buffer.from(csvContent, 'utf-8'),
        },
      },
    });

    expect(importRes.status()).toBe(200);
    const body = await importRes.json();
    expect(body.code).toBe(200);
    expect(body.result).toHaveLength(1);
    expect(body.result[0].row).toBe(2);
    expect(body.result[0].error).toContain('does not belong to your authorized hotel');

    const savedRows = queryAcceptanceDatabase(
      `SELECT COUNT(*) FROM payroll_summary WHERE staff_id = 3 AND period_month = ${periodMonth} AND period_year = ${periodYear} AND is_deleted = FALSE`
    );
    expect(Number(savedRows)).toBe(1);
  });
});
