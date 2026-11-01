import { expect, test } from '@playwright/test'
import { execFileSync } from 'node:child_process'
import { getKeycloakToken, BACKEND_URL } from '../helpers/auth.helper'
import { getAcceptanceDatabaseDigest, getAcceptancePostgresContainerId } from '../helpers/acceptance-db.helper'

const enabled = process.env.U22_UTILITY_DEMO_ENABLED === 'true'
const url = `${BACKEND_URL}/api/v1/ai/demo/utility-anomalies`
const scenarios = ['ELECTRIC_FLAT', 'WATER_FLAT', 'ELECTRIC_SPIKES', 'WATER_SPIKES', 'ZERO_BOUNDARY', 'MAD_NOISE', 'GAP', 'RESET', 'REPLACEMENT', 'NEGATIVE_DROP', 'LOW_COVERAGE', 'SLOW_VARIATION']

function digest() {
  if (BACKEND_URL !== 'http://localhost:18080') throw new Error('Fixed synthetic acceptance target required')
  const pg = getAcceptancePostgresContainerId()
  if (!/^[a-f0-9]+$/.test(pg) || execFileSync('docker', ['port', pg, '5432/tcp'], { encoding: 'utf8' }).trim() !== '127.0.0.1:15432') throw new Error('Owned loopback DB required')
  return getAcceptanceDatabaseDigest()
}

test('anonymous cannot read utility replay', async ({ request }) => {
  expect((await request.get(url)).status()).toBe(401)
})

test('real Keycloak admin reads all12 fixed synthetic scenarios or404, with zero business writes', async ({ request }) => {
  const token = await getKeycloakToken('admin')
  const headers = { Authorization: `Bearer ${token}` }
  const before = digest()
  for (const scenario of scenarios) {
    const response = await request.get(url, { params: { scenario }, headers })
    expect(response.status()).toBe(enabled ? 200 : 404)
    if (!enabled) continue
    const body = await response.json()
    expect(body.code).toBe(200)
    const result = body.result
    expect(result.dataMode).toBe('SYNTHETIC')
    expect(result.businessData).toBe(false)
    expect(result.scenario).toBe(scenario)
    expect(result.referenceInstant).toBe('2026-10-30T17:00:00Z')
    expect(result.notice).toContain('không phải kết luận gian lận')
    expect(result.scenarios.map((row: { id: string }) => row.id)).toEqual(scenarios)
    expect(result.analysis.dataMode).toBe('SYNTHETIC')
    expect(result.analysis.modelVersion).toBe('u22-causal-median-mad-v1')
    expect(result.analysis.fromInclusive).toBe('2026-09-19')
    expect(result.analysis.toInclusive).toBe('2026-10-30')
    expect(result.analysis.points).toHaveLength(42)
    expect(result.analysis.unit).toBe(['WATER_FLAT', 'WATER_SPIKES', 'GAP', 'REPLACEMENT', 'LOW_COVERAGE'].includes(scenario) ? 'M3' : 'KWH')
    expect(result.counts.evaluated + result.counts.insufficient + result.counts.excluded).toBe(42)
    expect(result.counts.valid).toBe(result.counts.evaluated + result.counts.insufficient)
    expect(result.counts.needsReview).toBeLessThanOrEqual(result.counts.evaluated)
    for (const point of result.analysis.points) {
      if (point.status === 'EXCLUDED') expect(point.usage).toBeNull()
      if (point.status === 'NEEDS_REVIEW') expect(point.label).toBe('Cần kiểm tra')
      if (point.status === 'NORMAL' || point.status === 'NEEDS_REVIEW') expect(point.referenceCount).toBeGreaterThanOrEqual(28)
    }
    expect(JSON.stringify(result)).not.toContain('"expected"')
    const repeated = await request.get(url, { params: { scenario }, headers })
    expect(repeated.status()).toBe(200)
    expect(await repeated.json()).toEqual(body)
  }
  expect(digest()).toBe(before)
})

test('customer and reception cannot read utility replay', async ({ request }) => {
  for (const username of ['test_customer', 'reception_h1']) {
    const token = await getKeycloakToken(username)
    expect((await request.get(url, { headers: { Authorization: `Bearer ${token}` } })).status()).toBe(enabled ? 403 : 404)
  }
})

test('admin cannot supply business selectors, arbitrary or duplicate scenarios', async ({ request }) => {
  const token = await getKeycloakToken('admin')
  for (const query of ['scenario=OTHER', 'scenario=electric_spikes', 'scenario=', 'scenario=GAP&scenario=RESET', 'hotelId=1', 'meterId=1', 'regionId=1', 'role=CHAIN_ADMIN', 'unit=M3', 'clock=2026-10-30', 'url=https%3A%2F%2Fexample.test']) {
    const response = await request.get(`${url}?${query}`, { headers: { Authorization: `Bearer ${token}` } })
    expect(response.status()).toBe(enabled ? 400 : 404)
    if (enabled) expect((await response.json()).message).toBe('Kịch bản demo không hợp lệ.')
  }
})

test('default, excluded/reset and insufficient coverage remain explicit', async ({ request }) => {
  const token = await getKeycloakToken('admin')
  const headers = { Authorization: `Bearer ${token}` }
  const defaultResponse = await request.get(url, { headers })
  expect(defaultResponse.status()).toBe(enabled ? 200 : 404)
  if (!enabled) return
  expect((await defaultResponse.json()).result.scenario).toBe('ELECTRIC_SPIKES')
  const flat = (await (await request.get(url, { headers, params: { scenario: 'ELECTRIC_FLAT' } })).json()).result
  expect(flat.counts).toEqual({ valid: 42, evaluated: 14, needsReview: 0, excluded: 0, insufficient: 28 })
  expect(flat.analysis.points[28].median).toBe(10)
  expect(flat.analysis.points[28].mad).toBe(0)
  expect(flat.analysis.points[28].threshold).toBe(2)
  const low = (await (await request.get(url, { headers, params: { scenario: 'LOW_COVERAGE' } })).json()).result
  expect(low.analysis.status).toBe('INSUFFICIENT_COVERAGE')
  expect(low.counts.evaluated).toBe(0)
  expect(low.analysis.points[0].status).toBe('EXCLUDED')
  const reset = (await (await request.get(url, { headers, params: { scenario: 'RESET' } })).json()).result
  expect(reset.analysis.points.some((point: { reasonCode: string }) => point.reasonCode === 'RESET')).toBe(true)
  expect(reset.counts.needsReview).toBe(0)
})
