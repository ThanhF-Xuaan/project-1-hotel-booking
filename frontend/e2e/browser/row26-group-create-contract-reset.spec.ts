import { test, expect } from '@playwright/test';

test.describe('Row 26: GroupCreateContractModal form reset on close and reopen', () => {
  test.beforeEach(async ({ page }) => {
    // Intercept backend API calls so the fixture runs deterministically
    await page.route(/\/api\/v1\/companies/, async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          json: {
            code: 200,
            message: 'ok',
            result: { id: 888, name: 'Công ty Du Lịch Á Châu' },
          },
        });
      } else {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          json: { code: 200, message: 'ok', result: [] },
        });
      }
    });

    await page.route(/\/api\/v1\/hotels/, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        json: {
          code: 200,
          message: 'ok',
          result: { content: [{ id: 1, name: 'Khách sạn Hà Nội Central' }] },
        },
      });
    });

    await page.route(/\/api\/v1\/(inventory\/)?hotel-room-types/, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        json: {
          code: 200,
          message: 'ok',
          result: {
            content: [
              { id: 101, roomTypeName: 'Deluxe King Suite', basePrice: 1500000 },
            ],
          },
        },
      });
    });

    await page.route(/\/api\/v1\/staffs/, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        json: {
          code: 200,
          message: 'ok',
          result: { content: [{ id: 5, fullName: 'Nguyễn Văn Quản Lý', username: 'quanly' }] },
        },
      });
    });

    await page.goto('/e2e/fixtures/group-create-reset.html');
  });

  test('resets all form fields when kept mounted and user closes with Hủy bỏ', async ({ page }) => {
    // 1. Open modal kept mounted
    await page.locator('#btn-open-kept').click();
    await expect(page.getByRole('heading', { name: 'Tạo Hợp Đồng Khách Đoàn' })).toBeVisible();

    // 2. Fill in inputs
    const compName = page.getByPlaceholder('VD: Tổng Công ty Cổ phần May 10');
    const compTax = page.getByPlaceholder('0100109106');
    const groupName = page.getByPlaceholder('VD: Đoàn Hội Thảo Tổng Kết Q4/2026');
    const compAddr = page.getByPlaceholder('VD: 765 Nguyễn Văn Linh, Long Biên, Hà Nội');
    const compPhone = page.getByPlaceholder('VD: 0912345678');
    const compEmail = page.getByPlaceholder('contact@company.com');
    const specialReq = page.getByPlaceholder('VD: Hội trường 80 người set up chữ U ngày 16/11, xuất hóa đơn VAT công ty...');

    await compName.fill('Công ty TNHH Hàng Không');
    await compTax.fill('0109876543');
    await groupName.fill('Đoàn Tham Quan Đà Nẵng');
    await compAddr.fill('456 Đường Nguyễn Huệ, Quận 1');
    await compPhone.fill('0912345678');
    await compEmail.fill('tour@airline.com');
    await specialReq.fill('Cần 2 phòng view biển và 1 phòng họp nhỏ');

    // Verify fields have values
    await expect(compName).toHaveValue('Công ty TNHH Hàng Không');
    await expect(compTax).toHaveValue('0109876543');
    await expect(groupName).toHaveValue('Đoàn Tham Quan Đà Nẵng');

    // 3. Click "Hủy bỏ"
    await page.getByRole('button', { name: 'Hủy bỏ' }).click();
    await expect(page.getByRole('heading', { name: 'Tạo Hợp Đồng Khách Đoàn' })).toBeHidden();

    // 4. Reopen the modal
    await page.locator('#btn-open-kept').click();
    await expect(page.getByRole('heading', { name: 'Tạo Hợp Đồng Khách Đoàn' })).toBeVisible();

    // 5. Verify all fields are completely blank / reset
    await expect(page.getByPlaceholder('VD: Tổng Công ty Cổ phần May 10')).toHaveValue('');
    await expect(page.getByPlaceholder('0100109106')).toHaveValue('');
    await expect(page.getByPlaceholder('VD: Đoàn Hội Thảo Tổng Kết Q4/2026')).toHaveValue('');
    await expect(page.getByPlaceholder('VD: 765 Nguyễn Văn Linh, Long Biên, Hà Nội')).toHaveValue('');
    await expect(page.getByPlaceholder('VD: 0912345678')).toHaveValue('');
    await expect(page.getByPlaceholder('contact@company.com')).toHaveValue('');
    await expect(page.getByPlaceholder('VD: Hội trường 80 người set up chữ U ngày 16/11, xuất hóa đơn VAT công ty...')).toHaveValue('');
  });

  test('resets all form fields when kept mounted and user closes with X button', async ({ page }) => {
    // 1. Open modal kept mounted
    await page.locator('#btn-open-kept').click();
    await expect(page.getByRole('heading', { name: 'Tạo Hợp Đồng Khách Đoàn' })).toBeVisible();

    // 2. Fill in inputs
    const compName = page.getByPlaceholder('VD: Tổng Công ty Cổ phần May 10');
    const compTax = page.getByPlaceholder('0100109106');
    const groupName = page.getByPlaceholder('VD: Đoàn Hội Thảo Tổng Kết Q4/2026');

    await compName.fill('Tập đoàn Công nghệ FPT');
    await compTax.fill('0101234567');
    await groupName.fill('Teambuilding Khối Phần Mềm');

    // 3. Click "X" close button
    await page.getByRole('button', { name: 'Đóng' }).click();
    await expect(page.getByRole('heading', { name: 'Tạo Hợp Đồng Khách Đoàn' })).toBeHidden();

    // 4. Reopen modal
    await page.locator('#btn-open-kept').click();
    await expect(page.getByRole('heading', { name: 'Tạo Hợp Đồng Khách Đoàn' })).toBeVisible();

    // 5. Verify fields are reset
    await expect(page.getByPlaceholder('VD: Tổng Công ty Cổ phần May 10')).toHaveValue('');
    await expect(page.getByPlaceholder('0100109106')).toHaveValue('');
    await expect(page.getByPlaceholder('VD: Đoàn Hội Thảo Tổng Kết Q4/2026')).toHaveValue('');
  });

  test('resets all form fields in unmounted mode (GroupManagementPage pattern)', async ({ page }) => {
    // 1. Open unmounted modal
    await page.locator('#btn-open-unmounted').click();
    await expect(page.getByRole('heading', { name: 'Tạo Hợp Đồng Khách Đoàn' })).toBeVisible();

    // 2. Fill in inputs
    await page.getByPlaceholder('VD: Tổng Công ty Cổ phần May 10').fill('Viettel Telecom');
    await page.getByPlaceholder('0100109106').fill('0104567890');
    await page.getByPlaceholder('VD: Đoàn Hội Thảo Tổng Kết Q4/2026').fill('Hội thảo 5G');

    // 3. Cancel
    await page.getByRole('button', { name: 'Hủy bỏ' }).click();
    await expect(page.getByRole('heading', { name: 'Tạo Hợp Đồng Khách Đoàn' })).toBeHidden();

    // 4. Reopen
    await page.locator('#btn-open-unmounted').click();
    await expect(page.getByRole('heading', { name: 'Tạo Hợp Đồng Khách Đoàn' })).toBeVisible();

    // 5. Verify blank
    await expect(page.getByPlaceholder('VD: Tổng Công ty Cổ phần May 10')).toHaveValue('');
    await expect(page.getByPlaceholder('0100109106')).toHaveValue('');
    await expect(page.getByPlaceholder('VD: Đoàn Hội Thảo Tổng Kết Q4/2026')).toHaveValue('');
  });

  test('resets all form fields after successful submission and reopen', async ({ page }) => {
    // 1. Open modal kept mounted
    await page.locator('#btn-open-kept').click();
    await expect(page.getByRole('heading', { name: 'Tạo Hợp Đồng Khách Đoàn' })).toBeVisible();

    // 2. Fill required inputs
    await page.getByPlaceholder('VD: Tổng Công ty Cổ phần May 10').fill('Công ty Du Lịch Á Châu');
    await page.getByPlaceholder('0100109106').fill('0109999888');
    await page.getByPlaceholder('VD: Đoàn Hội Thảo Tổng Kết Q4/2026').fill('Đoàn Du Lịch Trọn Gói');

    // Select staff by clicking trigger to trigger loadStaffs
    const staffSelect = page.getByRole('button', { name: /Chọn nhân viên phụ trách/i });
    await staffSelect.click();
    await expect(page.getByRole('button', { name: /Nguyễn Văn Quản Lý/i })).toBeVisible();

    // 3. Submit
    await page.getByRole('button', { name: /Tạo Hợp Đồng & Giữ Phòng/i }).click();
    await expect(page.getByRole('heading', { name: 'Tạo Hợp Đồng Khách Đoàn' })).toBeHidden();

    // 4. Reopen modal
    await page.locator('#btn-open-kept').click();
    await expect(page.getByRole('heading', { name: 'Tạo Hợp Đồng Khách Đoàn' })).toBeVisible();

    // 5. Verify fields are reset
    await expect(page.getByPlaceholder('VD: Tổng Công ty Cổ phần May 10')).toHaveValue('');
    await expect(page.getByPlaceholder('0100109106')).toHaveValue('');
    await expect(page.getByPlaceholder('VD: Đoàn Hội Thảo Tổng Kết Q4/2026')).toHaveValue('');
  });

  test('opens modal on /admin/groups page without white screen or useRef error', async ({ page }) => {
    const consoleErrors: string[] = [];
    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text());
      }
    });


    // Mock admin token in localStorage
    const token = `header.${Buffer.from(
      JSON.stringify({ sub: 'admin-user', realm_access: { roles: ['CHAIN_ADMIN', 'SALES_GROUP'] } })
    ).toString('base64url')}.signature`;

    await page.addInitScript((t) => {
      localStorage.setItem('auth_token', t);
    }, token);

    // Mock group contracts API
    await page.route(/\/api\/v1\/group-contracts/, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        json: {
          code: 200,
          message: 'ok',
          result: { content: [], totalPages: 1, totalElements: 0, pageNumber: 0 },
        },
      });
    });

    await page.goto('/admin/groups');
    await expect(page.getByRole('heading', { name: 'Quản Lý Khách Đoàn' })).toBeVisible();

    // Click "+ Tạo Hợp Đồng" button
    const createBtn = page.getByRole('button', { name: /Tạo Hợp Đồng/i });
    await expect(createBtn).toBeVisible();
    await createBtn.click();

    // Modal should be visible and NOT crash into white screen
    await expect(page.getByRole('heading', { name: 'Tạo Hợp Đồng Khách Đoàn' })).toBeVisible();
    await expect(page.getByPlaceholder('VD: Tổng Công ty Cổ phần May 10')).toBeVisible();

    // Verify no "useRef is not defined" error occurred
    const hasUseRefError = consoleErrors.some((e) => e.includes('useRef is not defined'));
    expect(hasUseRefError).toBe(false);
  });
});
