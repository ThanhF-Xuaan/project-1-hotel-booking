import { test, expect } from '@playwright/test';

test.describe('Row 28: Group contract cancellation reason verification', () => {
  test('cancels group contract with reason successfully without validation error', async ({ page }) => {
    // 1. Login with admin / 123456
    await page.goto('/login');
    await page.getByLabel('Tên đăng nhập hoặc Email', { exact: true }).fill('admin');
    await page.getByLabel('Mật khẩu', { exact: true }).fill('123456');

    const loginResponse = page.waitForResponse(
      (res) => new URL(res.url()).pathname === '/api/v1/auth/login' && res.request().method() === 'POST'
    );
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
    await loginResponse;
    await expect(page).toHaveURL(/\/dashboard$/);

    // 2. Navigate to Group Management Page
    await page.goto('/booking/groups');
    await expect(page.getByRole('heading', { name: 'Quản Lý Khách Đoàn' })).toBeVisible();

    // 3. Monitor dialog alerts (should NOT trigger 'Lý do hủy hợp đồng không được để trống')
    let dialogMessage = '';
    page.on('dialog', async (dialog) => {
      dialogMessage = dialog.message();
      console.log('Dialog detected:', dialogMessage);
      await dialog.accept();
    });

    // 4. Click the first "Chi tiết" button in the table
    const detailButtons = page.getByRole('button', { name: 'Chi tiết' });
    await expect(detailButtons.first()).toBeVisible();
    await detailButtons.first().click();

    // 5. In the drawer, check if "Hủy Hợp Đồng" button is visible
    const cancelBtn = page.getByRole('button', { name: 'Hủy Hợp Đồng' });
    await expect(cancelBtn).toBeVisible();
    await cancelBtn.click();

    // 6. Confirm Modal should be visible
    await expect(page.getByText('Xác Nhận Hủy Hợp Đồng Khách Đoàn')).toBeVisible();

    // 7. Type cancellation reason
    const reasonText = 'Khách dời lịch công tác sang quý sau do thời tiết xấu';
    const reasonTextarea = page.getByPlaceholder('VD: Khách đổi lịch trình, không duyệt thanh toán cọc...');
    await expect(reasonTextarea).toBeVisible();
    await reasonTextarea.fill(reasonText);

    // 8. Capture the cancel API request & response
    const cancelPromise = page.waitForResponse(
      (res) => new URL(res.url()).pathname.endsWith('/cancel') && res.request().method() === 'POST'
    );

    await page.getByRole('button', { name: 'Đồng Ý Hủy HĐ' }).click();

    const cancelRes = await cancelPromise;
    expect(cancelRes.status()).toBe(200);

    const cancelBody = await cancelRes.json();
    console.log('Cancel response body:', JSON.stringify(cancelBody));
    expect(['CANCELLED', 'CANCELLED_NO_DEPOSIT']).toContain(cancelBody?.result?.status);
    expect(cancelBody?.result?.cancellationReason).toBe(reasonText);

    // 9. Confirm that dialog did not show "Lý do hủy hợp đồng không được để trống"
    expect(dialogMessage).not.toContain('Lý do hủy hợp đồng không được để trống');

    // 10. Drawer and Modal should be closed
    await page.waitForTimeout(1000);
    await expect(page.getByText('Xác Nhận Hủy Hợp Đồng Khách Đoàn')).toBeHidden();
  });
});
