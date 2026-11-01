import { expect, test } from '@playwright/test'
import { execFileSync } from 'node:child_process'
import { getKeycloakToken, BACKEND_URL } from '../helpers/auth.helper'
import { getAcceptanceDatabaseDigest, getAcceptancePostgresContainerId } from '../helpers/acceptance-db.helper'

const enabled = process.env.GROUP_DEMO_ENABLED === 'true'
const url = `${BACKEND_URL}/api/v1/ai/demo/group-room-assignment`
const oracles = [
  ['FIT_AND_TIE', [11, 12], []], ['QUOTA_ONE', [11], ['B']], ['MIDDLE_NIGHT_LIMIT', [11], ['B']],
  ['ZERO_QUOTA', [], ['A', 'B']], ['QUOTA_CONTROL', [11, 12], []],
  ['NO_TRANSFER_BLOCK', [12], ['B']], ['NO_TRANSFER_ROOM_TYPE', [12], ['B']],
] as const
function digest() {
  if (BACKEND_URL !== 'http://localhost:18080') throw new Error('Fixed synthetic sandbox required')
  const pg = getAcceptancePostgresContainerId()
  if (!/^[a-f0-9]+$/.test(pg) || execFileSync('docker', ['port', pg, '5432/tcp'], { encoding: 'utf8' }).trim() !== '127.0.0.1:15432') throw new Error('Owned loopback DB required')
  return getAcceptanceDatabaseDigest()
}

test('anonymous cannot read Group preview demo', async ({ request }) => {
  expect((await request.get(url)).status()).toBe(401)
})

test('real Keycloak admin gets all7 exact synthetic previews or404 with zero business writes', async ({ request }) => {
  const token = await getKeycloakToken('admin')
  const headers = { Authorization: `Bearer ${token}` }
  const before = digest()
  for (const [scenario, rooms, unmet] of oracles) {
    const response = await request.get(url, { params: { scenario }, headers })
    expect(response.status()).toBe(enabled ? 200 : 404)
    if (!enabled) continue
    const body = await response.json(), result = body.result
    expect(body.code).toBe(200)
    expect(result.scenario).toBe(scenario)
    expect(result.dataMode).toBe('SYNTHETIC')
    expect(result.businessData).toBe(false)
    expect(result.previewOnly).toBe(true)
    expect(result.referenceInstant).toBe('2026-10-31T02:00:00Z')
    expect(result.notice).toContain('không giữ phòng')
    expect(result.scenarios.map((s: { id: string }) => s.id)).toEqual(oracles.map(row => row[0]))
    expect(result.preview.status).toBe('COMPLETE')
    expect(result.preview.previewOnly).toBe(true)
    expect(result.preview.dataMode).toBe('SYNTHETIC')
    expect(result.preview.assignments.map((a: { roomInstanceId: number }) => a.roomInstanceId)).toEqual(rooms)
    expect(result.preview.unmet.map((a: { demandKey: string }) => a.demandKey)).toEqual(unmet)
    expect(result.preview.objective.assignedCount).toBe(rooms.length)
    expect(result.preview.objective.bedWaste).toBe(0)
    expect(result.preview.objective.floorSpan).toBe(0)
    expect(result.allDemandsAssigned).toBe(unmet.length === 0)
    expect(result.preview.assignments.length + result.preview.unmet.length).toBe(result.request.demands.length)
    expect(new Set(result.preview.assignments.map((a: { roomInstanceId: number }) => a.roomInstanceId)).size).toBe(rooms.length)
    expect(result.snapshot.coverage).toBe('COMPLETE')
    expect(result.snapshot.dataMode).toBe('SYNTHETIC')
    expect(result.snapshot.basis).toBe('SYNTHETIC_PREVIEWABLE_ROOMS_V1')
    expect(result.preview.observedAt).toBe(result.snapshot.observedAt)
    // TTL is evaluated at the fixed replay instant, never at today's clock.
    expect(Date.parse(result.snapshot.validUntil)).toBeGreaterThan(Date.parse(result.referenceInstant))
    expect(JSON.stringify(result)).not.toContain('"expected"')
    expect(JSON.stringify(result)).not.toContain('"c01Sha256"')
    const repeated = await request.get(url, { params: { scenario }, headers })
    expect(repeated.status()).toBe(200)
    expect(await repeated.json()).toEqual(body)
  }
  expect(digest()).toBe(before)
})

test('customer and reception cannot read Group demo', async ({ request }) => {
  for (const user of ['test_customer', 'reception_h1']) {
    const token = await getKeycloakToken(user)
    expect((await request.get(url, { headers: { Authorization: `Bearer ${token}` } })).status()).toBe(enabled ? 403 : 404)
  }
})

test('admin cannot inject dates, demands, rooms, business scope or write action', async ({ request }) => {
  const token = await getKeycloakToken('admin')
  for (const query of ['scenario=OTHER', 'scenario=fit_and_tie', 'scenario=', 'scenario=P01', 'scenario=FIT_AND_TIE&scenario=ZERO_QUOTA', 'hotelId=1', 'contractId=501', 'checkInDate=2026-11-01', 'demands=A', 'rooms=11', 'quota=100', 'role=CHAIN_ADMIN', 'clock=2026-10-31', 'workLimit=1', 'action=APPLY']) {
    const response = await request.get(`${url}?${query}`, { headers: { Authorization: `Bearer ${token}` } })
    expect(response.status()).toBe(enabled ? 400 : 404)
    if (enabled) expect((await response.json()).message).toBe('Kịch bản demo không hợp lệ.')
  }
})

test('default and complete evaluation never imply all demands assigned', async ({ request }) => {
  const token = await getKeycloakToken('admin')
  const headers = { Authorization: `Bearer ${token}` }
  const initial = await request.get(url, { headers })
  expect(initial.status()).toBe(enabled ? 200 : 404)
  if (!enabled) return
  expect((await initial.json()).result.scenario).toBe('FIT_AND_TIE')
  const zero = (await (await request.get(url, { params: { scenario: 'ZERO_QUOTA' }, headers })).json()).result
  expect(zero.preview.status).toBe('COMPLETE')
  expect(zero.preview.assignments).toEqual([])
  expect(zero.preview.unmet).toEqual([{ demandKey: 'A', reason: 'NO_ELIGIBLE_CAPACITY' }, { demandKey: 'B', reason: 'NO_ELIGIBLE_CAPACITY' }])
  expect(zero.allDemandsAssigned).toBe(false)
})
