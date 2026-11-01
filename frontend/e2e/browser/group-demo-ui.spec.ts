import { expect, test, type Page } from '@playwright/test'
import { readFileSync } from 'node:fs'
import type { GroupDemo } from '../../src/modules/analytics/services/groupDemoService'

const PATH = '/ai/demo/group-room-assignment', API = '**/api/v1/ai/demo/group-room-assignment*'
const cases = (JSON.parse(readFileSync(new URL('../../../backend/src/main/resources/ai/demo/group-preview-v1.json', import.meta.url), 'utf8')) as { cases: ({ id: string } & Pick<GroupDemo, 'request' | 'snapshot'>)[] }).cases
const aliases = ['FIT_AND_TIE', 'QUOTA_ONE', 'MIDDLE_NIGHT_LIMIT', 'ZERO_QUOTA', 'QUOTA_CONTROL', 'NO_TRANSFER_BLOCK', 'NO_TRANSFER_ROOM_TYPE']
const sourceIds = ['P01', 'P11_QUOTA_ONE', 'P12_MIDDLE_BOTTLENECK', 'P14_ZERO', 'P14_CONTROL', 'P14_NO_TRANSFER_roomBlockId', 'P14_NO_TRANSFER_hotelRoomTypeId']
const roomOracles = [[11, 12], [11], [11], [], [11, 12], [12], [12]]
function mockData(selected = 'FIT_AND_TIE'): GroupDemo {
  const index = aliases.indexOf(selected)
  if (index < 0) throw Error('Unknown test alias')
  const source = cases.find(row => row.id === sourceIds[index])!
  const { request, snapshot } = structuredClone(source)
  const assignments = roomOracles[index].map((roomInstanceId, i) => ({ demandKey: i === 0 ? 'A' : 'B', roomInstanceId }))
  const unmet = request.demands.filter(demand => !assignments.some(row => row.demandKey === demand.demandKey)).map(demand => ({ demandKey: demand.demandKey, reason: 'NO_ELIGIBLE_CAPACITY' as const }))
  return { dataMode: 'SYNTHETIC', businessData: false, previewOnly: true, scenario: selected as GroupDemo['scenario'],
    referenceInstant: '2026-10-31T02:00:00Z', notice: 'Synthetic preview', scenarios: aliases.map(id => ({ id: id as GroupDemo['scenario'], label: `Synthetic ${id}` })),
    request, snapshot, allDemandsAssigned: unmet.length === 0,
    preview: { status: 'COMPLETE', reasonCode: null, dataMode: 'SYNTHETIC', hotelId: 1, contractId: 501,
      checkInDate: request.checkInDate, checkOutDate: request.checkOutDate, observedAt: snapshot.observedAt, previewOnly: true,
      assignments, unmet, warnings: [], objective: { assignedCount: assignments.length, bedWaste: 0, floorSpan: 0, floorObjectiveEnabled: true } } }
}
const envelope = (result: unknown) => ({ code: 200, message: 'ok', result })
async function signIn(page: Page, role = 'CHAIN_ADMIN') {
  const token = `header.${Buffer.from(JSON.stringify({ sub: 'synthetic-group-ui', realm_access: { roles: [role] } })).toString('base64url')}.signature`
  await page.addInitScript(token => {
    if (!sessionStorage.getItem('group-test-seeded')) {
      localStorage.setItem('auth_token', token); sessionStorage.setItem('group-test-seeded', 'true')
    }
  }, token)
}

