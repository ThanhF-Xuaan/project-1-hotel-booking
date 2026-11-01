import { expect, test, type Page } from '@playwright/test'
import { readFileSync } from 'node:fs'

const PATH = '/ai/demo/occupancy'
const API = '**/api/v1/ai/demo/occupancy*'
const corpus = JSON.parse(readFileSync(new URL('../../../backend/src/main/resources/ai/demo/occupancy-history-v1.json', import.meta.url), 'utf8')) as {
  hotels: { hotelId: number; samples: { hotelId: number; date: string; occupiedRooms: number | null; capacityRooms: number | null; coverage: string }[] }[]
}

function response(scenario = 'HOTEL_A') {
  const hotel = corpus.hotels[scenario === 'HOTEL_A' ? 0 : 1]
  const samples = structuredClone(hotel.samples)
  samples[samples.length - 4] = { ...samples[samples.length - 4], occupiedRooms: null, capacityRooms: null, coverage: 'MISSING' }
  samples[samples.length - 2] = { ...samples[samples.length - 2], occupiedRooms: 0, capacityRooms: 0, coverage: 'COMPLETE' }
  return {
    dataMode: 'SYNTHETIC', businessData: false, scenario, referenceInstant: '2026-10-29T17:00:00Z',
    history: { status: 'PARTIAL', hotelId: hotel.hotelId, fromInclusive: samples[0].date, toInclusive: '2026-10-29', asOf: '2026-10-29T17:00:00Z', dataMode: 'SYNTHETIC', samples },
    forecast: { status: 'FORECAST_READY', hotelId: hotel.hotelId, asOf: '2026-10-29T17:00:00Z', dataMode: 'SYNTHETIC', model: 'SEASONAL_NAIVE_7' as string | null, modelVersion: 'u22-seasonal-naive-7-v1', rangeMethod: 'ROLLING_OUT_OF_SAMPLE_ABS_ERROR_P90', residualRadiusPp: 5 as number | null,
      points: Array.from({ length: 30 }, (_, i) => ({ date: new Date(Date.UTC(2026, 9, 30 + i)).toISOString().slice(0, 10), occupancyPercent: (scenario === 'HOTEL_A' ? 70 : 80) + i / 10, lowerPercent: (scenario === 'HOTEL_A' ? 65 : 75) + i / 10, upperPercent: (scenario === 'HOTEL_A' ? 75 : 85) + i / 10 })) },
  }
}
async function signIn(page: Page, role = 'CHAIN_ADMIN') {
  const token = `header.${Buffer.from(JSON.stringify({ sub: 'u22-synthetic-ui', realm_access: { roles: [role] } })).toString('base64url')}.signature`
  await page.addInitScript(token => {
    if (!sessionStorage.getItem('u22-test-seeded')) {
      localStorage.setItem('auth_token', token)
      sessionStorage.setItem('u22-test-seeded', 'true')
    }
  }, token)
}
const envelope = (result: unknown) => ({ code: 200, message: 'ok', result })

