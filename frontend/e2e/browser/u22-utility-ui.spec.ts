import { expect, test, type Page } from '@playwright/test'

const PATH = '/ai/demo/utility-anomalies', API = '**/api/v1/ai/demo/utility-anomalies*'
const ids = ['ELECTRIC_FLAT', 'WATER_FLAT', 'ELECTRIC_SPIKES', 'WATER_SPIKES', 'ZERO_BOUNDARY', 'MAD_NOISE', 'GAP', 'RESET', 'REPLACEMENT', 'NEGATIVE_DROP', 'LOW_COVERAGE', 'SLOW_VARIATION']
const water = new Set(['WATER_FLAT', 'WATER_SPIKES', 'GAP', 'REPLACEMENT', 'LOW_COVERAGE'])
function mockData(scenario = 'ELECTRIC_SPIKES') {
  let references = 0
  const points = Array.from({ length: 42 }, (_, i) => {
    const excluded = scenario === 'LOW_COVERAGE' && i < 25 || ['ELECTRIC_SPIKES', 'GAP'].includes(scenario) && [2, 3].includes(i) || ['RESET', 'REPLACEMENT'].includes(scenario) && i === 32
    const barrier = ['RESET', 'REPLACEMENT'].includes(scenario) && i === 32
    if (barrier) references = 0
    const scored = !excluded && references >= 28
    const review = scored && (scenario.includes('SPIKES') && i === 32 || scenario === 'ZERO_BOUNDARY' && i === 29)
    const base = scenario === 'ZERO_BOUNDARY' ? 0 : water.has(scenario) ? 5 : 10
    const usage = review ? scenario === 'ZERO_BOUNDARY' ? 0.002 : base * 3 : scenario === 'ZERO_BOUNDARY' && i === 28 ? 0.001 : base
    const point = {
      date: new Date(Date.UTC(2026, 8, 19 + i)).toISOString().slice(0, 10),
      status: excluded ? 'EXCLUDED' : scored ? review ? 'NEEDS_REVIEW' : 'NORMAL' : 'INSUFFICIENT_COVERAGE',
      usage: excluded ? null : usage, referenceCount: references,
      median: scored ? base : null, mad: scored ? 0 : null, threshold: scored ? scenario === 'ZERO_BOUNDARY' ? 0.001 : base * 0.2 : null,
      reasonCode: excluded ? barrier ? scenario : i === 3 || scenario === 'LOW_COVERAGE' && i === 24 ? 'MISSING_PREDECESSOR' : 'MISSING_READING' : scored ? review ? 'ROBUST_DEVIATION' : null : 'FEWER_THAN_28_VALID_PRIOR_POINTS',
      label: review ? 'Cần kiểm tra' : null,
    }
    if (!excluded) references++
    return point
  })
  const evaluated = points.filter(p => p.status === 'NORMAL' || p.status === 'NEEDS_REVIEW').length
  return {
    dataMode: 'SYNTHETIC', businessData: false, scenario, referenceInstant: '2026-10-30T17:00:00Z',
    scenarios: ids.map(id => ({ id, label: `Synthetic ${id}`, unit: water.has(id) ? 'M3' : 'KWH' })),
    analysis: { status: evaluated ? 'ANALYZED' : 'INSUFFICIENT_COVERAGE', reasonCode: evaluated ? null : 'INSUFFICIENT_VALID_REFERENCE', hotelId: water.has(scenario) ? 2 : 1, meterId: ids.indexOf(scenario) + 1, unit: water.has(scenario) ? 'M3' : 'KWH',
      dataMode: 'SYNTHETIC', modelVersion: 'u22-causal-median-mad-v1', asOf: '2026-10-30T17:00:00Z', fromInclusive: '2026-09-19', toInclusive: '2026-10-30', points },
    counts: { valid: points.filter(p => p.usage !== null).length, evaluated, needsReview: points.filter(p => p.status === 'NEEDS_REVIEW').length, excluded: points.filter(p => p.status === 'EXCLUDED').length, insufficient: points.filter(p => p.status === 'INSUFFICIENT_COVERAGE').length },
  }
}
async function signIn(page: Page, role = 'CHAIN_ADMIN') {
  const token = `header.${Buffer.from(JSON.stringify({ sub: 'synthetic-utility-ui', realm_access: { roles: [role] } })).toString('base64url')}.signature`
  await page.addInitScript(token => {
    if (!sessionStorage.getItem('utility-test-seeded')) {
      localStorage.setItem('auth_token', token); sessionStorage.setItem('utility-test-seeded', 'true')
    }
  }, token)
}
const envelope = (result: unknown) => ({ code: 200, message: 'ok', result })

