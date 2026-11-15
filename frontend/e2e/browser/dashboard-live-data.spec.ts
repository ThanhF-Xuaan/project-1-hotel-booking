import { expect, test } from '@playwright/test';

const kpi = (hotelId = 7) => ({
  hotelId, hotelName: 'Khách sạn kiểm thử', targetDate: '2026-11-15',
  occupancyRate: 26.7, averageDailyRate: 550000, revPar: 146666.67,
  totalActiveRooms: 15, occupiedRooms: 4, availableRooms: 8, outOfServiceRooms: 3,
  totalRoomRevenue: 2200000, totalServiceRevenue: 34020, totalRevenueToday: 2234020,
  arrivalsTodayCount: 1, departuresTodayCount: 0, inHouseGuestsCount: 8,
});

test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => {
    const payload = btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600,
      realm_access: { roles: ['CHAIN_ADMIN'] } }));
    localStorage.setItem('auth_token', `e30.${payload}.fixture`);
  });
});

test('loads real hotel IDs across pages and sends selected date to all reports', async ({ page }) => {
  const urls: string[] = [];
  await page.route('**/api/v1/**', async route => {
    const url = new URL(route.request().url());
    if (url.pathname === '/api/v1/hotels/filter') {
      const pageNumber = route.request().postDataJSON().page;
      return route.fulfill({ json: { code: 200, result: { content: [pageNumber === 0
        ? { id: 7, name: 'Cơ sở thật A' } : { id: 8, name: 'Cơ sở thật B' }], last: pageNumber === 1, empty: false } } });
    }
    if (url.pathname.startsWith('/api/v1/dashboard/')) {
      urls.push(url.toString());
      const hotelId = Number(url.searchParams.get('hotelId'));
      const date = url.searchParams.get('endDate');
      return route.fulfill({ json: { code: 200, result: url.pathname.endsWith('/kpis') ? kpi(hotelId)
        : url.pathname.endsWith('/revenue-trends') ? [{ date, roomRevenue: 2200000, serviceRevenue: 34020, totalRevenue: 2234020 }]
          : [{ date, totalRooms: 15, occupiedRooms: 4, occupancyRate: 26.7 }] } });
    }
    await route.fulfill({ json: { code: 200, result: [] } });
  });
  await page.goto('/dashboard');
  await expect(page.getByText('Tổng: 15 phòng')).toBeVisible();
  expect(urls.every(url => !url.includes('hotelId=1'))).toBe(true);
  await page.getByLabel('Ngày báo cáo').fill('2026-11-10');
  await expect.poll(() => urls.filter(url => url.includes('2026-11-10')).length).toBe(3);
  const trends = urls.filter(url => url.includes('endDate=2026-11-10'));
  expect(trends).toHaveLength(2);
  expect(trends.every(url => url.includes('startDate=2026-11-04'))).toBe(true);
  await page.getByRole('button', { name: 'Cơ sở thật A', exact: true }).click();
  await page.getByRole('option', { name: 'Cơ sở thật B' }).click();
  await expect.poll(() => urls.filter(url => url.includes('hotelId=8')).length).toBe(3);
});

test('KPI failure is visible, preserves successful trends and allows retry', async ({ page }) => {
  let fail = true;
  await page.route('**/api/v1/**', async route => {
    const path = new URL(route.request().url()).pathname;
    if (path === '/api/v1/hotels/filter') return route.fulfill({ json: { code: 200,
      result: { content: [{ id: 7, name: 'Cơ sở thật A' }], last: true } } });
    if (path.endsWith('/kpis')) return route.fulfill(fail
      ? { status: 404, json: { code: 2021, message: 'Không tìm thấy khách sạn' } }
      : { json: { code: 200, result: kpi() } });
    return route.fulfill({ json: { code: 200, result: [] } });
  });
  await page.goto('/dashboard');
  await expect(page.getByRole('alert')).toContainText('Không tải được chỉ số KPI');
  await expect(page.getByText('Tổng: 0 phòng')).toHaveCount(0);
  await expect(page.getByText('Chưa có dữ liệu xu hướng doanh thu trong khoảng ngày đã chọn.')).toBeVisible();
  fail = false;
  await page.getByRole('button', { name: 'Làm mới', exact: true }).click();
  await expect(page.getByText('Tổng: 15 phòng')).toBeVisible();
  await expect(page.getByRole('alert')).toHaveCount(0);
});

test('hotel lookup failure does not request made-up hotel IDs', async ({ page }) => {
  const reports: string[] = [];
  await page.route('**/api/v1/**', async route => {
    const path = new URL(route.request().url()).pathname;
    if (path.startsWith('/api/v1/dashboard/')) reports.push(path);
    return route.fulfill({ status: 503, json: { code: 503 } });
  });
  await page.goto('/dashboard');
  await expect(page.getByRole('alert')).toContainText('Không tải được danh sách khách sạn');
  await expect(page.getByRole('button', { name: 'Thử lại' })).toBeVisible();
  expect(reports).toHaveLength(0);
  await expect(page.getByText('Tổng: 0 phòng')).toHaveCount(0);
});

test('late response from the previous hotel cannot overwrite the selected hotel', async ({ page }) => {
  let release: () => void = () => {};
  const gate = new Promise<void>(resolve => { release = resolve; });
  let firstStarted = false;
  await page.route('**/api/v1/**', async route => {
    const url = new URL(route.request().url());
    if (url.pathname.endsWith('/hotels/filter')) return route.fulfill({ json: { code: 200,
      result: { content: [{ id: 7, name: 'Cơ sở A' }, { id: 8, name: 'Cơ sở B' }], last: true } } });
    const id = Number(url.searchParams.get('hotelId'));
    if (id === 7) { firstStarted = true; await gate; }
    return route.fulfill({ json: { code: 200, result: url.pathname.endsWith('/kpis')
      ? { ...kpi(id), totalActiveRooms: id === 7 ? 15 : 30 } : [] } });
  });
  await page.goto('/dashboard');
  await expect.poll(() => firstStarted).toBe(true);
  await page.getByRole('button', { name: 'Cơ sở A', exact: true }).click();
  await page.getByRole('option', { name: 'Cơ sở B' }).click();
  await expect(page.getByText('Tổng: 30 phòng')).toBeVisible();
  release();
  await expect(page.getByText('Tổng: 30 phòng')).toBeVisible();
  await expect(page.getByText('Tổng: 15 phòng')).toHaveCount(0);
});

test('invalid success envelope is an error instead of zero KPI cards', async ({ page }) => {
  await page.route('**/api/v1/**', route => {
    const path = new URL(route.request().url()).pathname;
    return route.fulfill({ json: { code: 200, result: path.endsWith('/hotels/filter')
      ? { content: [{ id: 7, name: 'Cơ sở A' }], last: true }
      : path.endsWith('/kpis') ? null : [] } });
  });
  await page.goto('/dashboard');
  await expect(page.getByRole('alert')).toContainText('Không tải được chỉ số KPI');
  await expect(page.getByText('Tổng: 0 phòng')).toHaveCount(0);
});
