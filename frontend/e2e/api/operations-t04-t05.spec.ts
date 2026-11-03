import { test, expect } from '@playwright/test';
import { getKeycloakToken, BACKEND_URL } from '../helpers/auth.helper';
import { queryAcceptanceDatabase } from '../helpers/acceptance-db.helper';
import { inspectSyntheticWorkbook } from '../helpers/xlsx.helper';

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

  test('T05: should reject unresolved staff scope on reads and queue writes', async ({ request }) => {
    expect(queryAcceptanceDatabase("SELECT scope_type || ':' || scope_entity_id FROM staffs WHERE username = 'manager_h1'"))
      .toBe('PROPERTY:1');
    const before = queryAcceptanceDatabase('SELECT status FROM lodging_queues WHERE id = 1');
    try {
      for (const scope of [{ type: 'CHAIN', id: 'NULL' }, { type: 'REGION', id: '1' }, { type: 'PROPERTY', id: '65537' }]) {
        // Fault injection is confined to the synthetic acceptance DB and restored in finally.
        queryAcceptanceDatabase(`UPDATE staffs SET scope_type = '${scope.type}', scope_entity_id = ${scope.id} WHERE username = 'manager_h1'`);
        for (const route of ['lodging-queues/filter', 'stay-guests/filter', 'lodging-queues/retry/1', 'lodging-export/download']) {
          const res = await request.post(`${BACKEND_URL}/api/v1/${route}`, {
            headers: { Authorization: `Bearer ${managerH1Token}` },
            data: { hotelId: 1, date: new Date().toISOString().split('T')[0], category: 'VIETNAMESE', page: 0, pageSize: 10 },
          });
          expect(res.status(), `${scope.type}/${scope.id} must not access ${route}`).toBe(403);
        }
      }
      expect(queryAcceptanceDatabase('SELECT status FROM lodging_queues WHERE id = 1')).toBe(before);
    } finally {
      queryAcceptanceDatabase("UPDATE staffs SET scope_type = 'PROPERTY', scope_entity_id = 1 WHERE username = 'manager_h1'");
    }
  });

  test('review: booking-room reads and associations enforce hotel scope with real tokens', async ({ request }) => {
    const headers = { Authorization: `Bearer ${managerH1Token}` };
    const guestBefore = queryAcceptanceDatabase("SELECT row_to_json(g)::text FROM stay_guests g WHERE id=1");
    expect(queryAcceptanceDatabase('SELECT hotel_id FROM bookings WHERE id=1')).toBe('1');
    expect((await request.get(`${BACKEND_URL}/api/v1/stay-guests/by-booking-room/1`)).status()).toBe(401);
    const own = await request.get(`${BACKEND_URL}/api/v1/stay-guests/by-booking-room/1`, { headers });
    expect(own.status()).toBe(200);
    expect((await own.json()).result.map((g: { id: number }) => g.id)).toContain(1);
    try {
      // Synthetic dependency fault only; restore the parent hotel in finally.
      queryAcceptanceDatabase('UPDATE bookings SET hotel_id=2 WHERE id=1');
      const foreign = await request.get(`${BACKEND_URL}/api/v1/stay-guests/by-booking-room/1`, { headers });
      expect(foreign.status()).toBe(403);
      expect(await foreign.text()).not.toContain('001090012345');
      const created = await request.post(`${BACKEND_URL}/api/v1/stay-guests/create`, { headers, data: {
        hotelId: 1, bookingRoomId: 1, roomNumber: '101', fullName: 'SYNTHETIC_REJECTED',
        documentType: 'CCCD', documentNumber: 'SYNTHETIC_REJECTED',
        checkInTime: '2026-11-03T14:00:00+07:00', expectedCheckOutTime: '2026-11-04T12:00:00+07:00',
      } });
      expect(created.status()).toBe(400);
      const updated = await request.put(`${BACKEND_URL}/api/v1/stay-guests/update/1`, { headers,
        data: { bookingRoomId: 1, fullName: 'SYNTHETIC_REJECTED' } });
      expect(updated.status()).toBe(400);
      expect(queryAcceptanceDatabase("SELECT count(*) FROM stay_guests WHERE full_name='SYNTHETIC_REJECTED'")).toBe('0');
      expect(queryAcceptanceDatabase("SELECT row_to_json(g)::text FROM stay_guests g WHERE id=1")).toBe(guestBefore);
    } finally {
      queryAcceptanceDatabase('UPDATE bookings SET hotel_id=1 WHERE id=1');
    }
  });

  test('T05: should reject invalid cutoff and cross-hotel export without changing queue state', async ({ request }) => {
    const before = queryAcceptanceDatabase('SELECT status FROM lodging_queues WHERE id = 1');
    const date = new Date().toISOString().split('T')[0];
    for (const cutOffHour of [-1, 24]) {
      const invalid = await request.post(`${BACKEND_URL}/api/v1/lodging-export/download`, {
        headers: { Authorization: `Bearer ${managerH1Token}` },
        data: { hotelId: 1, date, category: 'VIETNAMESE', cutOffHour },
      });
      expect(invalid.status()).toBe(400);
      expect((await invalid.json()).code).toBe(1002);
    }
    const crossHotel = await request.post(`${BACKEND_URL}/api/v1/lodging-export/download`, {
      headers: { Authorization: `Bearer ${managerH1Token}` },
      data: { hotelId: 2, date, category: 'VIETNAMESE' },
    });
    expect(crossHotel.status()).toBe(403);
    expect(queryAcceptanceDatabase('SELECT status FROM lodging_queues WHERE id = 1')).toBe(before);
  });

  test('T05: should export the current technical workbook and preserve queue status on repeat export', async ({ request }) => {
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
    const workbook = inspectSyntheticWorkbook(buffer);
    expect(workbook.sheetName).toBe('Thong_Bao_Luu_Tru_VN');
    expect(workbook.rows).toHaveLength(2);
    expect(workbook.rows[0]).toEqual([
      'STT', 'Họ và tên', 'Ngày tháng năm sinh', 'Giới tính', 'Quốc tịch', 'Loại giấy tờ', 'Số giấy tờ',
      'Nơi ĐKTT / Nơi cấp', 'Chỗ ở hiện nay', 'Ngày đến', 'Ngày đi (dự kiến)', 'Số phòng', 'Lý do lưu trú',
    ]);
    expect(Number(workbook.rows[1][0])).toBe(1);
    expect(workbook.rows[1].slice(1, 8)).toEqual([
      'NGUYEN VAN A', '15/05/1990', 'Nam', 'Vietnam', 'CCCD', '001090012345', 'Ha Noi, Viet Nam',
    ]);
    expect(['inlineStr', 's', 'str']).toContain(workbook.cellTypes[1][6]);
    expect(workbook.rows[1][11]).toBe('101');
    const expectedTimes = queryAcceptanceDatabase(
      "SELECT to_char(check_in_time AT TIME ZONE 'Asia/Ho_Chi_Minh', 'DD/MM/YYYY HH24:MI') || '|' || " +
      "to_char(expected_check_out_time AT TIME ZONE 'Asia/Ho_Chi_Minh', 'DD/MM/YYYY HH24:MI') FROM stay_guests WHERE id = 1"
    ).split('|');
    expect(workbook.rows[1].slice(9, 11)).toEqual(expectedTimes);
    expect(workbook.formulaCells).toBe(0);
    expect(queryAcceptanceDatabase(
      "SELECT status || '|' || (exported_at IS NOT NULL)::text || '|' || (batch_reference IS NOT NULL)::text FROM lodging_queues WHERE id = 1"
    )).toBe('EXPORTED|true|true');
    const completionMarker = queryAcceptanceDatabase("SELECT exported_at::text || '|' || batch_reference FROM lodging_queues WHERE id = 1");
    const terminalRetry = await request.post(`${BACKEND_URL}/api/v1/lodging-queues/retry/1`, {
      headers: { Authorization: `Bearer ${managerH1Token}` },
    });
    expect(terminalRetry.status()).toBe(200);
    expect((await terminalRetry.json()).result.status).toBe('EXPORTED');
    const noop = await request.put(`${BACKEND_URL}/api/v1/stay-guests/update/1`, {
      headers: { Authorization: `Bearer ${managerH1Token}` }, data: {},
    });
    expect(noop.status()).toBe(200);
    const changed = await request.put(`${BACKEND_URL}/api/v1/stay-guests/update/1`, {
      headers: { Authorization: `Bearer ${managerH1Token}` }, data: { fullName: 'SYNTHETIC_BLOCKED_CHANGE' },
    });
    expect(changed.status()).toBe(409);
    expect((await changed.json()).code).toBe(9073);
    const repeat = await request.post(`${BACKEND_URL}/api/v1/lodging-export/download`, {
      headers: { Authorization: `Bearer ${managerH1Token}` },
      data: { hotelId: 1, date: today, category: 'VIETNAMESE' },
    });
    expect(repeat.status()).toBe(400);
    expect(queryAcceptanceDatabase('SELECT status FROM lodging_queues WHERE id = 1')).toBe('EXPORTED');
    expect(queryAcceptanceDatabase("SELECT exported_at::text || '|' || batch_reference FROM lodging_queues WHERE id = 1"))
      .toBe(completionMarker);
  });

  test('T05: should include fractional seconds before cutoff and exclude the next-hour boundary', async ({ request }) => {
    const date = new Date().toISOString().split('T')[0];
    const originalTime = queryAcceptanceDatabase('SELECT check_in_time::text FROM stay_guests WHERE id = 1');
    const originalStatus = queryAcceptanceDatabase('SELECT status FROM lodging_queues WHERE id = 1');
    expect(originalStatus).toBe('EXPORTED');
    try {
      for (const boundary of [
        { interval: '23 hours 59 minutes 59.5 seconds', expectedStatus: 200 },
        { interval: '1 day', expectedStatus: 400 },
      ]) {
        queryAcceptanceDatabase(
          `UPDATE stay_guests SET check_in_time = ('${date}'::date + INTERVAL '${boundary.interval}') AT TIME ZONE 'Asia/Ho_Chi_Minh' WHERE id = 1`
        );
        // Explicit synthetic setup: production retry must preserve an already exported queue.
        queryAcceptanceDatabase("UPDATE lodging_queues SET status='PENDING',exported_at=NULL,batch_reference=NULL WHERE id=1");
        const retry = await request.post(`${BACKEND_URL}/api/v1/lodging-queues/retry/1`, {
          headers: { Authorization: `Bearer ${managerH1Token}` },
        });
        expect(retry.status()).toBe(200);
        const exported = await request.post(`${BACKEND_URL}/api/v1/lodging-export/download`, {
          headers: { Authorization: `Bearer ${managerH1Token}` },
          data: { hotelId: 1, date, cutOffHour: 23, category: 'VIETNAMESE' },
        });
        expect(exported.status(), boundary.interval).toBe(boundary.expectedStatus);
        if (boundary.expectedStatus === 200) {
          expect(inspectSyntheticWorkbook(await exported.body()).rows).toHaveLength(2);
        } else {
          expect((await exported.json()).code).toBe(9072);
          expect(queryAcceptanceDatabase('SELECT status FROM lodging_queues WHERE id = 1')).toBe('PENDING');
        }
      }
    } finally {
      const timestamp = originalTime.replaceAll("'", "''");
      queryAcceptanceDatabase(`UPDATE stay_guests SET check_in_time = '${timestamp}'::timestamptz WHERE id = 1`);
      queryAcceptanceDatabase("UPDATE lodging_queues SET status = 'EXPORTED' WHERE id = 1");
    }
  });

  test('T05: should produce one batch for two concurrent export requests', async ({ request }) => {
    queryAcceptanceDatabase("UPDATE lodging_queues SET status='PENDING',exported_at=NULL,batch_reference=NULL WHERE id=1");
    const retry = await request.post(`${BACKEND_URL}/api/v1/lodging-queues/retry/1`, {
      headers: { Authorization: `Bearer ${managerH1Token}` },
    });
    expect(retry.status()).toBe(200);
    const date = new Date().toISOString().split('T')[0];
    const responses = await Promise.all([0, 1].map(() => request.post(`${BACKEND_URL}/api/v1/lodging-export/download`, {
      headers: { Authorization: `Bearer ${managerH1Token}` },
      data: { hotelId: 1, date, category: 'VIETNAMESE' },
    })));
    expect(responses.map(response => response.status()).sort((left, right) => left - right)).toEqual([200, 400]);
    const winner = responses.find(response => response.status() === 200)!;
    const loser = responses.find(response => response.status() === 400)!;
    expect(inspectSyntheticWorkbook(await winner.body()).rows).toHaveLength(2);
    expect((await loser.json()).code).toBe(9072);
    expect(queryAcceptanceDatabase('SELECT status FROM lodging_queues WHERE id = 1')).toBe('EXPORTED');
    expect(queryAcceptanceDatabase('SELECT batch_reference FROM lodging_queues WHERE id = 1')).toMatch(/^BCA-/);
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

  test('review: payroll rejects lossy money over HTTP without inserting a row', async ({ request }) => {
    expect(queryAcceptanceDatabase('SELECT count(*) FROM payroll_summary WHERE staff_id=3 AND period_year=2099 AND period_month=10')).toBe('0');
    const response = await request.post(`${BACKEND_URL}/api/v1/payroll/import`, {
      headers: { Authorization: `Bearer ${managerH1Token}` }, multipart: { file: {
        name: 'synthetic-lossy.csv', mimeType: 'text/csv', buffer: Buffer.from(
          'staff_id,period_month,period_year,base_salary,bonus,deductions,net_salary\n3,10,2099,100.005,0.005,0,100.010'),
      } },
    });
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.result).toHaveLength(1);
    expect(body.result[0].error).toContain('SALARY_AMOUNT_NOT_REPRESENTABLE');
    expect(queryAcceptanceDatabase('SELECT count(*) FROM payroll_summary WHERE staff_id=3 AND period_year=2099 AND period_month=10')).toBe('0');
  });
});
