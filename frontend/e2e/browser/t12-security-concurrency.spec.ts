import { test, expect } from '@playwright/test';

test.describe('T12 Session Security, Concurrency & Rate Limiting', () => {
  test('should isolate anonymous sessions across independent browser contexts', async ({ browser }) => {
    // 1. Context A (User 1)
    const contextA = await browser.newContext();
    const pageA = await contextA.newPage();
    await pageA.goto('/portal/search');
    await pageA.getByRole('button', { name: /Trợ lý AI Tìm Phòng/i }).click();
    const inputA = pageA.getByPlaceholder(/Nhập nhu cầu/i);
    await inputA.fill('Tìm phòng cho 1 người lớn');

    const chatPromiseA = pageA.waitForResponse(
      (resp) => resp.url().includes('/api/v1/public/ai/room-advisor/chat') && resp.status() === 200
    );
    await pageA.getByLabel('Gửi tin nhắn').click();
    await chatPromiseA;

    const cookiesA = await contextA.cookies();
    const advisorCookieA = cookiesA.find((c) => c.name === 't12_session');
    expect(advisorCookieA).toBeDefined();

    // 2. Context B (User 2)
    const contextB = await browser.newContext();
    const pageB = await contextB.newPage();
    await pageB.goto('/portal/search');
    await pageB.getByRole('button', { name: /Trợ lý AI Tìm Phòng/i }).click();
    const inputB = pageB.getByPlaceholder(/Nhập nhu cầu/i);
    await inputB.fill('Tìm phòng hạng Suite cho gia đình');

    const chatPromiseB = pageB.waitForResponse(
      (resp) => resp.url().includes('/api/v1/public/ai/room-advisor/chat') && resp.status() === 200
    );
    await pageB.getByLabel('Gửi tin nhắn').click();
    await chatPromiseB;

    const cookiesB = await contextB.cookies();
    const advisorCookieB = cookiesB.find((c) => c.name === 't12_session');
    expect(advisorCookieB).toBeDefined();

    // 3. Confirm cookies are distinct and cryptographically isolated
    expect(advisorCookieA?.value).not.toBe(advisorCookieB?.value);

    await contextA.close();
    await contextB.close();
  });

  test('should enforce rate limiting and display cooldown countdown on rapid submission', async ({ page }) => {
    await page.goto('/portal/search');
    await page.getByRole('button', { name: /Trợ lý AI Tìm Phòng/i }).click();
    const input = page.getByPlaceholder(/Nhập nhu cầu/i);
    const sendBtn = page.getByLabel('Gửi tin nhắn');

    // Create one real anonymous session; subsequent invalid refreshes are rate-limited before lookup.
    const firstChat = page.waitForResponse(
      (response) => response.url().includes('/api/v1/public/ai/room-advisor/chat')
    );
    await input.fill('Tìm phòng cho 1 người lớn');
    await sendBtn.click();
    expect((await firstChat).status()).toBe(200);
    const sessionCookie = (await page.context().cookies()).find((cookie) => cookie.name === 't12_session');
    expect(sessionCookie).toBeDefined();
    if (!sessionCookie) throw new Error('Advisor session cookie was not issued.');

    const backendUrl = process.env.VITE_API_URL || 'http://localhost:18080';
    const refreshUrl = `${backendUrl}/api/v1/public/ai/room-advisor/refresh`;
    let retryAfter: number | undefined;
    for (let attempt = 0; attempt < 20; attempt++) {
      const response = await page.context().request.post(refreshUrl, {
        data: {
          contractVersion: '2.0',
          sessionId: '00000000-0000-4000-8000-000000000001',
        },
        headers: { Cookie: `t12_session=${sessionCookie.value}` },
      });
      if (response.status() === 429) {
        retryAfter = Number(response.headers()['retry-after']);
        break;
      }
      expect(response.status()).toBe(404);
    }

    if (retryAfter === undefined) {
      throw new Error('Backend did not return HTTP 429 after the anonymous request limit.');
    }
    expect(Number.isInteger(retryAfter) && retryAfter > 0).toBe(true);

    const browserRateLimit = page.waitForResponse(
      (response) => response.url().includes('/api/v1/public/ai/room-advisor/chat') && response.status() === 429
    );
    await input.fill('Yêu cầu tiếp theo trong lúc giới hạn đang hoạt động');
    await sendBtn.click();
    await browserRateLimit;

    const cooldownBanner = page.getByText(/Bạn đang gửi yêu cầu quá nhanh\. Vui lòng chờ:/i);
    await expect(cooldownBanner).toBeVisible();
    await expect(sendBtn).toBeDisabled();
  });
});
