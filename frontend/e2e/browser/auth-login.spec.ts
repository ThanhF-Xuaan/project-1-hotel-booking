import { test, expect } from '@playwright/test';

test.describe('Login form dependency compatibility', () => {
  test('shows required-field errors without sending invalid credentials', async ({ page }) => {
    let loginRequests = 0;
    page.on('request', (request) => {
      if (new URL(request.url()).pathname === '/api/v1/auth/login') {
        loginRequests++;
      }
    });

    await page.goto('/login');
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();

    await expect(page.getByText('Tên đăng nhập không được để trống', { exact: true })).toBeVisible();
    await expect(page.getByText('Mật khẩu không được để trống', { exact: true })).toBeVisible();
    expect(loginRequests).toBe(0);

    await page.getByLabel('Tên đăng nhập hoặc Email', { exact: true }).fill('manager_h1');
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();

    await expect(page.getByText('Tên đăng nhập không được để trống', { exact: true })).toBeHidden();
    await expect(page.getByText('Mật khẩu không được để trống', { exact: true })).toBeVisible();
    expect(loginRequests).toBe(0);
  });

  test('logs in through the real backend and keeps authorized access after reload', async ({ page }) => {
    const requestedScripts: string[] = [];
    page.on('request', (request) => {
      if (request.resourceType() === 'script') {
        requestedScripts.push(new URL(request.url()).pathname);
      }
    });

    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/login$/);
    await page.getByLabel('Tên đăng nhập hoặc Email', { exact: true }).fill('manager_h1');
    await page.getByLabel('Mật khẩu', { exact: true }).fill('Password123!');

    const loginResponse = page.waitForResponse((response) =>
      new URL(response.url()).pathname === '/api/v1/auth/login'
      && response.request().method() === 'POST'
    );
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
    expect((await loginResponse).status()).toBe(200);
    await expect(page).toHaveURL(/\/dashboard$/);

    const dashboardResponse = page.waitForResponse((response) =>
      new URL(response.url()).pathname === '/api/v1/dashboard/kpis'
      && response.request().method() === 'GET'
    );
    await page.reload();
    expect((await dashboardResponse).status()).toBe(200);
    await expect(page.getByRole('heading', { name: /Bảng Điều Hành Quản Trị Khách Sạn/ })).toBeVisible();
    expect(await page.evaluate(() => Boolean(localStorage.getItem('auth_token'))
      && Boolean(localStorage.getItem('auth_refresh_token')))).toBe(true);

    expect(requestedScripts.some((path) => /\/BookingListPage[-.]/.test(path))).toBe(false);
    await page.getByRole('link', { name: 'Danh sách Đặt phòng', exact: true }).click();
    await expect(page).toHaveURL(/\/booking\/list$/);
    await expect(page.getByRole('heading', { name: 'Quản lý Đặt phòng (Bookings)', exact: true })).toBeVisible();
    expect(requestedScripts.some((path) => /\/BookingListPage[-.]/.test(path))).toBe(true);

    await page.getByRole('link', { name: 'Bàn làm việc (Dashboard)', exact: true }).click();
    await expect(page).toHaveURL(/\/dashboard$/);
    await expect(page.getByRole('heading', { name: /Bảng Điều Hành Quản Trị Khách Sạn/ })).toBeVisible();
  });
});
