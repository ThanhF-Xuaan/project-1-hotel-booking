import { test, expect } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { getKeycloakToken, BACKEND_URL } from '../helpers/auth.helper';

const enabled = process.env.U22_DEMO_ENABLED === 'true';
const url = `${BACKEND_URL}/api/v1/ai/demo/occupancy`;
function digest() {
  if (BACKEND_URL !== 'http://localhost:18080') throw new Error('Fixed synthetic acceptance target required');
  const pg = execFileSync('docker', ['ps', '--filter', 'label=com.docker.compose.project=hotel-m1-acceptance',
    '--filter', 'label=com.docker.compose.service=acceptance_postgres', '--format', '{{.ID}}'], { encoding: 'utf8' }).trim();
  if (!/^[a-f0-9]+$/.test(pg)) throw new Error('Owned database required');
  if (execFileSync('docker', ['port', pg, '5432/tcp'], { encoding: 'utf8' }).trim() !== '127.0.0.1:15432') throw new Error('Unexpected target');
  const dump = execFileSync('docker', ['exec', pg, 'pg_dump', '--data-only', '--no-owner', '--no-privileges', '-U', 'postgres',
    '-d', 'hotel_acceptance_db'], { encoding: 'utf8', maxBuffer: 32 * 1024 * 1024 });
  return createHash('sha256').update(dump.split('\n').filter(line => !/^\\(?:un)?restrict /.test(line)).join('\n')).digest('hex');
}

test('anonymous cannot read demo', async ({ request }) => {
  expect((await request.get(url)).status()).toBe(401);
});

test('real Keycloak chain admin gets explicit synthetic replay or disabled 404 with zero business writes', async ({ request }) => {
  const token = await getKeycloakToken('admin');
  const before = digest();
  for (const scenario of ['HOTEL_A', 'HOTEL_B']) {
    const response = await request.get(`${url}?scenario=${scenario}`, { headers: { Authorization: `Bearer ${token}` } });
    expect(response.status()).toBe(enabled ? 200 : 404);
    if (!enabled) continue;
    const result = (await response.json()).result;
    expect(result.dataMode).toBe('SYNTHETIC');
    expect(result.businessData).toBe(false);
    expect(result.scenario).toBe(scenario);
    expect(result.history.samples).toHaveLength(420);
    expect(result.forecast.status).toBe('FORECAST_READY');
    expect(result.forecast.dataMode).toBe('SYNTHETIC');
    expect(result.forecast.points).toHaveLength(30);
    expect(result.forecast.points[0].date).toBe('2026-10-30');
    for (const day of result.forecast.points) {
      expect(day.lowerPercent).toBeGreaterThanOrEqual(0);
      expect(day.upperPercent).toBeLessThanOrEqual(100);
      expect(day.occupancyPercent).toBeGreaterThanOrEqual(day.lowerPercent);
      expect(day.occupancyPercent).toBeLessThanOrEqual(day.upperPercent);
    }
  }
  expect(digest()).toBe(before);
});

test('customer and reception tokens cannot use replay', async ({ request }) => {
  for (const username of ['test_customer', 'reception_h1']) {
    const token = await getKeycloakToken(username);
    expect((await request.get(url, { headers: { Authorization: `Bearer ${token}` } })).status()).toBe(enabled ? 403 : 404);
  }
});

test('admin cannot select business scope, arbitrary scenarios or duplicate selectors', async ({ request }) => {
  const token = await getKeycloakToken('admin');
  for (const query of ['scenario=OTHER', 'hotelId=1', 'scenario=HOTEL_A&scenario=HOTEL_B', 'scenario=']) {
    const response = await request.get(`${url}?${query}`, { headers: { Authorization: `Bearer ${token}` } });
    expect(response.status()).toBe(enabled ? 400 : 404);
    if (enabled) expect((await response.json()).message).toBe('Kịch bản demo không hợp lệ.');
  }
});
