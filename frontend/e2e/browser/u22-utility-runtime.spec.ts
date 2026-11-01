import { expect, test } from '@playwright/test'
import { execFileSync } from 'node:child_process'
import { getKeycloakToken, BACKEND_URL } from '../helpers/auth.helper'
import { getAcceptanceDatabaseDigest, getAcceptancePostgresContainerId } from '../helpers/acceptance-db.helper'

test('real Keycloak admin views all12 utility scenarios, correct values and no business writes, or disabled state', async ({ page }) => {
  test.setTimeout(60_000)
  if (BACKEND_URL !== 'http://localhost:18080') throw new Error('Fixed synthetic target required')
  const pg = getAcceptancePostgresContainerId()
  if (!/^[a-f0-9]+$/.test(pg) || execFileSync('docker', ['port', pg, '5432/tcp'], { encoding: 'utf8' }).trim() !== '127.0.0.1:15432') throw new Error('Owned loopback DB required')
  const token = await getKeycloakToken('admin')
  await page.addInitScript(token => localStorage.setItem('auth_token', token), token)
  await page.goto('/dashboard')
  const before = getAcceptanceDatabaseDigest()
  const methods: string[] = []
  page.on('request', r => { if (new URL(r.url()).pathname === '/api/v1/ai/demo/utility-anomalies') methods.push(r.method()) })
  const nextResponse = () => page.waitForResponse(r => new URL(r.url()).pathname === '/api/v1/ai/demo/utility-anomalies')
  const first = nextResponse()
  await page.getByRole('link', { name: 'Điện/nước bất thường (Demo)', exact: true }).click()
  const initial = await first
  const enabled = process.env.U22_UTILITY_DEMO_ENABLED === 'true'
  expect(initial.status()).toBe(enabled ? 200 : 404)
  if (enabled) {
    const catalog = (await initial.json()).result.scenarios as { id: string }[]
    for (const { id } of catalog) {
      const replay = nextResponse()
      if (id === 'ELECTRIC_SPIKES' && await page.getByLabel('Kịch bản điện/nước').inputValue() === id) await page.getByRole('button', { name: 'Tải lại', exact: true }).click()
      else await page.getByLabel('Kịch bản điện/nước', { exact: true }).selectOption(id)
      const response = await replay
      expect(response.status()).toBe(200)
      const result = (await response.json()).result
      await expect(page.getByRole('img')).toBeVisible()
      await expect(page.getByRole('row')).toHaveCount(43)
      await expect(page.getByTestId('utility-review-marker')).toHaveCount(result.counts.needsReview)
      const point = result.analysis.points.at(-1)
      const row = page.getByRole('row').filter({ hasText: '30/10/2026' })
      await expect(row.getByRole('cell').first()).toHaveText(point.usage === null ? '—' : new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 8 }).format(point.usage))
      await expect(page.getByRole('status')).toContainText(result.analysis.unit === 'KWH' ? 'kWh' : 'm³')
    }
  } else {
    await expect(page.getByRole('alert')).toContainText('Demo điện/nước chưa được bật')
    await expect(page.getByRole('table')).toHaveCount(0)
  }
  expect(methods.length).toBeGreaterThan(0)
  expect(methods.every(method => method === 'GET')).toBe(true)
  expect(getAcceptanceDatabaseDigest()).toBe(before)
})

test('real customer and reception cannot see utility link or deep-link data', async ({ page }) => {
  for (const username of ['test_customer', 'reception_h1']) {
    const token = await getKeycloakToken(username)
    await page.goto('/login')
    await page.evaluate(token => localStorage.setItem('auth_token', token), token)
    await page.goto('/dashboard')
    await expect(page.getByRole('link', { name: 'Điện/nước bất thường (Demo)', exact: true })).toHaveCount(0)
    await page.goto('/ai/demo/utility-anomalies')
    await expect(page.getByRole('heading', { name: '403 - Quyền truy cập bị hạn chế' })).toBeVisible()
  }
})
