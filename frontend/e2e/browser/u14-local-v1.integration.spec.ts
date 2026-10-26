import { test, expect } from '@playwright/test';

const CONSENT_PATH = '/api/v1/me/personalization-consent';
const RECOMMENDATIONS_PATH = '/api/v1/me/personalization/recommendations';

test('CUSTOMER completes local U14 consent and personalized recommendation through the real app and backend', async ({ page }) => {
  test.setTimeout(60_000);

  await page.goto('/portal/personalization?hotelId=1');
  await expect(page).toHaveURL(/\/login$/);
  await page.getByLabel('Tên đăng nhập hoặc Email', { exact: true }).fill('test_customer_b');
  await page.getByLabel('Mật khẩu', { exact: true }).fill('Password123!');

  const loginResponsePromise = page.waitForResponse((response) =>
    new URL(response.url()).pathname === '/api/v1/auth/login'
      && response.request().method() === 'POST'
  );
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  expect((await loginResponsePromise).status()).toBe(200);
  await expect(page).toHaveURL(/\/portal\/personalization\?hotelId=1$/);

  const policyCheckbox = page.getByRole('checkbox');
  await expect(policyCheckbox).toBeVisible();
  await expect(policyCheckbox).not.toBeChecked();

  const grantResponsePromise = page.waitForResponse((response) =>
    new URL(response.url()).pathname === CONSENT_PATH && response.request().method() === 'PUT'
  );
  const recommendationResponsePromise = page.waitForResponse((response) =>
    new URL(response.url()).pathname === RECOMMENDATIONS_PATH && response.request().method() === 'GET'
  );
  await policyCheckbox.check();
  await page.getByRole('button', { name: 'Đồng ý và xem gợi ý', exact: true }).click();

  expect((await grantResponsePromise).status()).toBe(200);
  const recommendationResponse = await recommendationResponsePromise;
  expect(recommendationResponse.status()).toBe(200);
  const recommendation = (await recommendationResponse.json()).result;
  expect(recommendation.status).toBe('PERSONALIZED_READY');
  expect(recommendation.roomTypeSuggestions).toHaveLength(1);
  expect(recommendation.roomTypeSuggestions[0].hotelRoomTypeId).toBe(2);
  await expect(page.getByRole('heading', { name: /Deluxe Room/ })).toBeVisible();

  const revokeResponsePromise = page.waitForResponse((response) =>
    new URL(response.url()).pathname === CONSENT_PATH && response.request().method() === 'PUT'
  );
  await page.getByRole('button', { name: 'Rút consent', exact: true }).click();
  expect((await revokeResponsePromise).status()).toBe(200);
  await expect(page.getByRole('checkbox')).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Hạng phòng gợi ý', exact: true })).toHaveCount(0);
});