test('renders synthetic chart, full accessible tables, missing/zero-capacity gaps and read-only scenario change on mobile', async ({ page }, testInfo) => {
  await signIn(page)
  await page.setViewportSize({ width: 375, height: 812 })
  await page.emulateMedia({ reducedMotion: 'reduce' })
  const calls: string[] = []
  await page.route(API, route => {
    expect(route.request().method()).toBe('GET')
    const query = new URL(route.request().url()).searchParams
    expect([...query.keys()]).toEqual(['scenario'])
    calls.push(query.get('scenario')!)
    return route.fulfill({ json: envelope(response(query.get('scenario')!)) })
  })
  await page.goto(PATH)
  await expect(page.getByRole('heading', { name: 'Dự báo tỷ lệ lấp đầy', exact: true })).toBeVisible()
  await expect(page.getByText('DEMO · DỮ LIỆU GIẢ LẬP', { exact: true })).toBeVisible()
  const chart = page.getByRole('img', { name: /Lịch sử và dự báo/ })
  await expect(chart).toBeVisible()
  await expect(page.getByRole('table')).toHaveCount(1)
  await expect(page.getByRole('table').getByRole('row')).toHaveCount(31)
  await expect(page.getByRole('row', { name: '30/10/2026 70% 65% 75%' })).toBeVisible()
  await page.getByText('Toàn bộ lịch sử mô phỏng (420 ngày)', { exact: true }).click()
  const history = page.getByRole('region', { name: 'Số liệu lịch sử', exact: true })
  await expect(history.getByRole('row')).toHaveCount(421)
  await expect(history.getByRole('row', { name: '28/10/2026 0 0 Không có sức chứa', exact: true })).toHaveCount(1)
  expect((await page.getByTestId('occupancy-history-line').getAttribute('d'))!.match(/M/g)!.length).toBeGreaterThanOrEqual(3)
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
  await page.screenshot({ path: testInfo.outputPath('u22-mobile.png'), fullPage: true })
  await page.setViewportSize({ width: 1440, height: 1000 })
  await page.getByLabel('Kịch bản mô phỏng', { exact: true }).selectOption('HOTEL_B')
  await expect(chart).toBeVisible()
  expect(calls).toContain('HOTEL_B')
  await page.getByRole('button', { name: 'Tải lại', exact: true }).focus()
  await page.keyboard.press('Enter')
  await expect(chart).toBeVisible()
  expect(calls.filter(value => value === 'HOTEL_B').length).toBeGreaterThanOrEqual(2)
  await page.screenshot({ path: testInfo.outputPath('u22-desktop.png'), fullPage: true })
})

test('loading and late previous scenario cannot overwrite current selection', async ({ page }) => {
  await signIn(page)
  let release!: () => void
  const gate = new Promise<void>(resolve => { release = resolve })
  await page.route(API, async route => {
    const scenario = new URL(route.request().url()).searchParams.get('scenario')!
    if (scenario === 'HOTEL_A') await gate
    await route.fulfill({ json: envelope(response(scenario)) }).catch(() => {}) // Aborted A is expected.
  })
  await page.goto(PATH)
  await expect(page.getByRole('status')).toHaveText('Đang tải dữ liệu mô phỏng…')
  await page.getByLabel('Kịch bản mô phỏng', { exact: true }).selectOption('HOTEL_B')
  await expect(page.getByRole('img')).toBeVisible()
  release()
  await expect(page.getByLabel('Kịch bản mô phỏng', { exact: true })).toHaveValue('HOTEL_B')
  await expect(page.getByRole('row', { name: '30/10/2026 80% 75% 85%' })).toBeVisible()
})

for (const [status, message] of [[404, 'Demo chưa được bật'], [403, 'Quyền truy cập demo đã bị từ chối'], [503, 'Dịch vụ demo tạm thời chưa sẵn sàng'], [500, 'Không tải được dữ liệu demo hợp lệ']] as const) {
  test(`safe ${status} error clears data and supports retry without backend text`, async ({ page }) => {
    await signIn(page)
    let failing = false
    await page.route(API, route => route.fulfill(failing ? { status, json: { message: '<script>PRIVATE-ERROR</script>' } } : { json: envelope(response()) }))
    await page.goto(PATH)
    await expect(page.getByRole('img')).toBeVisible()
    failing = true
    await page.getByRole('button', { name: 'Tải lại', exact: true }).click()
    await expect(page.getByRole('alert')).toContainText(message)
    await expect(page.getByRole('img')).toHaveCount(0)
    await expect(page.getByText('PRIVATE-ERROR', { exact: false })).toHaveCount(0)
    failing = false
    await page.getByRole('button', { name: 'Thử lại', exact: true }).click()
    await expect(page.getByRole('img')).toBeVisible()
  })
}

