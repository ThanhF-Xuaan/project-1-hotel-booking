import { test, expect, type Page } from '@playwright/test';

const result = (reply: string, version = 1) => ({ contractVersion: '2.0', requestId: 'synthetic',
  sessionId: '00000000-0000-4000-8000-000000000001', sessionVersion: version,
  status: 'NEED_INFO', reply, recommendations: [] });

async function send(page: Page, message: string) {
  await page.getByPlaceholder(/Nhập nhu cầu/).fill(message);
  await page.getByLabel('Gửi tin nhắn').click();
}

test('reset during conflict resync cannot overwrite an in-flight new chat', async ({ page }) => {
  let releaseOld!: () => void;
  let resyncStarted!: () => void;
  let releaseNew!: () => void;
  const oldPending = new Promise<void>(resolve => { releaseOld = resolve; });
  const resyncReceived = new Promise<void>(resolve => { resyncStarted = resolve; });
  const newPending = new Promise<void>(resolve => { releaseNew = resolve; });
  let calls = 0;
  await page.route('**/room-advisor/chat', async route => {
    calls++;
    if (calls === 2) { await route.fulfill({ status: 409, json: {} }); return; }
    if (calls === 3) await newPending;
    await route.fulfill({ json: { result: result(calls === 1 ? 'SYNTHETIC_FIRST_REPLY' : 'SYNTHETIC_NEW_REPLY') } });
  });
  await page.route('**/room-advisor/refresh', async route => {
    resyncStarted(); await oldPending;
    await route.fulfill({ json: { result: result('SYNTHETIC_OLD_RESYNC', 99) } }).catch(() => {});
  });
  await established(page); await send(page, 'Synthetic conflict'); await resyncReceived;
  await page.getByLabel('Làm mới phiên', { exact: true }).click();
  await send(page, 'Synthetic new'); await expect.poll(() => calls).toBe(3);
  releaseOld();
  await expect(page.getByPlaceholder(/Nhập nhu cầu/)).toBeDisabled();
  releaseNew();
  await expect(page.getByText('SYNTHETIC_NEW_REPLY', { exact: true })).toBeVisible();
  await expect(page.getByText(/Đã tự động đồng bộ lại phiên/)).toHaveCount(0);
  await expect(page.getByPlaceholder(/Nhập nhu cầu/)).toBeEnabled();
});

test('closing and reopening retains the current conversation', async ({ page }) => {
  await page.route('**/room-advisor/chat', route => route.fulfill({ json: { result: result('SYNTHETIC_FIRST_REPLY') } }));
  await established(page);
  await page.getByLabel('Đóng Trợ lý').click();
  await page.getByLabel('Mở Trợ lý Tìm phòng AI').click();
  await expect(page.getByText('SYNTHETIC_FIRST_REPLY', { exact: true })).toBeVisible();
  await expect(page.getByLabel('Làm mới phiên', { exact: true })).toBeVisible();
});

test('reset ignores a pending explicit price refresh', async ({ page }) => {
  let release!: () => void;
  let started!: () => void;
  const pending = new Promise<void>(resolve => { release = resolve; });
  const received = new Promise<void>(resolve => { started = resolve; });
  const recommendation = { recommendationId: 'synthetic-room', rank: 1,
    room: { hotelName: 'SYNTHETIC_HOTEL', roomTypeName: 'SYNTHETIC_ROOM', roomTypeCode: 'SYN', maxAdults: 2, maxChildren: 0 },
    price: { totalNights: 1, finalTotalAmount: '100000', currency: 'VND' }, reasons: [] };
  await page.route('**/room-advisor/chat', route => route.fulfill({ json: {
    result: { ...result('SYNTHETIC_FIRST_REPLY'), recommendations: [recommendation] },
  } }));
  await page.route('**/room-advisor/refresh', async route => {
    started(); await pending;
    await route.fulfill({ json: { result: result('SYNTHETIC_STALE_REFRESH', 99) } }).catch(() => {});
  });
  await established(page);
  await page.getByRole('button', { name: 'Cập nhật giá' }).click(); await received;
  await page.getByLabel('Làm mới phiên', { exact: true }).click(); release();
  await expect(page.getByPlaceholder(/Nhập nhu cầu/)).toBeEnabled();
  await send(page, 'Synthetic new');
  await expect(page.getByText('SYNTHETIC_FIRST_REPLY', { exact: true })).toBeVisible();
  await expect(page.getByText('SYNTHETIC_STALE_REFRESH', { exact: true })).toHaveCount(0);
});
async function established(page: Page) {
  await page.goto('/e2e/fixtures/t12-knowledge.html');
  await page.getByRole('button', { name: 'Mở Trợ lý Tìm phòng AI' }).click();
  await send(page, 'Synthetic first');
  await expect(page.getByText('SYNTHETIC_FIRST_REPLY', { exact: true })).toBeVisible();
}

for (const status of [200, 429, 409]) {
  test(`reset discards delayed chat ${status} and starts an independent request`, async ({ page }) => {
    let release!: () => void;
    let started!: () => void;
    let completed!: () => void;
    const pending = new Promise<void>(resolve => { release = resolve; });
    const received = new Promise<void>(resolve => { started = resolve; });
    const settled = new Promise<void>(resolve => { completed = resolve; });
    const payloads: Record<string, unknown>[] = [];
    let refreshCalls = 0;
    await page.route('**/room-advisor/refresh', route => {
      refreshCalls++;
      return route.fulfill({ json: { result: result('SYNTHETIC_OLD_RESYNC', 99) } });
    });
    await page.route('**/room-advisor/chat', async route => {
      payloads.push(route.request().postDataJSON());
      if (payloads.length === 2) {
        started(); await pending;
        await route.fulfill({ status, headers: { 'Retry-After': '60' },
          json: status === 200 ? { result: result('SYNTHETIC_STALE_REPLY', 99) } : { message: 'SYNTHETIC_STALE_ERROR' } }).catch(() => {});
        completed(); return;
      }
      await route.fulfill({ json: { result: result(payloads.length === 1 ? 'SYNTHETIC_FIRST_REPLY' : 'SYNTHETIC_NEW_REPLY') } });
    });
    await established(page);
    await send(page, 'Synthetic pending'); await received;
    await page.getByLabel('Làm mới phiên', { exact: true }).click();
    await expect(page.getByPlaceholder(/Nhập nhu cầu/)).toBeEnabled();
    await send(page, 'Synthetic new');
    await expect(page.getByText('SYNTHETIC_NEW_REPLY', { exact: true })).toBeVisible();
    release(); await settled;
    // A subsequent request proves a late completion cannot replace the new session/version or cooldown.
    await send(page, 'Synthetic next');
    await expect.poll(() => payloads.length).toBe(4);
    expect(payloads[2].sessionId).toBeUndefined();
    expect(payloads[2].expectedSessionVersion).toBeUndefined();
    expect(payloads[3].expectedSessionVersion).toBe(1);
    expect(refreshCalls).toBe(0);
    await expect(page.getByText('SYNTHETIC_STALE_REPLY', { exact: true })).toHaveCount(0);
    await expect(page.getByText('SYNTHETIC_STALE_ERROR', { exact: true })).toHaveCount(0);
  });
}
