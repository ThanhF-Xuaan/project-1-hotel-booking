import { expect, test } from '@playwright/test'

const consentView = (granted: boolean, revision = 0) => ({
  hotelId: 1,
  status: granted ? 'GRANTED' : 'NOT_GRANTED',
  granted,
  revision,
  bindingVersion: 4,
  purpose: 'ROOM_TYPE_RECOMMENDATION',
  currentPolicyVersion: 'u14-local-room-affinity-v1',
  acceptedPolicyVersion: granted ? 'u14-local-room-affinity-v1' : null,
  updatedAt: granted ? '2026-10-26T10:00:00Z' : null,
  historyLookbackMonths: 12,
  noticeText: 'Demo synthetic data notice',
  privacyPolicyUrl: '/portal/privacy',
  dataMode: 'LOCAL_SYNTHETIC',
})

const recommendation = {
  status: 'PERSONALIZED_READY',
  hotelId: 1,
  dataMode: 'LOCAL_SYNTHETIC',
  roomTypeSuggestions: [{
    hotelRoomTypeId: 17,
    roomTypeCode: 'DLX',
    roomTypeName: 'Phòng Deluxe',
    affinityScore: 0.875,
    reasonCode: 'HISTORICAL_BOOKING_AFFINITY',
  }],
  nonBindingSuggestion: true,
  pricingOrPromotionMutated: false,
  reasonCode: null,
}

async function signInAsCustomer(page: import('@playwright/test').Page) {
  const payload = Buffer.from(JSON.stringify({
    iss: 'https://identity.example.test/realms/hotel',
    sub: 'customer-demo-1',
    realm_access: { roles: ['CUSTOMER'] },
  })).toString('base64url')
  await page.addInitScript((token) => localStorage.setItem('auth_token', token), `header.${payload}.signature`)
}

test('returns to the complete protected URL after login', async ({ page }) => {
  const payload = Buffer.from(JSON.stringify({
    iss: 'https://identity.example.test/realms/hotel',
    sub: 'customer-deep-link',
    realm_access: { roles: ['CUSTOMER'] },
  })).toString('base64url')
  const accessToken = `header.${payload}.signature`

  await page.route('**/api/v1/auth/login', async (route) => {
    await route.fulfill({
      json: { code: 200, message: 'ok', result: { access_token: accessToken, refresh_token: 'synthetic-refresh' } },
    })
  })
  await page.route('**/api/v1/me/personalization-consent*', async (route) => {
    await route.fulfill({ json: { code: 200, message: 'ok', result: consentView(false) } })
  })

  await page.goto('/portal/personalization?hotelId=1#policy')
  await expect(page).toHaveURL(/\/login$/)
  await page.getByLabel('Tên đăng nhập hoặc Email', { exact: true }).fill('test_customer')
  await page.getByLabel('Mật khẩu', { exact: true }).fill('synthetic-password')
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click()

  await expect(page).toHaveURL(/\/portal\/personalization\?hotelId=1#policy$/)
  await expect(page.getByRole('checkbox')).toBeVisible()
})

test('requires unchecked policy consent before requesting a recommendation', async ({ page }) => {
  await signInAsCustomer(page)
  let recommendationCalls = 0
  let consentUpdate: Record<string, unknown> | undefined

  await page.route('**/api/v1/me/personalization-consent*', async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({ json: { code: 200, message: 'ok', result: consentView(false) } })
      return
    }
    consentUpdate = route.request().postDataJSON() as Record<string, unknown>
    await route.fulfill({ json: { code: 200, message: 'ok', result: consentView(true, 1) } })
  })
  await page.route('**/api/v1/me/personalization/recommendations*', async (route) => {
    recommendationCalls += 1
    await route.fulfill({ json: { code: 200, message: 'ok', result: recommendation } })
  })

  await page.goto('/portal/personalization?hotelId=1')
  const consentCheckbox = page.getByRole('checkbox')
  const grantButton = page.getByRole('button', { name: 'Đồng ý và xem gợi ý' })
  await expect(consentCheckbox).toBeVisible()
  await expect(consentCheckbox).not.toBeChecked()
  await expect(grantButton).toBeDisabled()
  expect(recommendationCalls).toBe(0)

  await consentCheckbox.check()
  await grantButton.click()

  await expect(page.getByRole('heading', { name: 'Phòng Deluxe' })).toBeVisible()
  expect(consentUpdate).toEqual({
    hotelId: 1,
    granted: true,
    expectedRevision: 0,
    expectedBindingVersion: 4,
    policyVersion: 'u14-local-room-affinity-v1',
  })
  expect(recommendationCalls).toBe(1)
  await expect(page.getByText('0.875')).toHaveCount(0)
})

test('opens the demo privacy page with data scope and consent withdrawal guidance', async ({ page }) => {
  await signInAsCustomer(page)
  await page.route('**/api/v1/me/personalization-consent*', async (route) => {
    await route.fulfill({ json: { code: 200, message: 'ok', result: consentView(false) } })
  })

  await page.goto('/portal/personalization?hotelId=1')
  await page.getByRole('link', { name: 'Đọc thông tin quyền riêng tư của demo' }).click()

  await expect(page).toHaveURL(/\/portal\/privacy\?hotelId=1$/)
  await expect(page.getByRole('heading', { name: 'Quyền riêng tư trong demo U14' })).toBeVisible()
  await expect(page.getByText(/Demo — dữ liệu giả lập/)).toBeVisible()
  await expect(page.getByText(/FIT một phòng đã hoàn tất/)).toBeVisible()
  await expect(page.getByText(/PostgreSQL và Keycloak của sandbox acceptance local/)).toBeVisible()
  await expect(page.getByText(/rút consent bất cứ lúc nào/)).toBeVisible()
  await expect(page.getByText(/không phải chính sách quyền riêng tư cho khách hàng thật/)).toBeVisible()
  await page.getByRole('link', { name: 'gợi ý hạng phòng cá nhân hóa' }).click()
  await expect(page).toHaveURL(/\/portal\/personalization\?hotelId=1$/)
})

