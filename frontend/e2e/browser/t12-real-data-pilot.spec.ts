import { expect, test, type Page } from '@playwright/test';
import { execFileSync } from 'node:child_process';

const api = 'http://127.0.0.1:18082';
const container = 'm1-realdata-pilot-pg';
const date = (days: number) => new Date(Date.now() + days * 86400000).toISOString().slice(0, 10);
test.skip(process.env.T12_REAL_DATA_PILOT !== 'true', 'Run with the isolated real-data pilot launcher');

function sql(database: string, query: string) {
  return execFileSync('docker', ['exec', container, 'psql', '-XqAt', '-v', 'ON_ERROR_STOP=1',
    '-U', 'postgres', '-d', database, '-c', `BEGIN READ ONLY; ${query}; ROLLBACK;`],
  { encoding: 'utf8' }).trim();
}

function snapshot() {
  return {
    core: sql('m1_pilot_core', `SELECT jsonb_build_array(
      (SELECT count(*) FROM hotels), (SELECT count(*) FROM hotel_room_types),
      (SELECT count(*) FROM room_availability), (SELECT count(*) FROM bookings),
      (SELECT count(*) FROM stay_guests))`),
    catalog: sql('m1_pilot_core', `SELECT md5(jsonb_build_object(
      'rooms',(SELECT jsonb_agg(jsonb_build_array(
        room_type.code,hotel_room_type.base_price,hotel_room_type.total_quantity,
        hotel_room_type.status,hotel_room_type.is_deleted) ORDER BY room_type.code)
        FROM hotel_room_types hotel_room_type JOIN room_types room_type
          ON room_type.id=hotel_room_type.room_type_id WHERE hotel_room_type.hotel_id=4),
      'availability',(SELECT jsonb_agg(jsonb_build_array(
        availability.hotel_room_type_id,availability.date,availability.total_rooms,
        availability.booked_rooms,availability.locked_rooms,availability.ooo_rooms)
        ORDER BY availability.hotel_room_type_id,availability.date)
        FROM room_availability availability JOIN hotel_room_types hotel_room_type
          ON hotel_room_type.id=availability.hotel_room_type_id WHERE hotel_room_type.hotel_id=4)
    )::text)`),
    corpus: sql('t12_pgvector_real_pilot', `SELECT md5(jsonb_agg(jsonb_build_array(
      s.source_id,d.version,d.status,d.content_hash,c.id,md5(c.embedding::text))
      ORDER BY s.source_id)::text)
      FROM rag_sources s JOIN rag_document_versions d ON s.id=d.source_key
      JOIN rag_chunks c ON c.document_id=d.id`),
  };
}

let before: ReturnType<typeof snapshot> | undefined;
test.beforeAll(() => {
  const label = execFileSync('docker', ['inspect', '--format',
    '{{index .Config.Labels "vn.edu.utc.hotel-booking.member1"}}', container], { encoding: 'utf8' });
  expect(label.trim()).toBe('real-data-pilot');
  before = snapshot();
  expect(JSON.parse(before.core)).toEqual([1, 4, 1460, 0, 0]);
  expect(before.catalog).toMatch(/^[a-f0-9]{32}$/);
  expect(before.corpus).toMatch(/^[a-f0-9]{32}$/);
});
test.afterAll(() => { if (before) expect(snapshot()).toEqual(before); });

async function openKnowledge(page: Page, path = '/t12-pilot.html') {
  await page.goto(path);
  await page.getByRole('button', { name: 'Mở Trợ lý Tìm phòng AI' }).click();
  await page.getByRole('button', { name: 'Chính sách & tiện ích' }).click();
  const hotels = page.getByLabel('Khách sạn tra cứu');
  await expect(hotels.locator('option[value="4"]')).toHaveText(/Vinpearl.*Hạ Long/i);
  return hotels;
}

