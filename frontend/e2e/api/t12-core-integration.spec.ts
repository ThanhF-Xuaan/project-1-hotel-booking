import { randomUUID } from 'node:crypto';
import { expect, test } from '@playwright/test';
import { BACKEND_URL } from '../helpers/auth.helper';
import { getAcceptanceDatabaseDigest, queryAcceptanceDatabase } from '../helpers/acceptance-db.helper';

interface AdvisorResult {
  sessionId: string;
  sessionVersion: number;
  status: string;
  recommendations: { room: { hotelRoomTypeId: number } }[];
}

test('T12 refresh follows Core group blocks and release without business writes', async ({ request }) => {
  // All writes below are controlled setup/cleanup in the disposable DB, outside advisor requests.
  const marker = `M1CORE-${randomUUID()}`;
  const dates = JSON.parse(queryAcceptanceDatabase(`
    SELECT json_build_object(
      'checkIn', ((CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh')::date + 7)::text,
      'checkOut', ((CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh')::date + 9)::text
    );
  `)) as { checkIn: string; checkOut: string };
  for (const date of Object.values(dates)) expect(date).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  expect(queryAcceptanceDatabase("SELECT name FROM hotels WHERE id=1;")).toBe('Acceptance Luxury Hanoi (Hà Nội)');

  async function readAvailability(): Promise<number[]> {
    const response = await request.post(`${BACKEND_URL}/api/v1/public/availability/search`, {
      data: { hotelRoomTypeId: 1, startDate: dates.checkIn, endDate: dates.checkIn },
    });
    expect(response.status()).toBe(200);
    const body = await response.json() as { result: { availableCount: number }[] };
    expect(body.result).toHaveLength(1);
    return body.result.map(row => row.availableCount);
  }

  async function advisorCall(path: 'search' | 'refresh', payload: object): Promise<AdvisorResult> {
    const digest = getAcceptanceDatabaseDigest();
    const response = await request.post(`${BACKEND_URL}/api/v1/public/ai/room-advisor/${path}`, {
      data: { contractVersion: '2.0', ...payload },
    });
    expect(response.status()).toBe(200);
    const body = await response.json() as { result: AdvisorResult };
    expect(body.result).toBeDefined();
    expect(body.result.sessionId).toBeTruthy();
    expect(body.result.status).toBe('RESULTS');
    expect(getAcceptanceDatabaseDigest()).toBe(digest);
    return body.result;
  }

  function roomIds(result: AdvisorResult): number[] {
    return result.recommendations.map(card => card.room.hotelRoomTypeId);
  }

  const originalAvailability = await readAvailability();
  expect(originalAvailability[0]).toBeGreaterThan(0);
  const original = await advisorCall('search', {
    criteria: { hotelId: 1, locationKeyword: 'Hà Nội', checkInDate: dates.checkIn,
      checkOutDate: dates.checkOut, adults: 2, children: 0, roomsCount: 1 },
  });
  expect(roomIds(original)).toContain(1);

  try {
    queryAcceptanceDatabase(`
      WITH company AS (
        INSERT INTO companies (name,tax_code,status)
        VALUES ('Synthetic acceptance group block','${marker}','ACTIVE') RETURNING id
      ), contract AS (
        INSERT INTO group_contracts (contract_number,hotel_id,company_id,sales_staff_id,
          group_name,company_tax_code,check_in_date,check_out_date,total_rooms,
          deposit_due_date,cutoff_date,final_payment_due_date,status)
        SELECT '${marker}',1,company.id,1,'Synthetic group','${marker}',
          '${dates.checkIn}'::date,'${dates.checkOut}'::date,GREATEST(6,h.total_quantity),
          CURRENT_TIMESTAMP+INTERVAL '1 day',CURRENT_TIMESTAMP+INTERVAL '2 days',
          CURRENT_TIMESTAMP+INTERVAL '3 days','DEPOSIT_PAID'
        FROM company CROSS JOIN hotel_room_types h WHERE h.id=1 RETURNING id
      ), block AS (
        INSERT INTO group_room_blocks (contract_id,hotel_room_type_id,agreed_rate,total_blocked_rooms,status)
        SELECT contract.id,1,100000,h.total_quantity,'DEFINITE'
        FROM contract CROSS JOIN hotel_room_types h WHERE h.id=1 RETURNING id
      )
      INSERT INTO group_room_block_daily (room_block_id,hotel_room_type_id,stay_date,
        blocked_quantity,picked_up_quantity,released_quantity,status)
      SELECT block.id,1,day::date,h.total_quantity,0,0,'ACTIVE'
      FROM block CROSS JOIN hotel_room_types h CROSS JOIN generate_series(
        '${dates.checkIn}'::date,'${dates.checkOut}'::date-1,INTERVAL '1 day') day WHERE h.id=1;
    `);
    expect(await readAvailability()).toEqual([0]);
    const blocked = await advisorCall('refresh', {
      sessionId: original.sessionId, expectedSessionVersion: original.sessionVersion,
    });
    expect(roomIds(blocked)).not.toContain(1);
    expect(blocked.recommendations.length).toBeGreaterThan(0);

    queryAcceptanceDatabase(`
      UPDATE group_room_block_daily SET status='RELEASED'
      WHERE room_block_id IN (
        SELECT b.id FROM group_room_blocks b JOIN group_contracts c ON c.id=b.contract_id
        WHERE c.contract_number='${marker}'
      );
    `);
    expect(await readAvailability()).toEqual(originalAvailability);
    const released = await advisorCall('refresh', {
      sessionId: blocked.sessionId, expectedSessionVersion: blocked.sessionVersion,
    });
    expect(roomIds(released)).toContain(1);
  } finally {
    queryAcceptanceDatabase(`
      BEGIN;
      DELETE FROM group_contracts WHERE contract_number='${marker}';
      DELETE FROM companies WHERE tax_code='${marker}';
      COMMIT;
    `);
    expect(queryAcceptanceDatabase(`SELECT count(*) FROM group_contracts WHERE contract_number='${marker}';`)).toBe('0');
  }
});
