import { expect, test } from '@playwright/test'
import { execFileSync } from 'node:child_process'
import { getKeycloakToken, BACKEND_URL } from '../helpers/auth.helper'
import { getAcceptanceDatabaseDigest, getAcceptancePostgresContainerId } from '../helpers/acceptance-db.helper'

const PATH = '/ai/demo/group-room-assignment', API = '/api/v1/ai/demo/group-room-assignment'
test('real Keycloak admin sees all7 Group plans and quota, read-only, or disabled state', async ({ page }) => {
  test.setTimeout(60_000)
  if (BACKEND_URL !== 'http://localhost:18080') throw Error('Fixed synthetic target required')
  const pg = getAcceptancePostgresContainerId()
  if (!/^[a-f0-9]+$/.test(pg) || execFileSync('docker', ['port', pg, '5432/tcp'], { encoding: 'utf8' }).trim() !== '127.0.0.1:15432') throw Error('Owned loopback DB required')
  const token = await getKeycloakToken('admin')
  await page.addInitScript(token => localStorage.setItem('auth_token', token), token)
  await page.goto('/dashboard')
  const before = getAcceptanceDatabaseDigest()
  const methods: string[] = []
  page.on('request', request => { if (new URL(request.url()).pathname === API) methods.push(request.method()) })
  const next = () => page.waitForResponse(response => new URL(response.url()).pathname === API)
  const initial = next()
  await page.getByRole('link', { name: 'Phân phòng Group (Demo)', exact: true }).click()
  const response = await initial
  const enabled = process.env.GROUP_DEMO_ENABLED === 'true'
  expect(response.status()).toBe(enabled ? 200 : 404)
  if (enabled) {
    const catalog = (await response.json()).result.scenarios as { id: string }[]
    expect(catalog).toHaveLength(7)
    for (const { id } of catalog) {
      const pending = next()
      if (await page.getByLabel('Kịch bản Group', { exact: true }).inputValue() === id) await page.getByRole('button', { name: 'Tải lại', exact: true }).click()
      else await page.getByLabel('Kịch bản Group', { exact: true }).selectOption(id)
      const replay = await pending
      expect(replay.status()).toBe(200)
      const result = (await replay.json()).result
      await expect(page.getByRole('table')).toHaveCount(3)
      for (const key of ['A', 'B']) {
        const assignment = result.preview.assignments.find((row: { demandKey: string }) => row.demandKey === key)
        await expect(page.getByTestId(`group-demand-${key}`).getByRole('cell').nth(2)).toHaveText(assignment ? `#${assignment.roomInstanceId}` : '—')
      }
      const quota = page.getByRole('region', { name: 'Quota từng đêm', exact: true })
      await expect(quota.getByRole('row')).toHaveCount(result.snapshot.quotaRows.length + 1)
      const first = result.snapshot.quotaRows[0]
      await expect(quota.getByRole('row').nth(1).getByRole('cell').nth(2)).toHaveText(String(first.previewableRoomCount))
      await expect(page.getByRole('status')).toContainText(result.allDemandsAssigned ? 'Đủ phương án' : `Còn ${result.preview.unmet.length} yêu cầu`)
      await expect(page.getByText('DEMO · DỮ LIỆU GIẢ LẬP', { exact: true })).toBeVisible()
    }
  } else {
    await expect(page.getByRole('alert')).toContainText('Demo Group chưa được bật')
    await expect(page.getByRole('table')).toHaveCount(0)
  }
  expect(methods.length).toBeGreaterThan(0)
  expect(methods.every(method => method === 'GET')).toBe(true)
  expect(getAcceptanceDatabaseDigest()).toBe(before)
})

test('real anonymous, customer and reception cannot request Group data through route/sidebar', async ({ page }) => {
  let calls = 0
  page.on('request', request => { if (new URL(request.url()).pathname === API) calls++ })
  await page.goto(PATH)
  await expect(page).toHaveURL(/\/login$/)
  for (const username of ['test_customer', 'reception_h1']) {
    const token = await getKeycloakToken(username)
    await page.goto('/login')
    await page.evaluate(token => localStorage.setItem('auth_token', token), token)
    await page.goto('/dashboard')
    await expect(page.getByRole('link', { name: 'Phân phòng Group (Demo)', exact: true })).toHaveCount(0)
    await page.goto(PATH)
    await expect(page.getByRole('heading', { name: '403 - Quyền truy cập bị hạn chế' })).toBeVisible()
  }
  expect(calls).toBe(0)
})
