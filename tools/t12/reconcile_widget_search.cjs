// Reconcile only the fixed, already-reviewed widget callback; never rewrite historical gold.
const fs = require('node:fs');
const path = require('node:path');
const cp = require('node:child_process');
const crypto = require('node:crypto');
const assert = require('node:assert/strict');
const SEARCH = 'frontend/src/modules/booking-portal/pages/SearchPage.tsx';
const PATHS = [SEARCH, 'backend/pom.xml'];
const PRIOR = 'e827013e89be77e3ec85c971ab441e470a297783';
const REVIEWED = '3f0cb2eacab3a5a2ca8c1155f187ff1ae6640afb';
const ADDITION = '        onOpenExternal={() => setIsAdvisorOpen(true)}\n';
const lf = text => text.replace(/\r\n/g, '\n');
function reconcile(root, file, actual) {
  if (!PATHS.includes(file)) return actual;
  const audit = JSON.parse(fs.readFileSync(path.join(root,
    'docs/member1/contracts/t12-01-source-reaudit-2026-10-29.json'), 'utf8'));
  assert.equal(audit.reviewedCodeCommit, REVIEWED);
  const entry = audit.files.find(row => row.path === file);
  assert.ok(entry);
  assert.equal(entry.priorCommit, PRIOR);
  cp.execFileSync('git', ['merge-base', '--is-ancestor', REVIEWED, 'HEAD'], {cwd:root});
  const read = commit => lf(cp.execFileSync('git', ['show', `${commit}:${file}`], {cwd:root,encoding:'utf8'}));
  const current = lf(actual);
  assert.equal(current, read(REVIEWED), 'Source differs from fixed reviewed widget commit');
  assert.equal(crypto.createHash('sha256').update(current).digest('hex'), entry.currentLfSha256);
  const legacy = read(PRIOR);
  assert.equal(crypto.createHash('sha256').update(legacy).digest('hex'), entry.priorLfSha256);
  if (file === SEARCH) {
    assert.equal(current.split(ADDITION).length - 1, 1, 'Expected one reviewed callback');
    assert.equal(current.replace(ADDITION, ''), legacy, 'Widget change exceeds the one reviewed callback');
  }
  return legacy;
}
module.exports = { SEARCH, PATHS, PRIOR, reconcile };