for (const kind of ['business', 'scenario', 'bounds', 'date', 'provenance', 'counts'] as const) {
  test(`rejects malformed ${kind} response without chart or tables`, async ({ page }) => {
    await signIn(page)
    const value = response()
    if (kind === 'business') value.businessData = true
    if (kind === 'scenario') value.scenario = 'HOTEL_B'
    if (kind === 'bounds') value.forecast.points[0].upperPercent = 101
    if (kind === 'date') value.forecast.points[0].date = '2026-02-30'
    if (kind === 'provenance') value.forecast.dataMode = 'BUSINESS'
    if (kind === 'counts') value.history.samples[0].occupiedRooms = -1
    await page.route(API, route => route.fulfill({ json: envelope(value) }))
    await page.goto(PATH)
    await expect(page.getByRole('alert')).toContainText('Không tải được dữ liệu demo hợp lệ')
    await expect(page.getByRole('img')).toHaveCount(0)
    await expect(page.getByRole('table')).toHaveCount(0)
  })
}

test('insufficient data does not invent a forecast; baseline fallback is explicit', async ({ page }) => {
  await signIn(page)
  const value = response()
  value.forecast.status = 'BASELINE_FALLBACK'
  await page.route(API, route => route.fulfill({ json: envelope(value) }))
  await page.goto(PATH)
  await expect(page.getByText('Dùng mô hình nền', { exact: true })).toBeVisible()
  value.forecast.status = 'INSUFFICIENT_COVERAGE'
  value.forecast.model = null
  value.forecast.residualRadiusPp = null
  value.forecast.points = []
  await page.getByRole('button', { name: 'Tải lại', exact: true }).click()
  await expect(page.getByText('Chưa đủ dữ liệu để dự báo', { exact: true })).toBeVisible()
  await expect(page.getByRole('table')).toHaveCount(0)
  await expect(page.getByTestId('occupancy-forecast-line')).toHaveAttribute('d', '')
})

test('anonymous deep link requires login without demo request', async ({ page }) => {
  let calls = 0
  page.on('request', request => { if (request.url().includes('/api/v1/ai/demo/occupancy')) calls++ })
  await page.goto(PATH)
  await expect(page).toHaveURL(/\/login$/)
  expect(calls).toBe(0)
})

test('network failure has no fixture fallback and can retry', async ({ page }) => {
  await signIn(page)
  let disconnected = true
  await page.route(API, route => disconnected ? route.abort('failed') : route.fulfill({ json: envelope(response()) }))
  await page.goto(PATH)
  await expect(page.getByRole('alert')).toContainText('Không tải được dữ liệu demo hợp lệ')
  await expect(page.getByRole('img')).toHaveCount(0)
  disconnected = false
  await page.getByRole('button', { name: 'Thử lại', exact: true }).click()
  await expect(page.getByRole('img')).toBeVisible()
})

test('expired server session clears displayed data and returns to login', async ({ page }) => {
  await signIn(page)
  let expired = false
  await page.route(API, route => route.fulfill(expired ? { status: 401, json: { message: 'expired' } } : { json: envelope(response()) }))
  await page.goto(PATH)
  await expect(page.getByRole('img')).toBeVisible()
  expired = true
  await page.getByRole('button', { name: 'Tải lại', exact: true }).click()
  await expect(page).toHaveURL(/\/login\?session=expired$/)
  await expect(page.getByRole('img')).toHaveCount(0)
  expect(await page.evaluate(() => localStorage.getItem('auth_token'))).toBeNull()
})

for (const role of ['CUSTOMER', 'RECEPTIONIST']) {
  test(`${role} cannot load page or send a demo request`, async ({ page }) => {
    await signIn(page, role)
    let calls = 0
    page.on('request', request => { if (request.url().includes('/api/v1/ai/demo/occupancy')) calls++ })
    await page.goto(PATH)
    await expect(page.getByRole('heading', { name: '403 - Quyền truy cập bị hạn chế' })).toBeVisible()
    expect(calls).toBe(0)
  })
}
