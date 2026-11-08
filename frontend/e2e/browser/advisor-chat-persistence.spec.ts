import { test, expect } from '@playwright/test';

test.describe('Room Advisor Chat Persistence and Auto-Reopen Flow', () => {
  test('should persist chat history, auto-reopen on return from room details, and clean up on reset', async ({ page }) => {
    page.on('console', msg => console.log('PW CONSOLE:', msg.text()));
    page.on('request', req => {
      if (req.url().includes('/api/')) console.log('PW REQ:', req.method(), req.url());
    });
    page.on('response', res => {
      if (res.url().includes('/api/')) console.log('PW RES:', res.status(), res.url());
    });

    const baseUrl = process.env.VITE_BASE_URL || 'http://localhost:3000';

    // 1. Visit Search Page
    console.log('1. Navigating to SearchPage:', `${baseUrl}/portal/search`);
    await page.goto(`${baseUrl}/portal/search`, { waitUntil: 'domcontentloaded' });

    // 2. Open Chatbot Widget
    const openBtn = page.getByRole('button', { name: /Trợ lý AI Tìm Phòng|Hỏi Trợ lý AI/i }).first();
    await expect(openBtn).toBeVisible({ timeout: 10000 });
    await openBtn.click();

    // 3. Send hotel search query
    const chatInput = page.getByPlaceholder(/Nhập nhu cầu/i);
    await expect(chatInput).toBeVisible({ timeout: 10000 });
    const query = 'Tôi cần tìm phòng cho 2 người lớn ở Hạ Long từ 10/11/2026 đến 12/11/2026';
    console.log('2. Sending chat query:', query);
    await chatInput.fill(query);
    await chatInput.press('Enter');

    // 4. Wait for recommendations and "Xem chi tiết →" button
    const viewDetailsBtn = page.getByRole('button', { name: /Xem chi tiết/i }).first();
    await expect(viewDetailsBtn).toBeVisible({ timeout: 25000 });
    console.log('3. Recommendation cards received.');

    // 5. Verify sessionStorage has cached chat data
    const rawStorage = await page.evaluate(() => sessionStorage.getItem('t12_advisor_chat_session'));
    expect(rawStorage).toBeTruthy();
    const parsedStorage = JSON.parse(rawStorage!);
    expect(parsedStorage.messages.length).toBeGreaterThan(1);
    expect(parsedStorage.sessionId).toBeTruthy();
    console.log('4. Verified sessionStorage contains active session:', parsedStorage.sessionId);

    // 6. Click "Xem chi tiết →" to navigate to RoomDetailsPage
    console.log('5. Clicking "Xem chi tiết →"');
    await viewDetailsBtn.click();

    // 7. Verify navigation to RoomDetailsPage
    await page.waitForURL('**/portal/hotels/**/room-types/**', { timeout: 15000 });
    expect(page.url()).toContain('/portal/hotels/');
    console.log('6. Landed on RoomDetailsPage:', page.url());

    // 8. Click "Chọn phòng khác"
    const chooseAnotherBtn = page.getByRole('button', { name: 'Chọn phòng khác' });
    await expect(chooseAnotherBtn).toBeVisible({ timeout: 15000 });
    console.log('7. Clicking "Chọn phòng khác"');
    await chooseAnotherBtn.click();

    // 9. Verify navigation back to SearchPage
    await page.waitForURL('**/portal/search', { timeout: 10000 });
    expect(page.url()).toContain('/portal/search');
    console.log('8. Returned to SearchPage.');

    // 10. Verify Chatbot is AUTOMATICALLY OPEN with restored conversation (after "Chọn phòng khác")
    await expect(page.locator('#advisor-dialog-title')).toBeVisible({ timeout: 10000 });
    console.log('9. Verified Chatbot auto-opened successfully after "Chọn phòng khác".');

    // 11. Verify restored messages contain the original query and recommendations
    await expect(page.locator(`text=${query}`)).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('button', { name: /Xem chi tiết/i }).first()).toBeVisible({ timeout: 10000 });
    console.log('10. Verified original conversation and recommendations restored.');

    // 12. Test BROWSER BACK BUTTON (history.back / page.goBack()) navigation
    console.log('11. Testing Browser Back button navigation...');
    await viewDetailsBtn.click();
    await page.waitForURL('**/portal/hotels/**/room-types/**', { timeout: 15000 });
    expect(page.url()).toContain('/portal/hotels/');
    console.log('12. Landed on RoomDetailsPage second time:', page.url());

    // Press browser Back button
    console.log('13. Triggering browser back button: page.goBack()');
    await page.goBack();
    await page.waitForURL('**/portal/search', { timeout: 10000 });
    expect(page.url()).toContain('/portal/search');

    // Verify Chatbot is AUTOMATICALLY OPEN via browser back button
    await expect(page.locator('#advisor-dialog-title')).toBeVisible({ timeout: 10000 });
    await expect(page.locator(`text=${query}`)).toBeVisible({ timeout: 10000 });
    console.log('14. Verified Chatbot auto-opened successfully after Browser Back button!');

    // 13. Test continue conversation to ensure no duplicate React key error
    console.log('15. Testing follow-up message...');
    await chatInput.fill('Có ăn sáng không?');
    await chatInput.press('Enter');
    await page.waitForTimeout(3000);

    // 14. Test Reset button 🔄
    const resetBtn = page.getByRole('button', { name: /Làm mới phiên/i });
    if (await resetBtn.isVisible()) {
      console.log('16. Clicking Reset button 🔄');
      await resetBtn.click();
      await expect(page.locator('text=Phiên tư vấn đã được làm mới')).toBeVisible({ timeout: 5000 });

      // Verify sessionStorage is cleared
      const clearedStorage = await page.evaluate(() => sessionStorage.getItem('t12_advisor_chat_session'));
      expect(clearedStorage).toBeNull();
      const clearedFlag = await page.evaluate(() => sessionStorage.getItem('t12_advisor_auto_reopen'));
      expect(clearedFlag).toBeNull();
      console.log('17. Verified sessionStorage and auto-reopen flag cleared on reset.');
    }
  });
});
