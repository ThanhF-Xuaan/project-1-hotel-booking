// Read-only verification of reviewed U14 source deltas and their provenance.
const fs = require('node:fs');
const path = require('node:path');
const cp = require('node:child_process');
const crypto = require('node:crypto');
const assert = require('node:assert/strict');
const { PATHS, PRIOR, reconcile } = require('./reconcile_widget_search.cjs');

const root = path.resolve(__dirname, '../..');
const auditPath = 'docs/t12/re-audits/2026-10-26-u14-local-v1-source.json';
const audit = JSON.parse(fs.readFileSync(path.join(root, auditPath), 'utf8'));
const manifest = JSON.parse(fs.readFileSync(path.join(root, audit.manifestPath), 'utf8'));
const lf = text => text.replace(/\r\n/g, '\n');
const hash = value => crypto.createHash('sha256').update(value).digest('hex');
const crlf = text => lf(text).replace(/\n/g, '\r\n');
const readGit = (commit, file) => cp.execFileSync('git', ['show', `${commit}:${file}`],
  { cwd: root, encoding: 'utf8' });
const readWorktree = file => fs.readFileSync(path.join(root, file), 'utf8');
const gitDiff = file => cp.execFileSync('git', ['diff', '--no-ext-diff', '--binary',
  audit.comparisonCommit, ...(PATHS.includes(file) ? [PRIOR] : []), '--', file], { cwd: root });

assert.equal(audit.status, 'WORKING_TREE_SOURCE_RECONCILED');
assert.equal(manifest.gitSha, audit.manifestGitSha, 'Historical contract baseline changed');

const appRouteImport =
  "const PersonalizationPage = lazy(() => import('./modules/booking-portal/pages/PersonalizationPage'))\n";
const appPrivacyRouteImport =
  "const PrivacyPage = lazy(() => import('./modules/booking-portal/pages/PrivacyPage'))\n";
const appCustomerRoute =
  "            <Route element={<ProtectedRoute roles={['CUSTOMER']} />}>\n" +
  '              <Route path="personalization" element={<PersonalizationPage />} />\n' +
  '            </Route>\n';
const appPrivacyRoute = '            <Route path="privacy" element={<PrivacyPage />} />\n';
const searchLink =
  '            <button\n' +
  '              type="button"\n' +
  '              onClick={() => navigate(`/portal/personalization?hotelId=${criteria.hotelId}`)}\n' +
  '              className="px-3.5 py-2 text-xs font-semibold text-neutral-700 hover:text-red-600 hover:bg-neutral-50 rounded-xl transition-colors flex items-center gap-1.5"\n' +
  '            >\n' +
  '              <ShieldCheck className="w-4 h-4" />\n' +
  '              Consent & gợi ý cá nhân hóa\n' +
  '            </button>\n';
const oldPomDependency =
  '\t\t<dependency>\n' +
  '\t\t\t<groupId>org.liquibase</groupId>\n' +
  '\t\t\t<artifactId>liquibase-core</artifactId>\n' +
  '\t\t</dependency>';
const newPomDependency =
  '\t\t<dependency>\n' +
  '\t\t\t<groupId>org.springframework.boot</groupId>\n' +
  '\t\t\t<artifactId>spring-boot-starter-liquibase</artifactId>\n' +
  '\t\t</dependency>';

for (const item of audit.files) {
  const base = lf(readGit(audit.comparisonCommit, item.path));
  const implementationBase = lf(readGit(audit.implementationBaseCommit, item.path));
  const current = reconcile(root, item.path, lf(readWorktree(item.path)));
  assert.equal(implementationBase, base,
    `${item.path} differs in the implementation base before this working-tree change`);
  assert.equal(hash(base), item.baseHashes.unixLfSha256, `${item.path} base LF hash mismatch`);
  assert.equal(hash(crlf(base)), item.baseHashes.windowsCrlfSha256,
    `${item.path} base CRLF hash mismatch`);
  assert.equal(hash(current), item.currentHashes.unixLfSha256, `${item.path} current LF hash mismatch`);
  assert.equal(hash(crlf(current)), item.currentHashes.windowsCrlfSha256,
    `${item.path} current CRLF hash mismatch`);
  assert.equal(hash(gitDiff(item.path)), item.patchSha256, `${item.path} patch digest mismatch`);

  if (item.path === 'backend/pom.xml') {
    assert.equal(base.split(oldPomDependency).length - 1, 1,
      'Expected exactly one original Liquibase dependency');
    const expected = base.replace(oldPomDependency, newPomDependency);
    assert.equal(current, expected, 'POM differs beyond the exact Liquibase starter replacement');
  } else if (item.path === 'frontend/src/App.tsx') {
    let stripped = current;
    for (const addition of [appRouteImport, appCustomerRoute, appPrivacyRouteImport, appPrivacyRoute]) {
      assert.equal(stripped.split(addition).length - 1, 1,
        'Expected one exact U14 App.tsx addition');
      stripped = stripped.replace(addition, '');
    }
    assert.equal(stripped, base, 'T12 App.tsx differs beyond the exact U14 additions');
  } else if (item.path === 'frontend/src/modules/booking-portal/pages/SearchPage.tsx') {
    assert.equal(current.split(searchLink).length - 1, 1,
      'Expected one exact U14 SearchPage entry link');
    assert.equal(current.replace(searchLink, ''), base,
      'SearchPage differs beyond the exact U14 entry link');
  } else {
    assert.fail(`Unexpected source path in U14 audit: ${item.path}`);
  }
  const entry = manifest.files.find(file => file.path === item.path);
  assert.ok(entry, `${item.path} source entry missing from T12 manifest`);
  assert.equal(entry.sha256, item.currentHashes.windowsCrlfSha256);
  assert.equal(entry.unixLfSha256, item.currentHashes.unixLfSha256);
  assert.equal(entry.windowsCrlfSha256, item.currentHashes.windowsCrlfSha256);
  assert.equal(entry.historicalSha256, item.previousSourceEvidence.historicalSha256);
  assert.equal(entry.previousReconciledSha256, item.previousSourceEvidence.sha256);
  assert.equal(entry.previousReconciledFromDevelopCommit,
    item.previousSourceEvidence.reconciledFromDevelopCommit);
  if (item.previousSourceEvidence.unixLfSha256) {
    assert.equal(entry.previousReconciledUnixLfSha256,
      item.previousSourceEvidence.unixLfSha256);
  }
  assert.equal(entry.reconciledFromDevelopCommit, audit.comparisonCommit);
  assert.equal(entry.reconciliationEvidence, auditPath);
  assert.equal(entry.baselineUnmodified, true);
}

console.log(`PASS: ${audit.files.length} U14 source deltas match reviewed content, hashes, Git patch digests and preserved T12 history.`);