async function ask(page: Page, question: string) {
  await page.getByLabel('Câu hỏi chính sách').fill(question);
  const response = page.waitForResponse(r => r.url() === `${api}/api/v1/public/ai/room-advisor/knowledge/ask`);
  await page.getByRole('button', { name: 'Tra cứu chính sách', exact: true }).click();
  const received = await response;
  expect(received.status()).toBe(200);
  return (await received.json()).result;
}

test('shows the citation from the approved database and clears it when scope changes', async ({ page }) => {
  const hotels = await openKnowledge(page);
  await hotels.selectOption('4');
  const result = await ask(page, 'Vinpearl Resort & Spa Hạ Long nằm trên đảo nào?');
  expect(result.status).toBe('GROUNDED_ANSWER');
  expect(result.citations).toEqual(expect.arrayContaining([
    expect.objectContaining({ sourceId: 'VP-HL-FACILITIES', hotelId: 4, version: 1 }),
  ]));
  await expect(page.getByText(/đảo Rều/i).first()).toBeVisible();
  await expect(page.getByLabel('Nguồn tham khảo')).toBeVisible();
  await hotels.selectOption('');
  await expect(page.getByLabel('Nguồn tham khảo')).toHaveCount(0);
  const unscoped = await ask(page, 'Khách sạn có nhà hàng Accogliente không?');
  expect(['INSUFFICIENT_EVIDENCE', 'SCOPE_REQUIRED']).toContain(unscoped.status);
  expect(unscoped.citations).toEqual([]);
});

test('routes a live price question away from the policy corpus in the booking portal', async ({ page }) => {
  const hotels = await openKnowledge(page, '/portal/search');
  await hotels.selectOption('4');
  const result = await ask(page, 'Đêm mai Deluxe giá bao nhiêu?');
  expect(result.status).toBe('OUT_OF_SCOPE');
  expect(result.citations).toEqual([]);
  await expect(page.getByLabel('Tra cứu chính sách khách sạn').getByRole('status')).toContainText(/Core|tìm phòng/i);
});

test('returns verified cards sourced from the pilot database', async ({ request }) => {
  const response = await request.post(`${api}/api/v1/public/ai/room-advisor/search`, { data: {
    contractVersion: '2.0',
    criteria: { hotelId: 4, checkInDate: date(2), checkOutDate: date(4), adults: 2, roomsCount: 1 },
  } });
  expect(response.status()).toBe(200);
  const result = (await response.json()).result;
  expect(result.status).toBe('RESULTS');
  expect(result.coverage.status).toBe('COMPLETE');
  expect(result.recommendations).toHaveLength(4);
  expect(result.recommendations.map((item: { room: { roomTypeCode: string } }) => item.room.roomTypeCode)).toEqual([
    'HL-DLX-K', 'HL-DLX-O', 'HL-EXS', 'HL-FAM',
  ]);
  for (const recommendation of result.recommendations) {
    expect(recommendation.room.hotelId).toBe(4);
    expect(recommendation.price).toEqual(expect.objectContaining({
      source: 'BACKEND_PRICING', currency: 'VND', serviceFeeStatus: 'INCLUDED', totalNights: 2,
    }));
    expect(Number(recommendation.price.finalTotalAmount)).toBeGreaterThan(0);
    expect(recommendation.action).toEqual(expect.objectContaining({ type: 'VIEW_ROOM_DETAILS' }));
    expect(recommendation.action.path).toMatch(/^\/portal\/hotels\/4\/room-types\/\d+$/);
  }
});

async function openRoomChat(page: Page) {
  await page.goto('/t12-pilot.html');
  await page.getByRole('button', { name: 'Mở Trợ lý Tìm phòng AI' }).click();
  return async (text: string) => {
    await page.getByPlaceholder(/Nhập nhu cầu/).fill(text);
    const response = page.waitForResponse(r => r.url() === `${api}/api/v1/public/ai/room-advisor/chat`);
    await page.getByLabel('Gửi tin nhắn').click();
    const received = await response;
    expect(received.status()).toBe(200);
    const result = (await received.json()).result;
    await expect(page.getByText(result.reply, { exact: true }).last()).toBeVisible();
    return result;
  };
}

