const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const {verify} = require('./verify_member1_routes.cjs');
const source = fs.readFileSync(path.resolve(__dirname, '../../frontend/src/App.tsx'), 'utf8');
test('current contracts pass with LF and CRLF', () => {
  verify(source); verify(source.replace(/\r?\n/g, '\r\n'));
});
test('unrelated route additions and formatting are accepted', () => {
  verify(source.replace('<Routes>', '<Routes>\n<Route path="/new-member4-page" element={<div />} />'));
  verify(source + '\n// harmless comment\n');
});
for (const [name, before, after] of [
  ['wrong role', "roles={['CHAIN_ADMIN']}", "roles={['CUSTOMER']}"],
  ['wrong path', '/ai/demo/occupancy', '/changed'],
  ['wrong import', './modules/analytics/pages/OccupancyDemoPage', './other/Page'],
  ['missing route', '<Route path="/ai/demo/occupancy" element={<OccupancyDemoPage />} />', ''],
  ['duplicate route', '<Route path="/ai/demo/occupancy" element={<OccupancyDemoPage />} />', '<Route path="/ai/demo/occupancy" element={<OccupancyDemoPage />} /><Route path="/ai/demo/occupancy" element={<OccupancyDemoPage />} />'],
  ['removed guard', "<ProtectedRoute roles={['CHAIN_ADMIN']} />", '<div />'],
]) test(name + ' is rejected', () => {
  assert.ok(source.includes(before)); assert.throws(() => verify(source.replace(before, after)));
});