test('mobile and desktop show all7 independent room oracles, zero quota and keyboard reload without writes', async ({ page }, info) => {
  await signIn(page)
  await page.setViewportSize({ width: 375, height: 812 })
  const calls: string[] = []
  await page.route(API, route => {
    expect(route.request().method()).toBe('GET')
    const params = new URL(route.request().url()).searchParams
    expect([...params.keys()]).toEqual(['scenario'])
    const selected = params.get('scenario')!
    calls.push(selected)
    return route.fulfill({ json: envelope(mockData(selected)) })
  })
  await page.goto(`${PATH}?hotelId=9&action=APPLY&scenario=ZERO_QUOTA`)
  await expect(page.getByRole('heading', { name: 'Phương án phân phòng Group', exact: true })).toBeVisible()
  await expect(page.getByText('DEMO · DỮ LIỆU GIẢ LẬP', { exact: true })).toBeVisible()
  await expect(page.getByRole('table')).toHaveCount(3)
  await expect(page.getByRole('status')).toContainText('Đủ phương án')
  expect(calls[0]).toBe('FIT_AND_TIE')
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  const region = page.getByRole('region', { name: 'Phương án theo yêu cầu', exact: true })
  await region.focus(); await page.keyboard.press('ArrowRight')
  await expect.poll(() => region.evaluate(element => element.scrollLeft)).toBeGreaterThan(0)
  await page.screenshot({ path: info.outputPath('group-mobile.png'), fullPage: true })
  await page.setViewportSize({ width: 1440, height: 1000 })
  for (let i = 0; i < aliases.length; i++) {
    await page.getByLabel('Kịch bản Group', { exact: true }).selectOption(aliases[i])
    await expect(page.getByRole('table')).toHaveCount(3)
    const demands = page.getByRole('region', { name: 'Phương án theo yêu cầu', exact: true }).getByRole('row')
    await expect(demands).toHaveCount(3)
    for (const [index, key] of ['A', 'B'].entries()) {
      const expected = roomOracles[i][index]
      await expect(page.getByTestId(`group-demand-${key}`).getByRole('cell').nth(2)).toHaveText(expected === undefined ? '—' : `#${expected}`)
    }
    await expect(page.getByRole('status')).toContainText(i === 0 || i === 4 ? 'Đủ phương án' : 'chưa có phương án')
  }
  await page.getByRole('combobox', { name: 'Kịch bản Group', exact: true }).selectOption('ZERO_QUOTA')
  await expect(page.getByRole('status')).toContainText('Còn 2 yêu cầu')
  await page.getByRole('button', { name: 'Tải lại', exact: true }).focus(); await page.keyboard.press('Enter')
  await expect(page.getByRole('table')).toHaveCount(3)
  expect(calls.filter(id => id === 'ZERO_QUOTA').length).toBeGreaterThanOrEqual(2)
  await page.screenshot({ path: info.outputPath('group-desktop.png'), fullPage: true })
})

test('cancelled late response cannot replace the newly selected zero-quota plan', async ({ page }) => {
  await signIn(page)
  let release!: () => void
  const gate = new Promise<void>(resolve => { release = resolve })
  let handled!: () => void
  const oldHandled = new Promise<void>(resolve => { handled = resolve })
  await page.route(API, async route => {
    const id = new URL(route.request().url()).searchParams.get('scenario')!
    if (id === 'FIT_AND_TIE') await gate
    await route.fulfill({ json: envelope(mockData(id)) }).catch(() => {})
    if (id === 'FIT_AND_TIE') handled()
  })
  await page.goto(PATH)
  await expect(page.getByRole('status')).toContainText('Đang tính')
  await page.getByRole('combobox', { name: 'Kịch bản Group', exact: true }).selectOption('ZERO_QUOTA')
  await expect(page.getByRole('status')).toContainText('Còn 2 yêu cầu')
  release()
  await oldHandled
  await page.evaluate(() => new Promise<void>(resolve => requestAnimationFrame(() => resolve())))
  await expect(page.getByTestId('group-demand-A').getByRole('cell').nth(2)).toHaveText('—')
  await expect(page.getByRole('status')).not.toContainText('Đủ phương án')
})

