import { test, expect } from '@playwright/test';
const field = (value: unknown, confidence = 0.9) => ({ value, confidence, valid: value !== null });
const draft = {
  fullName: field('NGUYEN VAN TEST'), dateOfBirth: field('1990-06-15'), gender: field('MALE'),
  nationality: field('Việt Nam'), documentType: field('CCCD'), documentNumber: field('079190012345'),
  permanentAddress: field('123 Test', 0.6), status: 'LOW_CONFIDENCE_REVIEW_REQUIRED',
  requiresHumanVerification: true, warnings: ['Kiểm tra địa chỉ'], missingOrLowConfidenceFields: ['permanentAddress'],
};
const upload = { name: 'synthetic.png', mimeType: 'image/png', buffer: Buffer.from([137, 80, 78, 71]) };

test('seven-field draft requires human review and explicit submit', async ({ page }) => {
  const writes: Record<string, unknown>[] = [];
  await page.route('**/api/v1/**', async route => {
    const req = route.request();
    if (req.url().endsWith('/ai/ocr/extract-file')) {
      expect(req.headers()['content-type']).toContain('multipart/form-data; boundary=');
      expect(req.postDataBuffer()?.toString()).not.toContain('name="rawOcrText"');
      await route.fulfill({ json: { code: 200, result: draft } });
    } else if (req.method() === 'POST') {
      writes.push(req.postDataJSON());
      await route.fulfill({ json: { code: 200, result: { id: 1 } } });
    } else await route.fulfill({ json: { code: 200, result: [] } });
  });
  await page.goto('/e2e/fixtures/ocr-review.html');
  await page.getByRole('button', { name: 'Thêm khách lưu trú' }).click();
  await page.getByLabel('Ảnh giấy tờ').setInputFiles(upload);
  await expect(page.getByText('Bản nháp OCR', { exact: false })).toBeVisible();
  await expect(page.getByPlaceholder('NGUYỄN VĂN A')).toHaveValue('NGUYEN VAN TEST');
  await expect(page.locator('input[type="date"]')).toHaveValue('1990-06-15');
  await expect(page.locator('select').nth(0)).toHaveValue('MALE');
  await expect(page.locator('select').nth(1)).toHaveValue('CCCD');
  await expect(page.getByPlaceholder('001200001234')).toHaveValue('079190012345');
  await expect(page.getByPlaceholder('Việt Nam')).toHaveValue('Việt Nam');
  await expect(page.getByPlaceholder('Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành')).toHaveValue('123 Test');
  await expect(page.getByText('Địa chỉ thường trú: 60%')).toBeVisible();
  expect(writes).toHaveLength(0);
  await expect(page.getByRole('button', { name: 'Lưu khách lưu trú' })).toBeDisabled();
  await page.getByPlaceholder('NGUYỄN VĂN A').fill('NGUYEN VAN EDIT');
  await page.getByLabel('Giờ nhận phòng', { exact: true }).fill('2026-10-28T14:00');
  await page.getByLabel('Giờ dự kiến trả phòng').fill('2026-10-29T12:00');
  await page.getByRole('checkbox').check();
  expect(writes).toHaveLength(0);
  await page.getByRole('button', { name: 'Lưu khách lưu trú' }).click();
  await expect.poll(() => writes.length).toBe(1);
  expect(writes[0]).toMatchObject({ fullName: 'NGUYEN VAN EDIT', documentNumber: '079190012345', gender: 'MALE', dateOfBirth: '1990-06-15', documentType: 'CCCD', nationality: 'Việt Nam', permanentAddress: '123 Test' });
  expect(writes[0]).not.toHaveProperty('confidence');
  expect(writes[0]).not.toHaveProperty('requiresHumanVerification');
  expect(Date.parse(String(writes[0].expectedCheckOutTime))).toBeGreaterThan(Date.parse(String(writes[0].checkInTime)));
});

for (const failure of ['503', 'unreadable', 'invalid-shape']) {
  test(`OCR ${failure} preserves manual input without writes`, async ({ page }) => {
    let writes = 0;
    await page.route('**/api/v1/**', async route => {
      if (route.request().url().endsWith('/ai/ocr/extract-file')) {
        await route.fulfill(failure === '503' ? { status: 503, json: { code: 503 } } : {
          json: { code: 200, result: failure === 'unreadable' ? { ...draft, status: 'UNREADABLE_DOCUMENT' } : {} },
        });
      } else { if (route.request().method() === 'POST') writes++; await route.fulfill({ json: { code: 200, result: [] } }); }
    });
    await page.goto('/e2e/fixtures/ocr-review.html');
    await page.getByRole('button', { name: 'Thêm khách lưu trú' }).click();
    await page.getByPlaceholder('NGUYỄN VĂN A').fill('MANUAL TEST');
    await page.getByLabel('Ảnh giấy tờ').setInputFiles(upload);
    await expect(page.getByText(/Không thể đọc ảnh|Không đọc được giấy tờ/)).toBeVisible();
    await expect(page.getByPlaceholder('NGUYỄN VĂN A')).toHaveValue('MANUAL TEST');
    expect(writes).toBe(0);
  });
}

test('cancel discards an in-flight OCR draft and clears identity fields', async ({ page }) => {
  let release: () => void = () => {};
  const pending = new Promise<void>(resolve => { release = resolve; });
  await page.route('**/api/v1/**', async route => {
    if (route.request().url().endsWith('/ai/ocr/extract-file')) {
      await pending;
      await route.fulfill({ json: { code: 200, result: draft } }).catch(() => {});
    } else await route.fulfill({ json: { code: 200, result: [] } });
  });
  await page.goto('/e2e/fixtures/ocr-review.html');
  await page.getByRole('button', { name: 'Thêm khách lưu trú' }).click();
  await page.getByLabel('Ảnh giấy tờ').setInputFiles(upload);
  await expect(page.getByText('Đang đọc ảnh...')).toBeVisible();
  await page.getByRole('button', { name: 'Hủy bỏ', exact: true }).click();
  release();
  await page.getByRole('button', { name: 'Thêm khách lưu trú' }).click();
  await expect(page.getByPlaceholder('NGUYỄN VĂN A')).toHaveValue('');
  await expect(page.getByRole('checkbox')).toHaveCount(0);
  await expect(page.getByText('Bản nháp OCR', { exact: false })).toHaveCount(0);
});
