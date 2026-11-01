// Exact additive utility route before delegating to the frozen occupancy/T12 provenance chain.
const fs = require('node:fs');
const path = require('node:path');
const cp = require('node:child_process');
const crypto = require('node:crypto');
const assert = require('node:assert/strict');
const ROOT = path.resolve(__dirname, '../..');
const APP = 'frontend/src/App.tsx';
const BASE = '5af00a7ab8652a58a488dc9a1bda59f2d5e1a2ad';
const SHA = '381d9455b7f17e248520253c59940f6eb8767748bfc5052afb2110a1bb650128';
const IMPORT_ANCHOR = "const OccupancyDemoPage = lazy(() => import('./modules/analytics/pages/OccupancyDemoPage'))\n";
const IMPORT = "const UtilityDemoPage = lazy(() => import('./modules/analytics/pages/UtilityDemoPage'))\n";
const ANCHOR = '          {/* Internal Hotel Staff & Executive Admin Console (bắt buộc đăng nhập) */}';
const ROUTE = "          <Route element={<ProtectedRoute roles={['CHAIN_ADMIN']} />}>\n" +
  '            <Route path="/ai/demo/utility-anomalies" element={<UtilityDemoPage />} />\n          </Route>\n\n';
function reconcile(raw) {
  const base = cp.execFileSync('git', ['show', `${BASE}:${APP}`], { cwd: ROOT, encoding: 'utf8' }).replace(/\r\n/g, '\n');
  assert.equal(crypto.createHash('sha256').update(base).digest('hex'), SHA);
  cp.execFileSync('git', ['merge-base', '--is-ancestor', BASE, 'HEAD'], { cwd: ROOT });
  assert.equal(base.split(IMPORT_ANCHOR).length, 2);
  assert.equal(base.split(ANCHOR).length, 2);
  const expected = base.replace(IMPORT_ANCHOR, IMPORT_ANCHOR + IMPORT).replace(ANCHOR, ROUTE + ANCHOR);
  assert.equal(raw.replace(/\r\n/g, '\n'), expected, 'App differs beyond exact CHAIN_ADMIN utility lazy route');
  return base;
}
module.exports = { reconcile };
if (require.main === module) process.stdout.write(reconcile(fs.readFileSync(0, 'utf8')));
