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
for (const statusCode of [503, 429]) test(`FAQ ${statusCode} does not block R1 chat or retry`, async ({ page }) => {
  let calls = 0;
  await page.route('**/knowledge/ask', route => { calls++; return route.fulfill({ status: statusCode, headers: { 'Retry-After': '60', 'Access-Control-Expose-Headers': 'Retry-After' }, json: { message: 'PRIVATE_ERROR' } }); });
  await page.route('**/room-advisor/chat', route => route.fulfill({ json: { result: { contractVersion: '2.0', status: 'NEED_INFO', reply: 'R1 vẫn hoạt động', recommendations: [] } } }));
  await open(page);
  await page.getByLabel('Câu hỏi chính sách').fill('Giờ nhận phòng?');
  await page.getByRole('button', { name: 'Tra cứu chính sách', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText(statusCode === 429 ? 'thử lại sau 60 giây' : 'tạm thời không khả dụng');
  await expect(page.getByText('PRIVATE_ERROR')).toHaveCount(0);
  await page.getByRole('button', { name: 'Tìm phòng', exact: true }).click();
  await page.getByPlaceholder(/Nhập nhu cầu/).fill('Tìm phòng');
  await page.getByLabel('Gửi tin nhắn').click();
  await expect(page.getByText('R1 vẫn hoạt động')).toBeVisible();
  expect(calls).toBe(1);
});

test('Gemini fallback retains citations and renders answer as text', async ({ page }) => {
  await page.route('**/knowledge/ask', route => route.fulfill({ json: { result: { ...answer(),
    answer: '<script>window.__injected = true</script>', warnings: ['GENERATION_FALLBACK_EXTRACTIVE'] } } }));
  await open(page);
  await page.getByLabel('Câu hỏi chính sách').fill('Giờ nhận phòng?');
  await page.getByRole('button', { name: 'Tra cứu chính sách', exact: true }).click();
  await expect(page.getByText('Đang hiển thị trích đoạn tài liệu do dịch vụ AI tạm gián đoạn.')).toBeVisible();
  await expect(page.getByText('GENERATION_FALLBACK_EXTRACTIVE')).toHaveCount(0);
  await expect(page.getByLabel('Nguồn tham khảo')).toBeVisible();
  await expect(page.getByText('<script>window.__injected = true</script>', { exact: true })).toBeVisible();
  expect(await page.evaluate(() => '__injected' in window)).toBe(false);
});

test('a newer question supersedes the pending response without mixing turns', async ({ page }) => {
  let release: () => void = () => {};
  const pending = new Promise<void>(resolve => { release = resolve; });
  let calls = 0;
  await page.route('**/knowledge/ask', async route => {
    calls++;
    if (calls === 1) { await pending; await route.fulfill({ json: { result: answer() } }).catch(() => {}); }
    else await route.fulfill({ json: { result: { ...answer(), answer: 'Câu trả lời mới' } } });
  });
  await open(page);
  await page.getByLabel('Câu hỏi chính sách').fill('Câu hỏi đầu');
  await page.getByRole('button', { name: 'Tra cứu chính sách', exact: true }).click();
  await expect(page.getByText('Đang tra cứu...')).toBeVisible();
  await page.getByLabel('Câu hỏi chính sách').fill('Câu hỏi mới');
  await page.getByRole('button', { name: 'Tra cứu chính sách', exact: true }).click();
  await expect(page.getByText('Câu trả lời mới')).toBeVisible(); release();
  await expect(page.getByText('Synthetic nhận phòng lúc 14:00.')).toHaveCount(0);
  expect(calls).toBe(2);
});

test('invalid Retry-After is never displayed as server text', async ({ page }) => {
  await page.route('**/knowledge/ask', route => route.fulfill({ status: 429, headers: { 'Retry-After': '<img src=x>' }, json: { message: 'PRIVATE_ERROR' } }));
  await open(page);
  await page.getByLabel('Câu hỏi chính sách').fill('Giờ nhận phòng?');
  await page.getByRole('button', { name: 'Tra cứu chính sách', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('Đã vượt giới hạn tra cứu. Vui lòng thử lại sau.');
  await expect(page.getByText('<img src=x>', { exact: true })).toHaveCount(0);
});

test('demo label matches the explicitly selected build mode', async ({ page }) => {
  await open(page);
  await expect(page.getByText('Dữ liệu giả lập (synthetic), chỉ phục vụ demo.'))
    .toHaveCount(process.env.T12_EXPECT_SYNTHETIC_LABEL === 'true' ? 1 : 0);
});

test('unmount aborts a pending answer and reopening starts an empty turn', async ({ page }) => {
  let release: () => void = () => {};
  const pending = new Promise<void>(resolve => { release = resolve; });
  await page.route('**/knowledge/ask', async route => {
    await pending; await route.fulfill({ json: { result: answer() } }).catch(() => {});
  });
  await open(page);
  await page.getByLabel('Câu hỏi chính sách').fill('Giờ nhận phòng?');
  await page.getByRole('button', { name: 'Tra cứu chính sách', exact: true }).click();
  await expect(page.getByText('Đang tra cứu...')).toBeVisible();
  await page.getByRole('button', { name: 'Tìm phòng', exact: true }).click();
  release();
  await page.getByRole('button', { name: 'Chính sách & tiện ích' }).click();
  await expect(page.getByLabel('Câu hỏi chính sách')).toHaveValue('');
  await expect(page.getByLabel('Nguồn tham khảo')).toHaveCount(0);
  await expect(page.getByText('Synthetic nhận phòng lúc 14:00.')).toHaveCount(0);
});