for (const status of [403, 404, 503, 500]) {
  test(`safe ${status} clears data and retry recovers`, async ({ page }) => {
    await signIn(page)
    let failed = false
    await page.route(API, route => route.fulfill(failed ? { status, json: { message: 'PRIVATE-ERROR' } } : { json: envelope(mockData()) }))
    await page.goto(PATH); await expect(page.getByRole('table')).toHaveCount(3)
    failed = true; await page.getByRole('button', { name: 'Tải lại', exact: true }).click()
    await expect(page.getByRole('alert')).toBeVisible(); await expect(page.getByRole('table')).toHaveCount(0)
    await expect(page.getByText('PRIVATE-ERROR')).toHaveCount(0)
    failed = false; await page.getByRole('button', { name: 'Thử lại', exact: true }).click()
    await expect(page.getByRole('table')).toHaveCount(3)
  })
}

const malformed: Record<string, (data: GroupDemo) => void> = {
  business: data => { Object.assign(data, { businessData: true }) },
  previewOnly: data => { Object.assign(data.preview, { previewOnly: false }) },
  scenario: data => { data.scenario = 'ZERO_QUOTA' },
  clock: data => { Object.assign(data, { referenceInstant: '2026-11-01T00:00:00Z' }) },
  scope: data => { Object.assign(data.snapshot.scope, { hotelId: 2 }) },
  coverage: data => { data.snapshot.quotaRows.pop() },
  catalog: data => { data.scenarios.reverse() },
  night: data => { data.snapshot.nightEligibility[0].state = 'ROOM_UNAVAILABLE' },
  duplicateRoom: data => { data.preview.assignments[1].roomInstanceId = 11 },
  unknownRoom: data => { data.preview.assignments[0].roomInstanceId = 999 },
  partition: data => { data.preview.unmet.push({ demandKey: 'A', reason: 'NO_ELIGIBLE_CAPACITY' }) },
  objective: data => { data.preview.objective.assignedCount = 0 },
  quota: data => { data.snapshot.quotaRows[0].previewableRoomCount = 0 },
  beds: data => { data.snapshot.candidates[0].bedUnits = 0 },
  floor: data => { data.preview.objective.floorSpan = 3 },
  waste: data => { data.preview.objective.bedWaste = 1 },
  complete: data => { data.allDemandsAssigned = false },
  dates: data => { data.preview.checkOutDate = '2026-11-04' },
}
for (const [name, mutate] of Object.entries(malformed)) {
  test(`rejects inconsistent ${name} before rendering any plan`, async ({ page }) => {
    await signIn(page)
    const data = mockData(); mutate(data)
    await page.route(API, route => route.fulfill({ json: envelope(data) }))
    await page.goto(PATH)
    await expect(page.getByRole('alert')).toContainText('Không tải được phương án demo hợp lệ')
    await expect(page.getByRole('table')).toHaveCount(0)
  })
}
test('network failure has no data fallback', async ({ page }) => {
  await signIn(page); await page.route(API, route => route.abort('connectionfailed'))
  await page.goto(PATH); await expect(page.getByRole('alert')).toBeVisible(); await expect(page.getByRole('table')).toHaveCount(0)
})
test('expired session clears token and returns to login', async ({ page }) => {
  await signIn(page); await page.route(API, route => route.fulfill({ status: 401, json: { message: 'PRIVATE-ERROR' } }))
  await page.goto(PATH); await expect(page).toHaveURL(/\/login\?session=expired$/)
  expect(await page.evaluate(() => localStorage.getItem('auth_token'))).toBeNull()
  await expect(page.getByRole('table')).toHaveCount(0)
})
for (const role of ['ANONYMOUS', 'CUSTOMER', 'RECEPTIONIST']) {
  test(`${role} cannot request Group data through the page`, async ({ page }) => {
    if (role !== 'ANONYMOUS') await signIn(page, role)
    let calls = 0
    await page.route(API, route => { calls++; return route.fulfill({ json: envelope(mockData()) }) })
    await page.goto(PATH)
    if (role === 'ANONYMOUS') await expect(page).toHaveURL(/\/login$/)
    else await expect(page.getByRole('heading', { name: '403 - Quyền truy cập bị hạn chế' })).toBeVisible()
    expect(calls).toBe(0)
    await expect(page.getByRole('table')).toHaveCount(0)
  })
}
