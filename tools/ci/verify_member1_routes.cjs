// Check live route contracts without freezing unrelated application source.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const ts = require('../../frontend/node_modules/typescript');
const contracts = [
  ['/ai/demo/occupancy', 'OccupancyDemoPage', './modules/analytics/pages/OccupancyDemoPage', 'CHAIN_ADMIN'],
  ['/ai/demo/utility-anomalies', 'UtilityDemoPage', './modules/analytics/pages/UtilityDemoPage', 'CHAIN_ADMIN'],
  ['/ai/demo/group-room-assignment', 'GroupDemoPage', './modules/analytics/pages/GroupDemoPage', 'CHAIN_ADMIN'],
  ['/portal/personalization', 'PersonalizationPage', './modules/booking-portal/pages/PersonalizationPage', 'CUSTOMER'],
  ['/portal/privacy', 'PrivacyPage', './modules/booking-portal/pages/PrivacyPage', null],
  ['/portal/hotels/:hotelId/room-types/:hotelRoomTypeId', 'RoomDetailsPage', './modules/booking-portal/pages/RoomDetailsPage', null],
];
function verify(text) {
  const source = ts.createSourceFile('App.tsx', text, ts.ScriptTarget.Latest, true, ts.ScriptKind.TSX);
  assert.equal(source.parseDiagnostics.length, 0, 'Invalid TSX');
  const attrs = node => Object.fromEntries(node.attributes.properties.filter(ts.isJsxAttribute).map(a => [a.name.text, a.initializer]));
  const imports = new Map();
  function collect(node) {
    if (ts.isImportDeclaration(node) && node.importClause?.name) imports.set(node.importClause.name.text, node.moduleSpecifier.text);
    if (ts.isVariableDeclaration(node) && node.initializer) {
      const modules = [];
      function scan(n) {
        if (ts.isCallExpression(n) && n.expression.kind === ts.SyntaxKind.ImportKeyword && ts.isStringLiteral(n.arguments[0])) modules.push(n.arguments[0].text);
        ts.forEachChild(n, scan);
      }
      scan(node.initializer);
      if (modules.length === 1) imports.set(node.name.getText(source), modules[0]);
    }
    ts.forEachChild(node, collect);
  }
  collect(source);
  assert.equal(imports.get('ProtectedRoute'), './core/components/auth/ProtectedRoute', 'Wrong guard implementation');
  const found = [];
  function visit(node, parentPath = '', guards = []) {
    const tag = ts.isJsxElement(node) ? node.openingElement : ts.isJsxSelfClosingElement(node) ? node : null;
    if (tag?.tagName.getText(source) === 'Route') {
      const a = attrs(tag);
      let routePath = parentPath;
      if (a.path) {
        assert.ok(ts.isStringLiteral(a.path), 'Route paths must be static');
        routePath = a.path.text.startsWith('/') ? a.path.text : parentPath + '/' + a.path.text;
      }
      const element = a.element && ts.isJsxExpression(a.element) ? a.element.expression : null;
      const component = element && ts.isJsxSelfClosingElement(element) ? element.tagName.getText(source) : null;
      if (component === 'ProtectedRoute') {
        const roles = attrs(element).roles;
        assert.ok(!roles || (ts.isJsxExpression(roles) && ts.isArrayLiteralExpression(roles.expression) && roles.expression.elements.every(ts.isStringLiteral)), 'Guard roles must be static');
        guards = [...guards, roles ? roles.expression.elements.map(r => r.text) : []];
      }
      found.push({ routePath, component, guards });
      parentPath = routePath;
    }
    ts.forEachChild(node, child => visit(child, parentPath, guards));
  }
  visit(source);
  for (const [routePath, component, modulePath, role] of contracts) {
    assert.equal(imports.get(component), modulePath, 'Wrong component import: ' + component);
    const matches = found.filter(r => r.routePath === routePath);
    assert.equal(matches.length, 1, 'Missing or duplicate route: ' + routePath);
    const route = matches[0];
    assert.equal(route.component, component, 'Wrong route component: ' + routePath);
    assert.equal(found.filter(r => r.component === component).length, 1, 'Duplicate component route: ' + component);
    if (role) assert.deepEqual(route.guards, [[role]], 'Wrong guard: ' + routePath);
    else assert.equal(route.guards.length, 0, 'Public route became protected: ' + routePath);
  }
  return {status: 'PASS', scope: 'LIVE_MEMBER1_ROUTE_CONTRACTS', routes: contracts.length};
}
module.exports = {verify};
if (require.main === module) {
  try { console.log(JSON.stringify(verify(fs.readFileSync(path.resolve(__dirname, '../../frontend/src/App.tsx'), 'utf8')))); }
  catch (error) { console.error(error.message); process.exitCode = 1; }
}
