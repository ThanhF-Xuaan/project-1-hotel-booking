import { test, expect } from '@playwright/test';

test.describe('Room Details to Checkout Transition Flow', () => {
  test('should display proceed to booking button, guard checks, and navigate to checkout with active session timer', async ({ page }) => {
    page.on('console', msg => console.log('PW CONSOLE:', msg.text()));
    page.on('request', req => console.log('PW REQ:', req.method(), req.url()));
    page.on('response', async res => {
      if (res.url().includes('/api/')) {
        console.log('PW API RES:', res.status(), res.url());
      }
    });

    const baseUrl = process.env.VITE_BASE_URL || 'http://localhost:3000';

    // 1. Visit Room Details page for Deluxe King (Hotel 4, RoomType 2)
    const detailsUrl = `${baseUrl}/portal/hotels/4/room-types/2?checkInDate=2026-11-10&checkOutDate=2026-11-12&adults=2&roomsCount=1`;
    console.log('Navigating to:', detailsUrl);
    await page.goto(detailsUrl, { waitUntil: 'domcontentloaded' });

    // 2. Wait for quote and proceed button to load
    const proceedBtn = page.getByRole('button', { name: 'Tiến hành đặt phòng' });
    await expect(proceedBtn).toBeVisible({ timeout: 15000 });
    await expect(proceedBtn).toBeEnabled();

    // 3. Verify price quote and notice banner
    await expect(page.locator('text=6.622.560').first()).toBeVisible();
    await expect(page.locator('text=kích hoạt phiên đặt phòng 15 phút')).toBeVisible();

    // 4. Click "Tiến hành đặt phòng"
    await proceedBtn.click();

    // 5. Verify transition to Checkout page
    await page.waitForURL('**/portal/checkout', { timeout: 10000 });
    expect(page.url()).toContain('/portal/checkout');

    // 6. Verify 15-minute Session Timeout countdown timer
    await expect(page.locator('text=Phòng đang được khóa giữ chỗ độc quyền')).toBeVisible({ timeout: 10000 });

    // 7. Verify grand total matches Core Quote
    await expect(page.locator('text=6.622.560').first()).toBeVisible();

    // 8. Verify guest form inputs are ready
    await expect(page.getByPlaceholder('VD: Nguyễn Văn')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Xác Nhận Đặt Phòng' })).toBeVisible();
  });
});
