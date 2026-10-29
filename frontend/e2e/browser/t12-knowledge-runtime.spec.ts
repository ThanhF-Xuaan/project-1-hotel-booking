import { test, expect } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { getKeycloakToken, BACKEND_URL } from '../helpers/auth.helper';
test.skip(process.env.T12_FULL_RUNTIME !== 'true', 'Explicit synthetic sandbox only');
function databaseDigest() {
  const pg = execFileSync('docker', ['ps', '--filter', 'label=com.docker.compose.project=hotel-ocr02-final', '--filter', 'label=com.docker.compose.service=acceptance_postgres', '--format', '{{.ID}}'], { encoding: 'utf8' }).trim();
  if (!pg) throw new Error('Synthetic sandbox required');
  const dump = execFileSync('docker', ['exec', pg, 'pg_dump', '--data-only', '--no-owner', '--no-privileges', '-U', 'postgres', '-d', 'hotel_acceptance_db'], { encoding: 'utf8', maxBuffer: 32 * 1024 * 1024 });
  const slash = String.fromCharCode(92);
  const normalized = dump.split(String.fromCharCode(10)).filter(line => !line.startsWith(slash + 'restrict ') && !line.startsWith(slash + 'unrestrict ')).join(String.fromCharCode(10));
  return createHash('sha256').update(normalized).digest('hex');
}
let beforeDigest: string;
test.beforeAll(() => { beforeDigest = databaseDigest(); });
test.afterAll(() => { expect(databaseDigest()).toBe(beforeDigest); });
test('real portal widget retrieves scoped citations, rejects inactive scope and keeps R1 available', async ({ page, request }) => {
  const token = await getKeycloakToken('admin');
  const sourceId = `synthetic-widget-${Date.now()}`;
  const scope = { sourceId, version: 1, scopeType: 'HOTEL_SCOPED', hotelId: 1 };
  const stage = await request.post(`${BACKEND_URL}/api/v1/ai/admin/knowledge/stage?version=1`, { headers: { Authorization: `Bearer ${token}` }, data: {
    ...scope, category: 'POLICY', title: 'Synthetic check-in policy', content: 'Quy định nhận phòng từ 14:00 và trả phòng trước 12:00 tại khách sạn synthetic.', effectiveFrom: '2026-01-01', effectiveTo: '2027-01-01',
  } });
  expect(stage.status()).toBe(200);
  expect((await request.post(`${BACKEND_URL}/api/v1/ai/admin/knowledge/activate`, { headers: { Authorization: `Bearer ${token}` }, data: scope })).status()).toBe(200);
  await page.goto('/portal/search');
  await page.getByRole('button', { name: 'Mở Trợ lý Tìm phòng AI' }).click();
  await page.getByRole('button', { name: 'Chính sách & tiện ích' }).click();
  await expect(page.getByLabel('Khách sạn tra cứu').locator('option[value="1"]')).toHaveCount(1);
  await page.getByLabel('Khách sạn tra cứu').selectOption('1');
  await page.getByLabel('Câu hỏi chính sách').fill('Quy định nhận phòng và trả phòng?');
  const query = page.waitForResponse(response => response.url().endsWith('/knowledge/ask'));
  await page.getByRole('button', { name: 'Tra cứu chính sách', exact: true }).click();
  const response = await query; expect(response.status()).toBe(200);
  const result = (await response.json()).result;
  expect(result.status).toBe('GROUNDED_ANSWER');
  expect(result.citations.every((citation: { hotelId?: number }) => citation.hotelId == null || citation.hotelId === 1)).toBe(true);
  await expect(page.getByLabel('Nguồn tham khảo')).toBeVisible();
  await page.getByLabel('Khách sạn tra cứu').selectOption('2');
  await expect(page.getByLabel('Nguồn tham khảo')).toHaveCount(0);
  const invalidScope = await request.post(`${BACKEND_URL}/api/v1/public/ai/room-advisor/knowledge/ask`, { data: { hotelId: 32767, question: 'Quy định nhận phòng?', topK: 5 } });
  expect((await invalidScope.json()).result.status).toBe('SCOPE_REQUIRED');
  const otherHotel = await request.post(`${BACKEND_URL}/api/v1/public/ai/room-advisor/knowledge/ask`, { data: { hotelId: 2, question: 'Quy định nhận phòng?', topK: 5 } });
  expect((await otherHotel.json()).result.citations.every((citation: { hotelId?: number }) => citation.hotelId == null || citation.hotelId === 2)).toBe(true);
  await page.getByRole('button', { name: 'Tìm phòng', exact: true }).click();
  await page.getByPlaceholder(/Nhập nhu cầu/).fill('Xin chào');
  const chat = page.waitForResponse(response => response.url().endsWith('/room-advisor/chat'));
  await page.getByLabel('Gửi tin nhắn').click();
  expect((await chat).status()).toBe(200);
});

test('real R1 search returns verified rooms on missing inventory days without generating rows', async ({ request }) => {
  const countMissingRange = () => {
    const pg = execFileSync('docker', ['ps', '--filter', 'label=com.docker.compose.project=hotel-ocr02-final', '--filter', 'label=com.docker.compose.service=acceptance_postgres', '--format', '{{.ID}}'], { encoding: 'utf8' }).trim();
    if (!pg) throw new Error('Synthetic sandbox required');
    return execFileSync('docker', ['exec', pg, 'psql', '-U', 'postgres', '-d', 'hotel_acceptance_db', '-Atc',
      "SELECT count(*) FROM room_availability WHERE date BETWEEN DATE '2027-01-10' AND DATE '2027-01-11'"], { encoding: 'utf8' }).trim();
  };
  expect(countMissingRange(), 'Fixture must exercise absent inventory rows').toBe('0');
  const response = await request.post(`${BACKEND_URL}/api/v1/public/ai/room-advisor/search`, { data: {
    contractVersion: '2.0', criteria: { hotelId: 1, locationKeyword: 'Acceptance Luxury Hanoi (Hà Nội)',
      checkInDate: '2027-01-10', checkOutDate: '2027-01-12', adults: 2, children: 0,
      extraBeds: 0, roomsCount: 1, requiredFeatureCodes: [], preferredFeatureCodes: [] },
  } });
  expect(response.status()).toBe(200);
  const result = (await response.json()).result;
  expect(result.status).toBe('RESULTS');
  expect(result.recommendations.length).toBeGreaterThan(0);
  for (const card of result.recommendations) {
    expect(card.room.hotelId).toBe(1);
    expect(card.stay.checkInDate).toBe('2027-01-10');
    expect(card.stay.checkOutDate).toBe('2027-01-12');
    expect(card.action.type).toBe('VIEW_ROOM_DETAILS');
    expect(Number(card.price.finalTotalAmount)).toBeGreaterThan(0);
  }
  expect(countMissingRange(), 'R1 read must leave missing inventory rows absent').toBe('0');
});