test('refreshes consent after a revision conflict without resubmitting automatically', async ({ page }) => {
  await signInAsCustomer(page)
  let consentReads = 0
  let consentUpdates = 0
  let recommendationCalls = 0

  await page.route('**/api/v1/me/personalization-consent*', async (route) => {
    if (route.request().method() === 'GET') {
      consentReads += 1
      const current = consentReads === 1 ? consentView(false, 0) : consentView(false, 1)
      await route.fulfill({ json: { code: 200, message: 'ok', result: current } })
      return
    }
    consentUpdates += 1
    await route.fulfill({
      status: 409,
      json: {
        code: 11002,
        message: 'Consent changed',
        result: { reasonCode: 'CONSENT_REVISION_CONFLICT', retryable: false },
      },
    })
  })
  await page.route('**/api/v1/me/personalization/recommendations*', async (route) => {
    recommendationCalls += 1
    await route.fulfill({ json: { code: 200, message: 'ok', result: recommendation } })
  })

  await page.goto('/portal/personalization?hotelId=1')
  await page.getByRole('checkbox').check()
  await page.getByRole('button', { name: 'Đồng ý và xem gợi ý' }).click()

  await expect(page.getByText('Trạng thái consent đã được đồng bộ. Hãy kiểm tra lại trước khi tiếp tục.')).toBeVisible()
  await expect(page.getByRole('checkbox')).not.toBeChecked()
  expect(consentReads).toBeGreaterThanOrEqual(2)
  expect(consentUpdates).toBe(1)
  expect(recommendationCalls).toBe(0)
})

test('shows dependency outage and retries recommendations only after consent is confirmed', async ({ page }) => {
  await signInAsCustomer(page)
  let recommendationCalls = 0

  await page.route('**/api/v1/me/personalization-consent*', async (route) => {
    await route.fulfill({ json: { code: 200, message: 'ok', result: consentView(true, 2) } })
  })
  await page.route('**/api/v1/me/personalization/recommendations*', async (route) => {
    recommendationCalls += 1
    if (recommendationCalls === 1) {
      await route.fulfill({
        status: 503,
        json: {
          code: 11005,
          message: 'Unavailable',
          result: { reasonCode: 'DEPENDENCY_UNAVAILABLE', retryable: true },
        },
      })
      return
    }
    await route.fulfill({ json: { code: 200, message: 'ok', result: recommendation } })
  })

  await page.goto('/portal/personalization?hotelId=1')
  await expect(page.getByText('Dịch vụ tạm thời chưa sẵn sàng. Bạn có thể thử lại sau.')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Tải gợi ý' })).toBeEnabled()
  await page.getByRole('button', { name: 'Tải gợi ý' }).click()
  await expect(page.getByRole('heading', { name: 'Phòng Deluxe' })).toBeVisible()
  expect(recommendationCalls).toBe(2)
})

test('removes rendered suggestions as soon as the customer revokes consent', async ({ page }) => {
  await signInAsCustomer(page)
  let releaseRevoke!: () => void
  let notifyRevokeStarted!: () => void
  const waitForRelease = new Promise<void>((resolve) => { releaseRevoke = resolve })
  const revokeStarted = new Promise<void>((resolve) => { notifyRevokeStarted = resolve })

  await page.route('**/api/v1/me/personalization-consent*', async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({ json: { code: 200, message: 'ok', result: consentView(true, 3) } })
      return
    }
    notifyRevokeStarted()
    await waitForRelease
    await route.fulfill({ json: { code: 200, message: 'ok', result: consentView(false, 4) } })
  })
  await page.route('**/api/v1/me/personalization/recommendations*', async (route) => {
    await route.fulfill({ json: { code: 200, message: 'ok', result: recommendation } })
  })

  await page.goto('/portal/personalization?hotelId=1')
  await expect(page.getByRole('heading', { name: 'Phòng Deluxe' })).toBeVisible()
  await page.getByRole('button', { name: 'Rút consent' }).click()
  await revokeStarted
  await expect(page.getByRole('heading', { name: 'Phòng Deluxe' })).toHaveCount(0)
  releaseRevoke()
  await expect(page.getByRole('button', { name: 'Đồng ý và xem gợi ý' })).toBeVisible()
})

test('blocks a non-customer before calling personalization APIs', async ({ page }) => {
  const payload = Buffer.from(JSON.stringify({ realm_access: { roles: ['RECEPTIONIST'] } })).toString('base64url')
  await page.addInitScript((token) => localStorage.setItem('auth_token', token), `header.${payload}.signature`)
  let apiCalls = 0
  await page.route('**/api/v1/me/**', async (route) => {
    apiCalls += 1
    await route.fulfill({ status: 500, json: { code: 500, message: 'should not be called' } })
  })

  await page.goto('/portal/personalization?hotelId=1')
  await expect(page.getByRole('heading', { name: /403/ })).toBeVisible()
  expect(apiCalls).toBe(0)
})
