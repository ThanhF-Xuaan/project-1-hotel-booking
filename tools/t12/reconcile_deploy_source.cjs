// Compose only the exact four upstream source deltas; historical audits remain immutable.
const fs = require('node:fs');
const path = require('node:path');
const cp = require('node:child_process');
const crypto = require('node:crypto');
const assert = require('node:assert/strict');
const BASE = 'a70b833f8d138621e539fdbcade595eb56165a4c';
const REVIEWED = '7d8ee514b1cbc4e6a4aa4889817620a57e80b513';
const RECEIPT = 'docs/member1/contracts/deploy-source-reaudit-2026-11-01.json';
const RECEIPT_SHA = '632815054005b835d83b365e41170420c26a5c7822f28fa813adbf35da669a3c';
const PATHS = [
  'backend/src/main/java/vn/edu/utc/hotel_booking/common/config/SecurityConfig.java',
  'backend/pom.xml', 'docker-compose.yml',
  'frontend/src/modules/booking-portal/pages/SearchPage.tsx',
];
const lf = text => text.replace(/\r\n/g, '\n');
const sha = text => crypto.createHash('sha256').update(text).digest('hex');
function reconcile(root, file, actual) {
  if (!PATHS.includes(file)) return actual;
  const raw = fs.readFileSync(path.join(root, RECEIPT), 'utf8');
  assert.equal(sha(lf(raw)), RECEIPT_SHA, 'Unreviewed deployment source receipt');
  const audit = JSON.parse(raw);
  assert.equal(audit.status, 'REVIEWED_SOURCE_ONLY');
  assert.equal(audit.baseCommit, BASE);
  assert.equal(audit.reviewedCommit, REVIEWED);
  assert.deepEqual(audit.files.map(row => row.path).sort(), [...PATHS].sort());
  const git = args => cp.execFileSync('git', args, {cwd:root, encoding:'utf8'});
  git(['merge-base', '--is-ancestor', BASE, REVIEWED]);
  git(['merge-base', '--is-ancestor', REVIEWED, 'HEAD']);
  const row = audit.files.find(entry => entry.path === file);
  const before = lf(git(['show', `${BASE}:${file}`]));
  const after = lf(git(['show', `${REVIEWED}:${file}`]));
  assert.equal(sha(before), row.beforeLfSha256);
  assert.equal(sha(after), row.afterLfSha256);
  assert.equal(sha(lf(git(['diff', '--no-ext-diff', '--no-textconv', '--no-color', '--no-renames',
    '--unified=3', BASE, REVIEWED, '--', file]))), row.patchLfSha256);
  assert.ok(row.review);
  assert.equal(lf(actual), after, `Source differs from reviewed deployment commit: ${file}`);
  return before;
}
module.exports = {BASE, REVIEWED, PATHS, reconcile};
