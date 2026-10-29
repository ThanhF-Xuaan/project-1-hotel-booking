import { test, expect, type Page } from '@playwright/test';
const answer = (hotelId = 1) => ({ status: 'GROUNDED_ANSWER', answer: 'Synthetic nhận phòng lúc 14:00.',
  citations: [{ sourceId: 'SYNTHETIC POLICY', version: 1, chunkId: '00000000-0000-4000-8000-000000000001', category: 'POLICY', hotelId, snippet: '<img src=x onerror=alert(1)>', similarityScore: 0.9 }], warnings: [], disclaimers: ['Synthetic fixture'] });
async function open(page: Page) {
  await page.goto('/e2e/fixtures/t12-knowledge.html');
  await page.getByRole('button', { name: 'Mở Trợ lý Tìm phòng AI' }).click();
  await page.getByRole('button', { name: 'Chính sách & tiện ích' }).click();
  await expect(page.getByLabel('Khách sạn tra cứu').locator('option')).toHaveCount(3);
  await page.getByLabel('Khách sạn tra cứu').selectOption('1');
}
test.beforeEach(async ({ page }) => {
  await page.route('**/api/v1/public/hotels/filter', route => route.fulfill({ json: { result: { content: [
    { id: 1, name: 'Synthetic H1', status: 'ACTIVE' }, { id: 2, name: 'Synthetic H2', status: 'ACTIVE' },
  ] } } }));
});
test('shows citations as text and clears all FAQ state on hotel change', async ({ page }) => {
  await page.route('**/knowledge/ask', route => route.fulfill({ json: { result: answer() } }));
  await open(page);
  await page.getByLabel('Câu hỏi chính sách').fill('Giờ nhận phòng?');
  await page.getByRole('button', { name: 'Tra cứu chính sách', exact: true }).click();
  await expect(page.getByText('Synthetic nhận phòng lúc 14:00.')).toBeVisible();
  await expect(page.getByText('<img src=x onerror=alert(1)>', { exact: true })).toBeVisible();
  await expect(page.getByLabel('Nguồn tham khảo').locator('img')).toHaveCount(0);
  await page.getByLabel('Khách sạn tra cứu').selectOption('2');
  await expect(page.getByLabel('Nguồn tham khảo')).toHaveCount(0);
  await expect(page.getByLabel('Câu hỏi chính sách')).toHaveValue('');
});
test('does not render a delayed answer from the previous hotel', async ({ page }) => {
  let release: () => void = () => {};
  const pending = new Promise<void>(resolve => { release = resolve; });
  await page.route('**/knowledge/ask', async route => { await pending; await route.fulfill({ json: { result: answer() } }).catch(() => {}); });
  await open(page);
  await page.getByLabel('Câu hỏi chính sách').fill('Giờ nhận phòng?');
  await page.getByRole('button', { name: 'Tra cứu chính sách', exact: true }).click();
  await expect(page.getByText('Đang tra cứu...')).toBeVisible();
  await page.getByLabel('Khách sạn tra cứu').selectOption('2'); release();
  await expect(page.getByLabel('Khách sạn tra cứu')).toHaveValue('2');
  await expect(page.getByText('Synthetic nhận phòng lúc 14:00.')).toHaveCount(0);
});
test('rejects a citation belonging to another hotel', async ({ page }) => {
  await page.route('**/knowledge/ask', route => route.fulfill({ json: { result: answer(2) } }));
  await open(page);
  await page.getByLabel('Câu hỏi chính sách').fill('Giờ nhận phòng?');
  await page.getByRole('button', { name: 'Tra cứu chính sách', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('tạm thời không khả dụng');
  await expect(page.getByText('Synthetic nhận phòng lúc 14:00.')).toHaveCount(0);
});
test('FAQ 503 does not block R1 chat', async ({ page }) => {
  await page.route('**/knowledge/ask', route => route.fulfill({ status: 503, json: { message: 'PRIVATE_ERROR' } }));
  await page.route('**/room-advisor/chat', route => route.fulfill({ json: { result: { contractVersion: '2.0', status: 'NEED_INFO', reply: 'R1 vẫn hoạt động', recommendations: [] } } }));
  await open(page);
  await page.getByLabel('Câu hỏi chính sách').fill('Giờ nhận phòng?');
  await page.getByRole('button', { name: 'Tra cứu chính sách', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('tạm thời không khả dụng');
  await expect(page.getByText('PRIVATE_ERROR')).toHaveCount(0);
  await page.getByRole('button', { name: 'Tìm phòng', exact: true }).click();
  await page.getByPlaceholder(/Nhập nhu cầu/).fill('Tìm phòng');
  await page.getByLabel('Gửi tin nhắn').click();
  await expect(page.getByText('R1 vẫn hoạt động')).toBeVisible();
});