test('pins a complete first-use prompt and fills the input without auto-sending', async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 812 });
  let chatRequests = 0;
  page.on('request', request => {
    if (request.url() === `${api}/api/v1/public/ai/room-advisor/chat`) chatRequests++;
  });

  await page.goto('/t12-pilot.html');
  await page.getByRole('button', { name: 'Mở Trợ lý Tìm phòng AI' }).click();
  await expect(page.getByRole('region', { name: 'Gợi ý yêu cầu tìm phòng' })).toBeVisible();
  await expect(page.getByLabel('Thông tin nên cung cấp')).toContainText('Địa điểm');
  await expect(page.getByLabel('Thông tin nên cung cấp')).toContainText('Ngày nhận & trả');
  await expect(page.getByLabel('Thông tin nên cung cấp')).toContainText('Số khách');
  await expect(page.getByLabel('Thông tin nên cung cấp')).toContainText('Ngân sách');

  await page.getByRole('button', { name: 'Điền yêu cầu mẫu đầy đủ' }).click();
  const input = page.getByLabel('Nhu cầu tìm phòng');
  await expect(input).toHaveValue(new RegExp(
    `Tìm phòng ở Hạ Long từ ${date(2)} đến ${date(4)}.*7\\.000\\.000 VND cả kỳ`,
  ));
  expect(chatRequests).toBe(0);

  const response = page.waitForResponse(request =>
    request.url() === `${api}/api/v1/public/ai/room-advisor/chat`);
  await page.getByLabel('Gửi tin nhắn').click();
  expect((await response).status()).toBe(200);
  expect(chatRequests).toBe(1);
  await expect(page.getByRole('region', { name: 'Gợi ý yêu cầu tìm phòng' })).toHaveCount(0);
  await expect(page.getByText('Deluxe King hướng vườn').first()).toBeVisible();
});

test('keeps the anonymous chat session while showing database-backed cards', async ({ page }) => {
  const send = await openRoomChat(page);
  const first = await send(`Tìm phòng ở Hạ Long từ ${date(2)} đến ${date(4)} cho 2 người lớn`);
  expect(first.status).toBe('RESULTS');
  expect(first.recommendations).toHaveLength(4);
  await expect(page.getByText('Deluxe King hướng vườn').first()).toBeVisible();
  await expect(page.getByText('Giá ước tính từ Core').first()).toBeVisible();
  await expect(page.getByRole('button', { name: /Xem chi tiết phòng Deluxe King hướng vườn/ }).first()).toBeVisible();

  const second = await send('Đổi thành 1 người lớn');
  expect(second.status).toBe('RESULTS');
  expect(second.recommendations).toHaveLength(4);
  expect(second.criteria.adults).toBe(1);
  expect(second.sessionId).toBe(first.sessionId);
  expect(second.sessionVersion).toBeGreaterThan(first.sessionVersion);
});

test('opens the selected database-backed room details and requotes through Core', async ({ page }) => {
  const send = await openRoomChat(page);
  const result = await send(`Tìm phòng ở Hạ Long từ ${date(2)} đến ${date(4)} cho 2 người lớn`);
  expect(result.status).toBe('RESULTS');

  const selected = result.recommendations[0];
  const detailResponse = page.waitForResponse(response =>
    response.url() === `${api}/api/v1/public/booking-search/quote` && response.request().method() === 'POST');
  await page.getByRole('button', { name: /Xem chi tiết phòng Deluxe King hướng vườn/ }).first().click();
  await expect(page).toHaveURL(new RegExp(`/portal/hotels/4/room-types/${selected.room.hotelRoomTypeId}\\?`));
  expect((await detailResponse).status()).toBe(200);
  await expect(page.getByRole('heading', { name: 'Deluxe King hướng vườn' })).toBeVisible();
  await expect(page.getByText(/6\.622\.560/)).toBeVisible();
});

