// Synthetic sandbox only. No response bodies, JWTs or credentials are written to evidence.
import { execFileSync } from 'node:child_process';
import { writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { createRequire } from 'node:module';
const [phase, output, container, database, source] = process.argv.slice(2);
if (!['lifecycle', 'restart', 'outage', 'recovered'].includes(phase)
    || !/^[a-f0-9]{12,64}$/.test(container || '')
    || !/^t12_pgvector_http_[a-z0-9_]{1,40}$/.test(database || '')
    || !/^HTTP-[A-F0-9]{32}$/.test(source || '')) throw new Error('Invalid sandbox arguments');
const backend = 'http://localhost:18080';
const adminPath = '/api/v1/ai/admin/knowledge/';
const askPath = '/api/v1/public/ai/room-advisor/knowledge/ask';
let checks = 0;
let step = 'preflight';
function check(condition, label) {
  step = label;
  if (!condition) throw new Error(label);
  checks++;
}
async function post(path, body, token) {
  step = `HTTP ${path.split('?')[0]}`;
  const response = await fetch(backend + path, { method: 'POST',
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: JSON.stringify(body), signal: AbortSignal.timeout(15000) });
  const result = response.headers.get('content-type')?.includes('application/json') ? await response.json() : {};
  return { status: response.status, body: result };
}
async function token(username) {
  const response = await fetch('http://localhost:18081/realms/hotel-acceptance/protocol/openid-connect/token', {
    method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ grant_type: 'password', client_id: 'hotel-frontend', username, password: 'Password123!' }),
    signal: AbortSignal.timeout(15000),
  });
  check(response.ok, 'Keycloak authentication');
  const result = await response.json();
  check(typeof result.access_token === 'string', 'Keycloak token exists');
  return result.access_token;
}
function sql(query) {
  return execFileSync('docker', ['exec', container, 'psql', '-X', '-U', 'postgres', '-d', database, '-Atc', query],
    { encoding: 'utf8', timeout: 15000 }).trim();
}
const scope = (hotelId, version = 1) => ({ sourceId: `${source}-${hotelId}`, version, scopeType: 'HOTEL_SCOPED', hotelId });
const document = (hotelId, hour) => ({ ...scope(hotelId), category: 'POLICY', title: 'Synthetic check-in policy',
  content: `Check-in time is ${hour}:00.`, effectiveFrom: '2020-01-01', effectiveTo: '2099-12-31' });
