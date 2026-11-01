"""Reject unreviewed upstream changes without rewriting historical source evidence."""
import copy
import json
import subprocess
import unittest
from unittest.mock import patch

import reconcile_deploy_source as verifier


class DeploySourceReauditTest(unittest.TestCase):
    def test_exact_reviewed_lf_and_crlf_sources(self):
        for path in verifier.PATHS:
            actual = (verifier.ROOT / path).read_bytes()
            expected = verifier.blob(verifier.BASE, path)
            for raw in (verifier.lf(actual), verifier.lf(actual).replace(b'\n', b'\r\n')):
                with self.subTest(path=path):
                    self.assertEqual(verifier.reconcile(path, raw), expected)

    def test_each_source_mutation_rejected(self):
        for path in verifier.PATHS:
            with self.subTest(path=path), self.assertRaisesRegex(ValueError, 'reviewed deployment commit'):
                verifier.reconcile(path, (verifier.ROOT / path).read_bytes() + b'\n# unreviewed\n')

    def test_old_source_cannot_be_substituted_for_current(self):
        for path in verifier.PATHS:
            with self.subTest(path=path), self.assertRaisesRegex(ValueError, 'reviewed deployment commit'):
                verifier.reconcile(path, verifier.blob(verifier.BASE, path))

    def test_receipt_changes_rejected(self):
        original = verifier.load_receipt()
        for change in ('hash', 'identity', 'missing', 'extra', 'review'):
            altered = copy.deepcopy(original)
            if change == 'hash': altered['files'][0]['afterLfSha256'] = '0' * 64
            if change == 'identity': altered['reviewedCommit'] = verifier.BASE
            if change == 'missing': altered['files'].pop()
            if change == 'extra': altered['files'].append(copy.deepcopy(altered['files'][0]))
            if change == 'review': altered['files'][0]['review'] = ''
            with self.subTest(change=change), self.assertRaisesRegex(ValueError, 'source receipt'):
                verifier.load_receipt(json.dumps(altered).encode())

    def node(self, path, raw):
        script = """
const fs=require('node:fs'), hash=require('node:crypto');
const r=require('./tools/t12/reconcile_deploy_source.cjs');
const row=JSON.parse(fs.readFileSync(0,'utf8'));
try { const result=r.reconcile(process.cwd(),row.path,row.source);
console.log(hash.createHash('sha256').update(result).digest('hex')); }
catch { process.exit(7); }
"""
        return subprocess.run(['node', '-e', script], cwd=verifier.ROOT,
                              input=json.dumps({'path': path, 'source': raw.decode('utf-8')}),
                              text=True, capture_output=True)

    def test_node_and_python_reconciliation_match(self):
        for path in verifier.PATHS:
            with self.subTest(path=path):
                raw = (verifier.ROOT / path).read_bytes()
                result = self.node(path, raw)
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertEqual(result.stdout.strip(), verifier.sha(verifier.reconcile(path, raw)))

    def test_node_rejects_every_source_mutation(self):
        for path in verifier.PATHS:
            with self.subTest(path=path):
                self.assertEqual(self.node(path, (verifier.ROOT / path).read_bytes() + b'\n// drift\n').returncode, 7)

    def test_unlisted_path_is_unchanged(self):
        raw = b'unrelated source'
        self.assertIs(verifier.reconcile('unknown/source.txt', raw), raw)
        result = self.node('unknown/source.txt', raw)
        self.assertEqual(result.returncode, 0)
        self.assertEqual(result.stdout.strip(), verifier.sha(raw))

    def test_git_patch_tamper_rejected(self):
        path = 'backend/pom.xml'
        with patch.object(verifier, 'delta', return_value=b'forged delta'):
            with self.assertRaisesRegex(ValueError, 'patch mismatch'):
                verifier.reconcile(path, (verifier.ROOT / path).read_bytes())


if __name__ == '__main__':
    unittest.main()
