import { test, expect } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { getKeycloakToken, BACKEND_URL } from '../helpers/auth.helper';

test.skip(process.env.OCR_FULL_APP !== 'true', 'Explicit isolated OCR sandbox only');
function sql(query: string): string {
  const container = execFileSync('docker', ['ps', '--filter', 'label=com.docker.compose.project=hotel-ocr02-final', '--filter', 'label=com.docker.compose.service=acceptance_postgres', '--format', '{{.ID}}'], { encoding: 'utf8' }).trim();
  if (!container) throw new Error('Dedicated OCR sandbox is required');
  return execFileSync('docker', ['exec', container, 'psql', '-U', 'postgres', '-d', 'hotel_acceptance_db', '-v', 'ON_ERROR_STOP=1', '-Atc', query], { encoding: 'utf8' }).trim();
}

test('real OCR draft is editable and persists only after human verification and submit', async ({ page }) => {
  const token = await getKeycloakToken('reception_h1');
  await page.addInitScript(value => localStorage.setItem('auth_token', value), token);
  const before = sql('SELECT count(*) FROM stay_guests');
  await page.goto('/e2e/fixtures/ocr-review.html');
  await page.getByRole('button', { name: 'Thêm khách lưu trú' }).click();
  const extracted = page.waitForResponse(r => r.url().endsWith('/ai/ocr/extract-file'));
  await page.getByLabel('Ảnh giấy tờ').setInputFiles('../backend/src/test/resources/ai/ocr/cccd-synthetic.png');
  const response = await extracted;
  expect(response.status()).toBe(200);
  const draft = (await response.json()).result;
  expect(draft.requiresHumanVerification).toBe(true);
  await expect(page.getByPlaceholder('NGUYỄN VĂN A')).toHaveValue(draft.fullName.value);
  await expect(page.getByPlaceholder('001200001234')).toHaveValue(draft.documentNumber.value);
  await expect(page.locator('input[type="date"]')).toHaveValue(draft.dateOfBirth.value ?? '');
  await expect(page.locator('select').nth(0)).toHaveValue(draft.gender.value ?? 'OTHER');
  await expect(page.locator('select').nth(1)).toHaveValue(draft.documentType.value ?? 'OTHER');
  await expect(page.getByPlaceholder('Việt Nam')).toHaveValue(draft.nationality.value ?? '');
  await expect(page.getByPlaceholder('Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành')).toHaveValue(draft.permanentAddress.value ?? '');
  expect(sql('SELECT count(*) FROM stay_guests')).toBe(before);
  await expect(page.getByRole('button', { name: 'Lưu khách lưu trú' })).toBeDisabled();
  await page.getByPlaceholder('NGUYỄN VĂN A').fill('OCR SYNTHETIC REVIEWED');
  await page.getByLabel('Giờ nhận phòng', { exact: true }).fill('2026-10-29T14:00');
  await page.getByLabel('Giờ dự kiến trả phòng').fill('2026-10-30T12:00');
  await page.getByRole('checkbox').check();
  expect(sql('SELECT count(*) FROM stay_guests')).toBe(before);
  const created = page.waitForResponse(r => r.url().endsWith('/stay-guests/create'));
  await page.getByRole('button', { name: 'Lưu khách lưu trú' }).click();
  const saved = await created;
  expect(saved.status()).toBe(200);
  const id = Number((await saved.json()).result.id);
  expect(Number.isSafeInteger(id)).toBe(true);
  try {
    expect(sql(`SELECT full_name FROM stay_guests WHERE id=${id}`)).toBe('OCR SYNTHETIC REVIEWED');
    expect(sql(`SELECT document_number FROM stay_guests WHERE id=${id}`)).toBe(draft.documentNumber.value);
    expect(sql(`SELECT count(*) FROM lodging_queues WHERE stay_guest_id=${id}`)).toBe('1');
    expect(sql('SELECT count(*) FROM stay_guests')).toBe(String(Number(before) + 1));
  } finally {
    sql(`BEGIN; DELETE FROM lodging_queues WHERE stay_guest_id=${id}; DELETE FROM stay_guests WHERE id=${id}; COMMIT;`);
  }
});

test('production filter rejects missing role and revoked Keycloak token using real Redis', async ({ request }) => {
  const image = { fileName: 'synthetic.png', contentType: 'image/png', fileSizeBytes: 100, rawOcrText: 'synthetic' };
  const endpoint = `${BACKEND_URL}/api/v1/ai/ocr/extract`;
  expect((await request.post(endpoint, { data: image })).status()).toBe(401);
  const customer = await getKeycloakToken('test_customer');
  expect((await request.post(endpoint, { headers: { Authorization: `Bearer ${customer}` }, data: image })).status()).toBe(403);
  const token = await getKeycloakToken('reception_h1');
  const headers = { Authorization: `Bearer ${token}` };
  expect((await request.post(endpoint, { headers, data: image })).status()).toBe(200);
  expect((await request.post(`${BACKEND_URL}/api/v1/auth/logout`, { headers, data: {} })).status()).toBe(200);
  expect((await request.post(endpoint, { headers, data: image })).status()).toBe(401);
});