test('mobile table, gap, review marker, unit change and keyboard reload are read-only', async ({ page }, info) => {
  await signIn(page)
  await page.setViewportSize({ width: 375, height: 812 })
  await page.emulateMedia({ reducedMotion: 'reduce' })
  const calls: string[] = []
  await page.route(API, route => {
    expect(route.request().method()).toBe('GET')
    const params = new URL(route.request().url()).searchParams
    expect([...params.keys()]).toEqual(['scenario'])
    calls.push(params.get('scenario')!)
    return route.fulfill({ json: envelope(mockData(params.get('scenario')!)) })
  })
  await page.goto(PATH)
  await expect(page.getByRole('heading', { name: 'Kiểm tra bất thường điện/nước', exact: true })).toBeVisible()
  await expect(page.getByText('DEMO · DỮ LIỆU GIẢ LẬP', { exact: true })).toBeVisible()
  await expect(page.getByRole('img')).toBeVisible()
  await expect(page.getByRole('row')).toHaveCount(43)
  await expect(page.getByTestId('utility-review-marker')).toHaveCount(1)
  expect((await page.getByTestId('utility-usage-line').getAttribute('d'))!.match(/M/g)).toHaveLength(2)
  await expect(page.getByRole('row').filter({ hasText: '21/09/2026' })).toContainText('Thiếu chỉ số')
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
  await page.screenshot({ path: info.outputPath('utility-mobile.png'), fullPage: true })
  await page.setViewportSize({ width: 1440, height: 1000 })
  await page.getByLabel('Kịch bản điện/nước', { exact: true }).selectOption('WATER_FLAT')
  await expect(page.getByRole('status')).toContainText('Đơn vị: m³')
  await page.getByRole('button', { name: 'Tải lại', exact: true }).focus()
  await page.keyboard.press('Enter')
  await expect(page.getByRole('img')).toBeVisible()
  expect(calls.filter(id => id === 'WATER_FLAT').length).toBeGreaterThanOrEqual(2)
  await page.screenshot({ path: info.outputPath('utility-desktop.png'), fullPage: true })
})

test('loading and cancelled late response cannot overwrite the new unit/scenario', async ({ page }) => {
  await signIn(page)
  let release!: () => void
  const gate = new Promise<void>(resolve => { release = resolve })
  await page.route(API, async route => {
    const scenario = new URL(route.request().url()).searchParams.get('scenario')!
    if (scenario === 'ELECTRIC_SPIKES') await gate
    await route.fulfill({ json: envelope(mockData(scenario)) }).catch(() => {})
  })
  await page.goto(PATH)
  await expect(page.getByRole('status')).toContainText('Đang tải')
  await page.getByLabel('Kịch bản điện/nước', { exact: true }).selectOption('WATER_FLAT')
  await expect(page.getByRole('status')).toContainText('Đơn vị: m³')
  release()
  await expect(page.getByRole('row').filter({ hasText: '19/09/2026' }).getByRole('cell').first()).toHaveText('5')
})

