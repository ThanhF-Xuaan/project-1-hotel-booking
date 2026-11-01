import { expect, test } from '@playwright/test'
import { getKeycloakToken, BACKEND_URL } from '../helpers/auth.helper'
import { getAcceptanceDatabaseDigest } from '../helpers/acceptance-db.helper'
import { execFileSync } from 'node:child_process'

test('real Keycloak admin uses sidebar, both replay scenarios and reload with zero business writes, or sees disabled state', async ({ page }) => {
  test.setTimeout(60_000)
  if (BACKEND_URL !== 'http://localhost:18080') throw new Error('Fixed synthetic sandbox required')
  const pg = execFileSync('docker', ['ps', '--filter', 'label=com.docker.compose.project=hotel-m1-acceptance', '--filter', 'label=com.docker.compose.service=acceptance_postgres', '--format', '{{.ID}}'], { encoding: 'utf8' }).trim()
  if (!/^[a-f0-9]+$/.test(pg) || execFileSync('docker', ['port', pg, '5432/tcp'], { encoding: 'utf8' }).trim() !== '127.0.0.1:15432') throw new Error('Owned loopback DB required')
  const token = await getKeycloakToken('admin')
  await page.addInitScript(token => localStorage.setItem('auth_token', token), token)
  await page.goto('/dashboard')
  await expect(page.getByRole('link', { name: 'Dự báo lấp đầy (Demo)', exact: true })).toBeVisible()
  const before = getAcceptanceDatabaseDigest()
  const calls: string[] = []
  page.on('request', request => { if (new URL(request.url()).pathname === '/api/v1/ai/demo/occupancy') calls.push(request.method()) })
  const replay = page.waitForResponse(response => new URL(response.url()).pathname === '/api/v1/ai/demo/occupancy')
  await page.getByRole('link', { name: 'Dự báo lấp đầy (Demo)', exact: true }).click()
  const actual = await replay
  const enabled = process.env.U22_DEMO_ENABLED === 'true'
  expect(actual.status()).toBe(enabled ? 200 : 404)
  if (enabled) {
    const result = (await actual.json()).result
    await expect(page.getByRole('img', { name: /Lịch sử và dự báo/ })).toBeVisible()
    const first = result.forecast.points[0]
    const displayed = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 2 }).format(first.occupancyPercent) + '%'
    await expect(page.getByRole('row').filter({ hasText: '30/10/2026' }).getByRole('cell').first()).toHaveText(displayed)
    await page.getByLabel('Kịch bản mô phỏng', { exact: true }).selectOption('HOTEL_B')
    await expect(page.getByRole('img')).toBeVisible()
    await page.getByRole('button', { name: 'Tải lại', exact: true }).click()
    await expect(page.getByRole('img')).toBeVisible()
  } else {
    await expect(page.getByRole('alert')).toContainText('Demo chưa được bật')
    await expect(page.getByRole('img')).toHaveCount(0)
  }
  expect(calls.length).toBeGreaterThanOrEqual(1)
  expect(calls.every(method => method === 'GET')).toBe(true)
  expect(getAcceptanceDatabaseDigest()).toBe(before)
})

test('real customer and reception have no sidebar demo link and cannot deep link', async ({ page }) => {
  for (const user of ['test_customer', 'reception_h1']) {
    const token = await getKeycloakToken(user)
    await page.goto('/login')
    await page.evaluate(token => localStorage.setItem('auth_token', token), token)
    await page.goto('/dashboard')
    await expect(page.getByRole('link', { name: 'Dự báo lấp đầy (Demo)', exact: true })).toHaveCount(0)
    await page.goto('/ai/demo/occupancy')
    await expect(page.getByRole('heading', { name: '403 - Quyền truy cập bị hạn chế' })).toBeVisible()
  }
})