test('filters database-backed cards with a hard whole-stay budget', async ({ page }) => {
  const send = await openRoomChat(page);
  const result = await send(
    `Tìm phòng ở Hạ Long từ ${date(2)} đến ${date(4)} cho 2 người lớn, `
      + 'ngân sách tối đa 7.000.000 VND cả kỳ theo báo giá hệ thống',
  );

  expect(result.status).toBe('RESULTS');
  expect(result.criteria.budget).toEqual({
    amount: '7000000', currency: 'VND', basis: 'STAY_TOTAL',
    coverage: 'BACKEND_QUOTED_TOTAL', hard: true,
  });
  expect(result.recommendations).toHaveLength(1);
  expect(result.recommendations[0].room.roomTypeCode).toBe('HL-DLX-K');
  expect(result.recommendations[0].price.finalTotalAmount).toBe('6622560.00');
  await expect(page.getByText('Deluxe King hướng vườn').first()).toBeVisible();
  await expect(page.getByText(/6\.622\.560/).first()).toBeVisible();
  await expect(page.getByText('Deluxe Twin hướng biển')).toHaveCount(0);
});

test('asks for checkout and applies budget clarification without losing the amount or stay', async ({ page }) => {
  const send = await openRoomChat(page);
  const first = await send(`Tìm phòng ở Hạ Long, nhận phòng ${date(2)} cho 2 người lớn, ngân sách tối đa 1.500.000 VND mỗi đêm`);
  expect(first.status).toBe('NEED_INFO');
  expect(first.missingFields).toContain('checkOutDate');
  expect(first.criteria.checkOutDate).toBeFalsy();
  expect(first.reply).toContain('ngày trả phòng');
  expect(first.recommendations).toEqual([]);
  expect(first.warnings).toEqual([]);
  await expect(page.getByText(/SERVICE_FEE_UNVERIFIED|CURRENCY_SYNTHETIC_ASSUMPTION/)).toHaveCount(0);

  const checkout = await send(`Trả phòng ${date(4)}`);
  expect(checkout.status).toBe('NEED_INFO');
  expect(checkout.criteria.checkInDate).toBe(date(2));
  expect(checkout.criteria.checkOutDate).toBe(date(4));
  expect(checkout.reply).toContain('trung bình mỗi đêm');
  expect(checkout.recommendations).toEqual([]);
  expect(checkout.warnings).toEqual([]);

  const budget = await send('Trung bình mỗi đêm theo báo giá hệ thống');
  expect(budget.status).toBe('NO_MATCH');
  expect(budget.sessionId).toBe(first.sessionId);
  expect(budget.criteria.budget).toEqual(expect.objectContaining({
    amount: '1500000', currency: 'VND', basis: 'AVERAGE_PER_NIGHT',
    coverage: 'BACKEND_QUOTED_TOTAL', hard: true,
  }));
  expect(budget.criteria.checkOutDate).toBe(date(4));
  expect(budget.recommendations).toEqual([]);
  expect(budget.warnings).toEqual([]);
});

test('refuses a room hold without breaking the existing room search session', async ({ page }) => {
  const send = await openRoomChat(page);
  const search = await send(`Tìm phòng ở Hạ Long từ ${date(2)} đến ${date(4)} cho 2 người lớn`);
  expect(search.status).toBe('RESULTS');
  expect(search.recommendations).toHaveLength(4);

  const denied = await send('Giữ phòng này giúp tôi');
  expect(denied.status).toBe('OUT_OF_SCOPE');
  expect(denied.recommendations).toEqual([]);
  expect(denied.coverage.status).toBe('NOT_RUN');
  expect(denied.sessionId).toBe(search.sessionId);
  expect(denied.sessionVersion).toBe(search.sessionVersion);
  expect(denied.warnings).toEqual([]);

  const followUp = await send('Đổi thành 1 người lớn');
  expect(followUp.status).toBe('RESULTS');
  expect(followUp.recommendations).toHaveLength(4);
  expect(followUp.sessionId).toBe(search.sessionId);
  expect(followUp.criteria.adults).toBe(1);
  expect(followUp.criteria.checkOutDate).toBe(date(4));
});
