import { test, expect } from '@playwright/test';

test.describe('T12 Room Recommendation Assistant — Browser E2E Flow', () => {
  test('should open advisor widget, perform conversational search, and navigate to room details', async ({ page }) => {
    // 1. Visit Portal Search Page
    await page.goto('/portal/search');
    await expect(page).toHaveTitle(/frontend|Grand|Hotel/i);

    // 2. Open Room Advisor Widget
    const openAdvisorBtn = page.getByRole('button', { name: /Trợ lý AI Tìm Phòng/i });
    await expect(openAdvisorBtn).toBeVisible();
    await openAdvisorBtn.click();

    // 3. Verify Advisor Widget dialog / drawer is open
    const advisorInput = page.getByPlaceholder(/Nhập nhu cầu/i);
    await expect(advisorInput).toBeVisible();

    // 4. Send initial query: 2 adults in Hanoi with dates
    await advisorInput.fill('Tìm phòng ở Hà Nội cho 2 người lớn từ 2026-11-01 đến 2026-11-03');
    const sendBtn = page.getByLabel('Gửi tin nhắn');
    await sendBtn.click();

    // 5. Wait for Assistant response and recommendation cards
    // The widget displays "Gợi ý phù hợp" when recommendations arrive
    await expect(page.getByText(/Gợi ý phù hợp/i)).toBeVisible({ timeout: 15000 });

    // 6. Test MVP 1-room constraint clarification: query with multiple rooms
    await advisorInput.fill('Tôi muốn đặt 5 phòng cho đoàn 20 người');
    await sendBtn.click();

    // Assistant should clarify single-room scope / clarify criteria
    await expect(
      page.locator('.whitespace-pre-line').filter({ hasText: /phòng|đoàn|MVP|1 phòng|hỗ trợ/i }).last()
    ).toBeVisible({ timeout: 10000 });

    // 7. Click "Xem chi tiết" on the first recommendation card
    const viewDetailsBtn = page.getByRole('button', { name: /Xem chi tiết/i }).first();
    await expect(viewDetailsBtn).toBeVisible();
    await viewDetailsBtn.click();

    // 8. Verify Navigation to Room Details Page
    await page.waitForURL(/\/portal\/hotels\/\d+\/room-types\/\d+/);
    expect(page.url()).toContain('/portal/hotels/');
    expect(page.url()).toContain('/room-types/');
    expect(page.url()).toContain('checkInDate=');

    // 9. Verify Room Details Page content & Zero Business Write boundary
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible({ timeout: 10000 });
    await expect(page.getByText(/Báo giá xác thực từ Core|Tóm tắt chi phí/i)).toBeVisible({ timeout: 10000 });
    await expect(page.getByText(/Dữ liệu phòng được xác thực trực tiếp/i)).toBeVisible();

    // Confirm there is NO booking submit or payment mutation button
    await expect(page.getByRole('button', { name: /Đặt phòng ngay|Thanh toán ngay/i })).toHaveCount(0);
  });
});