async function ask(hotelId, hour) {
  const response = await post(askPath, { hotelId, question: 'What time is check-in?', topK: 5 });
  check(response.status === 200, 'Knowledge HTTP 200');
  const result = response.body.result;
  check(result.status === 'GROUNDED_ANSWER', 'Knowledge is grounded');
  check(result.answer.includes(`${hour}:00`), 'Expected policy time');
  check(result.citations.length > 0 && result.citations.every(c => c.hotelId === hotelId && c.sourceId === `${source}-${hotelId}`),
    'Citations match persistent source and hotel');
}
async function r1() {
  const response = await post('/api/v1/public/ai/room-advisor/search', { contractVersion: '2.0', criteria: {
    hotelId: 1, locationKeyword: 'Acceptance Luxury Hanoi (Hà Nội)', checkInDate: '2027-01-10', checkOutDate: '2027-01-12',
    adults: 2, children: 0, extraBeds: 0, roomsCount: 1, requiredFeatureCodes: [], preferredFeatureCodes: [],
  } });
  check(response.status === 200 && response.body.result.status === 'RESULTS', 'R1 works during RAG outage');
  check(response.body.result.recommendations.length > 0 && response.body.result.recommendations.every(c =>
    c.room.hotelId === 1 && Number(c.price.finalTotalAmount) > 0 && c.action.type === 'VIEW_ROOM_DETAILS'), 'R1 verified cards');
}
try {
  if (phase === 'lifecycle') {
    const info = JSON.parse(execFileSync('docker', ['inspect', container], { encoding: 'utf8', timeout: 15000 }))[0];
    check(info.Config.Labels['hotel.m1.rag-http'] === database, 'Owned RAG container');
    check(info.NetworkSettings.Ports['5432/tcp'].length === 1
      && info.NetworkSettings.Ports['5432/tcp'][0].HostIp === '127.0.0.1'
      && info.NetworkSettings.Ports['5432/tcp'][0].HostPort === '15435', 'Dedicated loopback binding');
    check(sql("SELECT count(*) FROM pg_roles WHERE rolname='rag_http_writer' AND NOT rolsuper AND NOT rolcreatedb AND NOT rolcreaterole AND NOT rolreplication AND NOT rolbypassrls") === '1', 'Writer has no administrative role attributes');
    check(sql("SELECT has_database_privilege('rag_http_writer',current_database(),'CREATE') OR has_database_privilege('rag_http_writer',current_database(),'TEMP') OR has_schema_privilege('rag_http_writer','public','CREATE')") === 'f', 'Writer cannot create databases, temporary objects or schema objects');
    check(sql("SELECT has_table_privilege('rag_http_writer','hotels','SELECT')") === 'f', 'Writer has no unrelated table read privilege');
    const customer = await token('test_customer');
    for (const auth of [undefined, customer]) {
      const denied = await post(adminPath + 'stage?version=1', document(1, 14), auth);
      check([401, 403].includes(denied.status), 'Anonymous/customer cannot stage');
    }
    check(sql('SELECT count(*) FROM rag_sources') === '0', 'Denied requests have zero RAG writes');
    const admin = await token('admin');
    for (const [hotel, hour] of [[1, 14], [2, 16]]) {
      const staged = await post(adminPath + 'stage?version=1', document(hotel, hour), admin);
      check(staged.status === 200 && staged.body.result.status === 'STAGED', 'Admin stages persistent source');
      const inactive = await post(askPath, { hotelId: hotel, question: 'What time is check-in?' });
      check(inactive.status === 200 && inactive.body.result.status === 'INSUFFICIENT_EVIDENCE', 'Staged policy is not public');
      check((await post(adminPath + 'activate', scope(hotel), admin)).status === 200, 'Admin activates source');
      await ask(hotel, hour);
    }
    check(sql('SELECT count(*) FROM rag_document_versions WHERE status=\'ACTIVE\'') === '2', 'HTTP activation persisted in SQL');
    check((await post(askPath, { hotelId: 32767, question: 'What time is check-in?' })).body.result.status === 'SCOPE_REQUIRED', 'Inactive hotel rejected');
    const staged = await post(adminPath + 'stage?version=2', document(1, 15), admin);
    check(staged.status === 200, 'Replacement staged');
    await ask(1, 14);
    check((await post(adminPath + 'activate', scope(1, 2), admin)).status === 200, 'Replacement activated');
    await ask(1, 15);
    check((await post(adminPath + 'delete', scope(1, 2), admin)).status === 400, 'Active version deletion refused');
    check((await post(adminPath + 'rollback', { ...scope(1), targetVersion: 1 }, admin)).status === 200, 'Rollback via HTTP');
    await ask(1, 14);
    check((await post(adminPath + 'delete', scope(1, 2), admin)).status === 200, 'Inactive replacement deleted');
    check(sql('SELECT count(*) FROM rag_document_versions') === '2', 'Rollback/delete leave original versions');
    await ask(2, 16);
  } else if (phase === 'outage') {
    const response = await post(askPath, { hotelId: 1, question: 'What time is check-in?' });
    check(response.status === 503 && response.body.code === 503, 'RAG outage is explicit 503');
    check(!response.body.result.citations?.length, 'No fabricated citations on outage');
    await r1();
  } else {
    await ask(1, 14);
    await ask(2, 16);
    check(sql('SELECT count(*) FROM rag_document_versions WHERE status=\'ACTIVE\'') === '2', 'Persistent active versions survive');
    if (phase === 'recovered') {
      step = 'browser persistent citations';
      const require = createRequire(new URL('../../frontend/package.json', import.meta.url));
      const { chromium } = require('@playwright/test');
      const browser = await chromium.launch({ headless: true });
      try {
        const page = await browser.newPage();
        await page.goto('http://localhost:13000/portal/search');
        await page.getByRole('button', { name: 'Mở Trợ lý Tìm phòng AI' }).click();
        await page.getByRole('button', { name: 'Chính sách & tiện ích' }).click();
        await page.getByLabel('Khách sạn tra cứu').selectOption('1');
        await page.getByLabel('Câu hỏi chính sách').fill('What time is check-in?');
        const response = page.waitForResponse(r => r.url().endsWith('/knowledge/ask'));
        await page.getByRole('button', { name: 'Tra cứu chính sách', exact: true }).click();
        const result = await (await response).json();
        check(result.result.status === 'GROUNDED_ANSWER' && result.result.citations.every(c => c.sourceId === `${source}-1`), 'Browser receives SQL-backed citations');
        await page.getByLabel('Nguồn tham khảo').waitFor({ state: 'visible' });
        check(true, 'Browser renders persistent citation panel');
      } finally { await browser.close(); }
    }
  }
  const report = { status: 'PASS', phase, checks, synthetic: true, embedding: 'local-multilingual-hashing-v1' };
  writeFileSync(join(output, `persistent-rag-${phase}.json`), JSON.stringify(report, null, 2) + '\n');
  console.log(JSON.stringify(report));
} catch {
  const report = { status: 'FAIL', phase, checks, failedStep: step, synthetic: true };
  writeFileSync(join(output, `persistent-rag-${phase}.json`), JSON.stringify(report, null, 2) + '\n');
  console.error(JSON.stringify(report));
  process.exitCode = 1;
}
