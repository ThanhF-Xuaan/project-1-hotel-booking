import pathlib
import subprocess
import unittest
import reconcile_u22_route as route

ROOT = pathlib.Path(__file__).resolve().parents[2]


class U22RouteTest(unittest.TestCase):
    def test_exact_lf_and_crlf_extension_preserves_previous_app(self):
        actual = (ROOT / route.APP).read_bytes().replace(b'\r\n', b'\n')
        base = subprocess.check_output(['git', 'show', f'35fb85a830b2f2315469faf289b303f795bf9380:{route.APP}'], cwd=ROOT)
        for value in [actual, actual.replace(b'\n', b'\r\n')]:
            self.assertEqual(route.reconcile(value), base.replace(b'\r\n', b'\n'))

    def test_guard_path_import_and_unrelated_route_mutations_rejected(self):
        raw = (ROOT / route.APP).read_bytes().replace(b'\r\n', b'\n')
        for before, after in [
            (b"roles={['CHAIN_ADMIN']}", b"roles={['CUSTOMER']}"),
            (b'/ai/demo/occupancy', b'/portal/demo/occupancy'),
            (b"./modules/analytics/pages/OccupancyDemoPage", b'./other/Page'),
            (b'path="/dashboard"', b'path="/changed"'),
        ]:
            with self.subTest(before=before), self.assertRaises(ValueError):
                route.reconcile(raw.replace(before, after))

    def test_missing_duplicate_or_relocated_extension_rejected(self):
        raw = (ROOT / route.APP).read_bytes().replace(b'\r\n', b'\n')
        opening = b"          <Route element={<ProtectedRoute roles={['CHAIN_ADMIN']} />}>\n"
        block = opening + b'            <Route path="/ai/demo/occupancy" element={<OccupancyDemoPage />} />\n          </Route>\n\n'
        for value in [raw.replace(block, b''), raw.replace(block, block * 2),
                      raw.replace(block, b'').replace(b'        <Routes>\n', b'        <Routes>\n' + block),
                      raw + b'\n// unrelated mutation\n']:
            with self.assertRaises(ValueError):
                route.reconcile(value)


if __name__ == '__main__':
    unittest.main()
