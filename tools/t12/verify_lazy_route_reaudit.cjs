// Read-only verification of the versioned lazy-route source reconciliation.
const fs = require('node:fs');
const path = require('node:path');
const cp = require('node:child_process');
const crypto = require('node:crypto');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '../..');
const ts = require(path.join(root, 'frontend/node_modules/typescript'));
const audit = JSON.parse(fs.readFileSync(path.join(root,
  'docs/t12/re-audits/2026-10-25-lazy-routes-v1.json'), 'utf8'));
const lf = text => text.replace(/\r\n/g, '\n');
const hash = text => crypto.createHash('sha256').update(text).digest('hex');
const readGit = (commit, file) => cp.execFileSync('git', ['show', `${commit}:${file}`],
  {cwd:root, encoding:'utf8'});
const parse = text => ts.createSourceFile('App.tsx', text, ts.ScriptTarget.Latest, true, ts.ScriptKind.TSX);

function routes(source) {
  const result = [];
  function visit(node, depth) {
    const tag = ts.isJsxElement(node) ? node.openingElement : ts.isJsxSelfClosingElement(node) ? node : null;
    const isRoute = tag && tag.tagName.getText(source) === 'Route';
    if (isRoute) result.push({depth, attributes:tag.attributes.getText(source).replace(/\s+/g, ' ').trim()});
    ts.forEachChild(node, child => visit(child, depth + (isRoute ? 1 : 0)));
  }
  visit(source, 0);
  return result;
}

function imports(source) {
  const result = {};
  for (const node of source.statements) {
    if (!ts.isImportDeclaration(node) || !node.importClause) continue;
    const target = node.moduleSpecifier.text;
    if (!target.startsWith('./')) continue;
    if (node.importClause.name) result[node.importClause.name.text] = {path:target, export:'default'};
    const named = node.importClause.namedBindings;
    if (named && ts.isNamedImports(named)) {
      for (const item of named.elements) result[item.name.text] = {path:target, export:(item.propertyName || item.name).text};
    }
  }
  return result;
}

function lazyImports(source) {
  const result = {};
  function visit(node) {
    if (ts.isVariableDeclaration(node) && node.initializer) {
      const text = node.initializer.getText(source);
      if (text.startsWith('lazy(')) {
        const target = text.match(/import\(\s*['"]([^'"]+)['"]\s*\)/);
        assert.ok(target, 'Missing lazy module');
        const named = text.match(/default:\s*module\.(\w+)/);
        result[node.name.getText(source)] = {path:target[1], export:named ? named[1] : 'default'};
      }
    }
    ts.forEachChild(node, visit);
  }
  visit(source);
  return result;
}

const before = readGit(audit.comparisonCommit, audit.path);
const accepted = readGit(audit.acceptedDevelopCommit, audit.path);
const current = fs.readFileSync(path.join(root, audit.path), 'utf8');
assert.equal(lf(current), lf(accepted), 'Working source differs from accepted source');
assert.equal(hash(lf(before)), audit.previousSourceEvidence.unixLfSha256);
assert.equal(hash(lf(current)), audit.currentHashes.unixLfSha256);
assert.equal(hash(lf(current).replace(/\n/g, '\r\n')), audit.currentHashes.windowsCrlfSha256);
const oldAst = parse(before), newAst = parse(current);
const beforeRoutes = routes(oldAst), currentRoutes = routes(newAst);
assert.deepEqual(currentRoutes, beforeRoutes, 'Route/guard/redirect contract changed');
assert.deepEqual(currentRoutes, audit.routes, 'Recorded route tree differs');
assert.equal(currentRoutes.length, audit.routeCount);
assert.equal(hash(JSON.stringify(currentRoutes)), audit.routeTreeSha256);
const oldImports = imports(oldAst), eagerImports = imports(newAst);
assert.deepEqual(eagerImports.ProtectedRoute, oldImports.ProtectedRoute, 'ProtectedRoute import changed');
delete oldImports.ProtectedRoute;
assert.deepEqual(oldImports, audit.previousComponentImports);
const lazy = lazyImports(newAst);
assert.deepEqual(lazy, audit.lazyComponents);
const aliases = [];
for (const [name, entry] of Object.entries(oldImports)) {
  const replacement = lazy[name];
  if (replacement && entry.path === replacement.path && entry.export !== replacement.export) {
    assert.equal(replacement.export, 'default');
    const modulePath = 'frontend/src/' + entry.path.slice(2) + '.tsx';
    const previous = readGit(audit.comparisonCommit, modulePath);
    const reviewed = readGit(audit.acceptedDevelopCommit, modulePath);
    assert.equal(lf(previous), lf(reviewed));
    assert.equal(lf(fs.readFileSync(path.join(root, modulePath), 'utf8')), lf(reviewed));
    const alias = parse(reviewed).statements.find(node => ts.isExportAssignment(node) && !node.isExportEquals);
    assert.ok(alias && ts.isIdentifier(alias.expression));
    assert.equal(alias.expression.text, entry.export, 'Default no longer aliases named component');
    aliases.push({modulePath,namedExport:entry.export,defaultAliases:alias.expression.text,
      unixLfSha256:hash(lf(reviewed))});
    entry.export = 'default';
  }
}
assert.deepEqual(aliases, audit.equivalentExportAliases);
assert.deepEqual(lazy, oldImports, 'Resolved component implementation changed');
assert.equal(Object.keys(lazy).length, audit.lazyComponentCount);
const evidence = JSON.parse(fs.readFileSync(path.join(root,
  'docs/t12/r1-00/v2.0.0-draft.1/baseline-source-evidence.json'), 'utf8'));
const currentEntry = evidence.files.find(entry => entry.path === audit.path);
assert.equal(currentEntry.unixLfSha256, audit.currentHashes.unixLfSha256);
assert.equal(currentEntry.windowsCrlfSha256, audit.currentHashes.windowsCrlfSha256);
assert.equal(currentEntry.previousReconciledSha256, audit.previousSourceEvidence.sha256);
console.log(`PASS: ${currentRoutes.length} Route nodes, ${Object.keys(lazy).length} lazy components, ${aliases.length} verified default aliases; source hashes match.`);
