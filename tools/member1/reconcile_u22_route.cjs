// Verify the exact additive U22 route, then return the immutable pre-U22 App for historical audits.
const fs = require('node:fs');
const cp = require('node:child_process');
const crypto = require('node:crypto');
const path = require('node:path');
const assert = require('node:assert/strict');
const ROOT = path.resolve(__dirname, '../..');
const APP = 'frontend/src/App.tsx';
const BASE = '35fb85a830b2f2315469faf289b303f795bf9380';
const SHA = '46f35957fe89cc803b0f7756b4ff5860ad93cb2ed36208515c536e3dcf308f3e';
const IMPORT_ANCHOR = "const PrivacyPage = lazy(() => import('./modules/booking-portal/pages/PrivacyPage'))\n";
const IMPORT = "const OccupancyDemoPage = lazy(() => import('./modules/analytics/pages/OccupancyDemoPage'))\n";
const ROUTE_ANCHOR = '          {/* Internal Hotel Staff & Executive Admin Console (bắt buộc đăng nhập) */}';
const ROUTE = "          <Route element={<ProtectedRoute roles={['CHAIN_ADMIN']} />}>\n" +
  '            <Route path="/ai/demo/occupancy" element={<OccupancyDemoPage />} />\n' +
  '          </Route>\n\n';
const lf = text => text.replace(/\r\n/g, '\n');
function reconcile(actual) {
  const base = lf(cp.execFileSync('git', ['show', `${BASE}:${APP}`], { cwd: ROOT, encoding: 'utf8' }));
  assert.equal(crypto.createHash('sha256').update(base).digest('hex'), SHA, 'Pre-U22 App hash drift');
  cp.execFileSync('git', ['merge-base', '--is-ancestor', BASE, 'HEAD'], { cwd: ROOT });
  assert.equal(base.split(IMPORT_ANCHOR).length, 2);
  assert.equal(base.split(ROUTE_ANCHOR).length, 2);
  const expected = base.replace(IMPORT_ANCHOR, IMPORT_ANCHOR + IMPORT).replace(ROUTE_ANCHOR, ROUTE + ROUTE_ANCHOR);
  assert.equal(lf(actual), expected, 'App differs beyond exact CHAIN_ADMIN U22 lazy route');
  return base;
}
module.exports = { reconcile, APP, BASE, IMPORT, ROUTE };
if (require.main === module) process.stdout.write(reconcile(fs.readFileSync(0, 'utf8')));
