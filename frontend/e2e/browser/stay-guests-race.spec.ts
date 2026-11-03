import { test, expect } from '@playwright/test';

for (const transition of ['Switch room', 'Switch hotel', 'close/reopen']) {
  test(`late list response cannot cross ${transition}`, async ({ page }) => {
    let calls = 0;
    let release!: () => void;
    const pending = new Promise<void>(resolve => { release = resolve; });
    await page.route('**/api/v1/stay-guests/by-booking-room/*', async route => {
      const current = ++calls;
      if (current === 1) await pending;
      await route.fulfill({ json: { code: 200, result: [{ id: current, fullName: current === 1 ? 'STALE_SYNTHETIC_GUEST' : 'CURRENT_SYNTHETIC_GUEST' }] } }).catch(() => {});
    });
    await page.goto('/e2e/fixtures/stay-guests-race.html');
    await expect.poll(() => calls).toBe(1);
    if (transition === 'close/reopen') {
      await page.getByRole('button', { name: 'Toggle modal' }).click();
      await page.getByRole('button', { name: 'Toggle modal' }).click();
    } else await page.getByRole('button', { name: transition, exact: true }).click();
    await expect.poll(() => calls).toBe(2);
    await expect(page.getByText('CURRENT_SYNTHETIC_GUEST', { exact: true })).toBeVisible();
    release();
    // Marker proves the delayed handler has been released before checking settled UI.
    await page.waitForTimeout(150);
    await expect(page.getByText('STALE_SYNTHETIC_GUEST', { exact: true })).toHaveCount(0);
    await expect(page.getByText('CURRENT_SYNTHETIC_GUEST', { exact: true })).toBeVisible();
  });
}

test('late create completion cannot reset a new room form or reload its list', async ({ page }) => {
  let reads = 0; let writes = 0; let release!: () => void;
  const pending = new Promise<void>(resolve => { release = resolve; });
  await page.route('**/api/v1/stay-guests/**', async route => {
    if (route.request().method() === 'POST') { writes++; await pending; }
    else reads++;
    await route.fulfill({ json: { code: 200, result: [] } }).catch(() => {});
  });
  await page.goto('/e2e/fixtures/stay-guests-race.html');
  await expect.poll(() => reads).toBe(1);
  await page.getByRole('button', { name: 'Thêm khách lưu trú' }).click();
  await page.getByPlaceholder('NGUYỄN VĂN A').fill('OLD_SYNTHETIC');
  await page.getByPlaceholder('001200001234').fill('SYNTHETIC_ONLY');
  await page.getByLabel('Giờ nhận phòng', { exact: true }).fill('2026-11-03T12:00');
  await page.getByLabel('Giờ dự kiến trả phòng').fill('2026-11-04T12:00');
  await page.getByRole('button', { name: 'Lưu khách lưu trú' }).click();
  await expect.poll(() => writes).toBe(1);
  await page.getByRole('button', { name: 'Switch room', exact: true }).click();
  await expect.poll(() => reads).toBe(2);
  await page.getByRole('button', { name: 'Thêm khách lưu trú' }).click();
  await page.getByPlaceholder('NGUYỄN VĂN A').fill('NEW_SYNTHETIC');
  release(); await page.waitForTimeout(150);
  await expect(page.getByPlaceholder('NGUYỄN VĂN A')).toHaveValue('NEW_SYNTHETIC');
  await expect(page.getByText('Khai báo khách lưu trú thành công!', { exact: true })).toHaveCount(0);
  expect(reads).toBe(2);
});

for (const failure of [false, true]) {
  test(`late delete ${failure ? 'failure' : 'success'} cannot affect a new room`, async ({ page }) => {
    let reads = 0; let deletes = 0; let release!: () => void;
    const pending = new Promise<void>(resolve => { release = resolve; });
    await page.route('**/api/v1/stay-guests/**', async route => {
      if (route.request().method() === 'DELETE') {
        deletes++; await pending;
        await route.fulfill({ status: failure ? 500 : 200, json: { code: failure ? 500 : 200 } }).catch(() => {});
      } else {
        reads++;
        await route.fulfill({ json: { code: 200, result: [{ id: reads, fullName: 'CURRENT_SYNTHETIC_GUEST' }] } });
      }
    });
    page.on('dialog', dialog => dialog.accept());
    await page.goto('/e2e/fixtures/stay-guests-race.html');
    await expect.poll(() => reads).toBe(1);
    await page.getByTitle('Xóa thông tin khách', { exact: true }).click();
    await expect.poll(() => deletes).toBe(1);
    await page.getByRole('button', { name: 'Switch room', exact: true }).click();
    await expect.poll(() => reads).toBe(2);
    release(); await page.waitForTimeout(150);
    await expect(page.getByText('CURRENT_SYNTHETIC_GUEST', { exact: true })).toBeVisible();
    await expect(page.getByText(/Đã xóa thông tin|Không thể xóa khách/)).toHaveCount(0);
    expect(reads).toBe(2);
  });
}
