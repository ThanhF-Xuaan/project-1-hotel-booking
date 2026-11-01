import pathlib
import subprocess
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
SCRIPT = ROOT / 'tools/member1/reconcile_utility_route.cjs'


class UtilityRouteTest(unittest.TestCase):
    def check(self, raw):
        return subprocess.run(['node', str(SCRIPT)], input=raw, capture_output=True)

    def test_exact_lf_crlf_project_to_previous_app(self):
        raw = (ROOT / 'frontend/src/App.tsx').read_bytes().replace(b'\r\n', b'\n')
        base = subprocess.check_output(['git', 'show', '5af00a7ab8652a58a488dc9a1bda59f2d5e1a2ad:frontend/src/App.tsx'], cwd=ROOT).replace(b'\r\n', b'\n')
        for text in [raw, raw.replace(b'\n', b'\r\n')]:
            result = self.check(text)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual(result.stdout, base)

    def test_guard_path_import_or_prior_route_mutation_rejected(self):
        raw = (ROOT / 'frontend/src/App.tsx').read_bytes()
        for before, after in [(b"roles={['CHAIN_ADMIN']}", b"roles={['CUSTOMER']}"),
                              (b'/ai/demo/utility-anomalies', b'/public/utility'),
                              (b'./modules/analytics/pages/UtilityDemoPage', b'./other/Page'),
                              (b'/ai/demo/occupancy', b'/changed/occupancy')]:
            with self.subTest(before=before):
                self.assertNotEqual(self.check(raw.replace(before, after)).returncode, 0)

    def test_missing_duplicate_relocated_or_unrelated_addition_rejected(self):
        raw = (ROOT / 'frontend/src/App.tsx').read_bytes().replace(b'\r\n', b'\n')
        block = b"          <Route element={<ProtectedRoute roles={['CHAIN_ADMIN']} />}>\n" + b'            <Route path="/ai/demo/utility-anomalies" element={<UtilityDemoPage />} />\n          </Route>\n\n'
        for value in [raw.replace(block, b''), raw.replace(block, block * 2),
                      raw.replace(block, b'').replace(b'        <Routes>\n', b'        <Routes>\n' + block), raw + b'\n// mutation\n']:
            self.assertNotEqual(self.check(value).returncode, 0)


if __name__ == '__main__':
    unittest.main()
