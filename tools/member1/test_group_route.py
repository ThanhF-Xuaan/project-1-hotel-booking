import pathlib
import subprocess
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
SCRIPT = ROOT / 'tools/member1/reconcile_group_route.cjs'


class GroupRouteTest(unittest.TestCase):
    def check(self, raw):
        return subprocess.run(['node', str(SCRIPT)], input=raw, capture_output=True)

    def test_exact_lf_crlf_preserve_previous_app(self):
        raw = (ROOT / 'frontend/src/App.tsx').read_bytes().replace(b'\r\n', b'\n')
        base = subprocess.check_output(['git', 'show', '2042240e9e6c6b213cd726415903b83da249a1f4:frontend/src/App.tsx'], cwd=ROOT).replace(b'\r\n', b'\n')
        for value in (raw, raw.replace(b'\n', b'\r\n')):
            result = self.check(value)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual(result.stdout, base)

    def test_guard_path_import_and_prior_route_tamper_rejected(self):
        raw = (ROOT / 'frontend/src/App.tsx').read_bytes()
        for before, after in [(b"roles={['CHAIN_ADMIN']}", b"roles={['CUSTOMER']}"),
                              (b'/ai/demo/group-room-assignment', b'/public/group'),
                              (b'./modules/analytics/pages/GroupDemoPage', b'./other/Page'),
                              (b'/ai/demo/utility-anomalies', b'/changed/utility')]:
            with self.subTest(before=before):
                self.assertNotEqual(self.check(raw.replace(before, after)).returncode, 0)

    def test_missing_duplicate_relocated_and_unrelated_changes_rejected(self):
        raw = (ROOT / 'frontend/src/App.tsx').read_bytes().replace(b'\r\n', b'\n')
        block = b"          <Route element={<ProtectedRoute roles={['CHAIN_ADMIN']} />}>\n" + b'            <Route path="/ai/demo/group-room-assignment" element={<GroupDemoPage />} />\n          </Route>\n\n'
        for value in (raw.replace(block, b''), raw.replace(block, block * 2),
                      raw.replace(block, b'').replace(b'        <Routes>\n', b'        <Routes>\n' + block), raw + b'\n// drift\n'):
            self.assertNotEqual(self.check(value).returncode, 0)


if __name__ == '__main__':
    unittest.main()
