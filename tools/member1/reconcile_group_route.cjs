// Verify only the additive Group demo route before passing bytes to historical route audits.
const fs = require('node:fs');
const path = require('node:path');
const cp = require('node:child_process');
const crypto = require('node:crypto');
const assert = require('node:assert/strict');
const ROOT = path.resolve(__dirname, '../..');
const APP = 'frontend/src/App.tsx';
const BASE = '2042240e9e6c6b213cd726415903b83da249a1f4';
const SHA = 'd134928e5e22165b41e65891ec2b22e7107a5d62713f5cbf6a6615daf59259f7';
const IMPORT_ANCHOR = "const UtilityDemoPage = lazy(() => import('./modules/analytics/pages/UtilityDemoPage'))\n";
const IMPORT = "const GroupDemoPage = lazy(() => import('./modules/analytics/pages/GroupDemoPage'))\n";
const ANCHOR = '          {/* Internal Hotel Staff & Executive Admin Console (bắt buộc đăng nhập) */}\n';
const ROUTE = "          <Route element={<ProtectedRoute roles={['CHAIN_ADMIN']} />}>\n" +
  '            <Route path="/ai/demo/group-room-assignment" element={<GroupDemoPage />} />\n          </Route>\n\n';
function reconcile(raw) {
  const base = cp.execFileSync('git', ['show', `${BASE}:${APP}`], {cwd:ROOT, encoding:'utf8'}).replace(/\r\n/g, '\n');
  assert.equal(crypto.createHash('sha256').update(base).digest('hex'), SHA);
  cp.execFileSync('git', ['merge-base', '--is-ancestor', BASE, 'HEAD'], {cwd:ROOT});
  assert.equal(base.split(IMPORT_ANCHOR).length, 2);
  assert.equal(base.split(ANCHOR).length, 2);
  const expected = base.replace(IMPORT_ANCHOR, IMPORT_ANCHOR + IMPORT).replace(ANCHOR, ANCHOR + ROUTE);
  assert.equal(raw.replace(/\r\n/g, '\n'), expected, 'App differs beyond exact CHAIN_ADMIN Group lazy route');
  return base;
}
module.exports = {reconcile};
if (require.main === module) process.stdout.write(reconcile(fs.readFileSync(0, 'utf8')));