for (const status of [403, 404, 503, 500]) {
  test(`safe ${status} clears stale data and retry recovers`, async ({ page }) => {
    await signIn(page)
    let failed = false
    await page.route(API, route => route.fulfill(failed ? { status, json: { message: 'PRIVATE-ERROR' } } : { json: envelope(mockData()) }))
    await page.goto(PATH)
    await expect(page.getByRole('img')).toBeVisible()
    failed = true
    await page.getByRole('button', { name: 'Tải lại', exact: true }).click()
    await expect(page.getByRole('alert')).toBeVisible()
    if (status === 404) await expect(page.getByRole('alert')).toContainText('Demo điện/nước chưa được bật')
    await expect(page.getByRole('img')).toHaveCount(0)
    await expect(page.getByRole('table')).toHaveCount(0)
    await expect(page.getByText('PRIVATE-ERROR', { exact: true })).toHaveCount(0)
    failed = false
    await page.getByRole('button', { name: 'Thử lại', exact: true }).click()
    await expect(page.getByRole('img')).toBeVisible()
  })
}
for (const kind of ['business', 'unit', 'scenario', 'date', 'counts', 'excluded', 'references', 'provenance']) {
  test(`malformed ${kind} is rejected before chart/table`, async ({ page }) => {
    await signIn(page)
    const data = mockData()
    if (kind === 'business') data.businessData = true
    if (kind === 'unit') data.analysis.unit = 'M3'
    if (kind === 'scenario') data.scenario = 'WATER_FLAT'
    if (kind === 'date') data.analysis.points[0].date = '2026-02-30'
    if (kind === 'counts') data.counts.needsReview = 40
    if (kind === 'excluded') data.analysis.points[2].usage = 0
    if (kind === 'references') data.analysis.points[32].referenceCount = 28
    if (kind === 'provenance') data.analysis.dataMode = 'BUSINESS'
    await page.route(API, route => route.fulfill({ json: envelope(data) }))
    await page.goto(PATH)
    await expect(page.getByRole('alert')).toContainText('Không tải được dữ liệu demo hợp lệ')
    await expect(page.getByRole('img')).toHaveCount(0)
    await expect(page.getByRole('table')).toHaveCount(0)
  })
}
test('decimal floor is visible and warmup/reset/low coverage never become normal', async ({ page }) => {
  await signIn(page)
  await page.route(API, route => route.fulfill({ json: envelope(mockData(new URL(route.request().url()).searchParams.get('scenario')!)) }))
  await page.goto(PATH)
  await expect(page.getByRole('img')).toBeVisible()
  await page.getByLabel('Kịch bản điện/nước', { exact: true }).selectOption('ZERO_BOUNDARY')
  await expect(page.getByRole('row').filter({ hasText: '17/10/2026' })).toContainText('0,001')
  await expect(page.getByRole('row').filter({ hasText: '17/10/2026' })).toContainText('Trong ngưỡng')
  await expect(page.getByRole('row').filter({ hasText: '18/10/2026' })).toContainText('Cần kiểm tra')
  await page.getByLabel('Kịch bản điện/nước', { exact: true }).selectOption('RESET')
  await expect(page.getByRole('row').filter({ hasText: '21/10/2026' })).toContainText('Đặt lại đồng hồ')
  await expect(page.getByRole('row').filter({ hasText: '22/10/2026' })).toContainText('Chưa đủ tham chiếu')
  await page.getByLabel('Kịch bản điện/nước', { exact: true }).selectOption('LOW_COVERAGE')
  await expect(page.getByRole('status')).toContainText('Chưa đủ dữ liệu để đánh giá')
  await expect(page.getByTestId('utility-review-marker')).toHaveCount(0)
  await expect(page.getByRole('cell', { name: 'Trong ngưỡng', exact: true })).toHaveCount(0)
})
test('network failure has no data fallback', async ({ page }) => {
  await signIn(page)
  await page.route(API, route => route.abort('failed'))
  await page.goto(PATH)
  await expect(page.getByRole('alert')).toContainText('Không tải được dữ liệu demo hợp lệ')
  await expect(page.getByRole('table')).toHaveCount(0)
})
test('expired session returns to login and clears token/data', async ({ page }) => {
  await signIn(page)
  let expired = false
  await page.route(API, route => route.fulfill(expired ? { status: 401, json: { message: 'expired' } } : { json: envelope(mockData()) }))
  await page.goto(PATH)
  await expect(page.getByRole('img')).toBeVisible()
  expired = true
  await page.getByRole('button', { name: 'Tải lại', exact: true }).click()
  await expect(page).toHaveURL(/\/login\?session=expired$/)
  expect(await page.evaluate(() => localStorage.getItem('auth_token'))).toBeNull()
})
for (const role of ['ANONYMOUS', 'CUSTOMER', 'RECEPTIONIST']) {
  test(`${role} cannot request utility data through the page`, async ({ page }) => {
    if (role !== 'ANONYMOUS') await signIn(page, role)
    let requests = 0
    page.on('request', r => { if (r.url().includes('/api/v1/ai/demo/utility-anomalies')) requests++ })
    await page.goto(PATH)
    if (role === 'ANONYMOUS') await expect(page).toHaveURL(/\/login$/)
    else await expect(page.getByRole('heading', { name: '403 - Quyền truy cập bị hạn chế' })).toBeVisible()
    expect(requests).toBe(0)
  })
}
