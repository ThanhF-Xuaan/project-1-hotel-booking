"""Positive and tamper cases for the fixed, explicit source reconciliation mode."""
import copy
from functools import lru_cache
import json
import subprocess
import sys
import unittest
from unittest.mock import patch

import verify_widget_source_reaudit as verifier


class WidgetSourceReauditTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.audit = verifier.load_audit()
        # Git objects are immutable; cache reads, never the working source checks.
        cls.git_cache = patch.object(verifier, 'git', lru_cache(maxsize=None)(verifier.git))
        cls.git_cache.start()

    @classmethod
    def tearDownClass(cls):
        cls.git_cache.stop()

    def assert_rejected(self, mutate=None, changed_path=None, changed_bytes=None):
        audit = copy.deepcopy(self.audit)
        if mutate:
            mutate(audit)
        def reader(path):
            raw = (verifier.ROOT / path).read_bytes()
            return (changed_bytes if changed_bytes is not None else raw + b'\n// tampered\n') if path == changed_path else raw
        with self.assertRaises(ValueError):
            verifier.verify_reconciliation(audit, reader)

    def test_exact_reviewed_sources_pass(self):
        self.assertEqual(verifier.verify_reconciliation()['reconciledSourceFiles'], 5)

    def test_crlf_working_sources_pass(self):
        result = verifier.verify_reconciliation(read_working=lambda path:
            verifier.lf((verifier.ROOT / path).read_bytes()).replace(b'\n', b'\r\n'))
        self.assertEqual(result['readDependenciesVerified'], 4)

    def test_unknown_code_commit_rejected(self):
        self.assert_rejected(lambda audit: audit.update(reviewedCodeCommit='0' * 40))

    def test_unknown_frozen_commit_rejected(self):
        self.assert_rejected(lambda audit: audit.update(frozenCommit='0' * 40))

    def test_wrong_manifest_rejected(self):
        self.assert_rejected(lambda audit: audit.update(manifestPath='../other.json'))

    def test_wrong_manifest_hash_rejected(self):
        self.assert_rejected(lambda audit: audit.update(manifestLfSha256='0' * 64))

    def test_missing_source_rejected(self):
        self.assert_rejected(lambda audit: audit['files'].pop())

    def test_extra_or_duplicate_source_rejected(self):
        self.assert_rejected(lambda audit: audit['files'].append(copy.deepcopy(audit['files'][0])))

    def test_changed_prior_commit_rejected(self):
        self.assert_rejected(lambda audit: audit['files'][0].update(priorCommit=verifier.CODE_COMMIT))

    def test_changed_prior_hash_rejected(self):
        self.assert_rejected(lambda audit: audit['files'][0].update(priorLfSha256='0' * 64))

    def test_changed_current_hash_rejected(self):
        self.assert_rejected(lambda audit: audit['files'][0].update(currentLfSha256='0' * 64))

    def test_changed_patch_rejected(self):
        self.assert_rejected(lambda audit: audit['files'][0].update(patchLfSha256='0' * 64))

    def test_each_reviewed_working_source_mutation_rejected(self):
        for path in verifier.PRIOR_COMMITS:
            with self.subTest(path=path):
                self.assert_rejected(changed_path=path)

    def test_unreconciled_manifest_source_mutation_rejected(self):
        path = 'backend/src/main/java/vn/edu/utc/hotel_booking/common/config/SecurityConfig.java'
        self.assert_rejected(changed_path=path)

    def test_gold_and_manifest_mutations_rejected(self):
        for path in (verifier.MANIFEST, 'eval/t12/r1/v0.1.0-draft/cases.json',
                     'docs/t12/r1-00/v2.0.0-draft.1/examples/results-response.json'):
            with self.subTest(path=path):
                self.assert_rejected(changed_path=path)

    def test_read_dependency_mutation_rejected(self):
        for path in verifier.READ_DEPENDENCIES:
            with self.subTest(path=path):
                self.assert_rejected(changed_path=path)

    def test_duplicate_json_key_rejected(self):
        with self.assertRaises(ValueError):
            verifier.unique_pairs([('status', 'REVIEWED_SOURCE_ONLY'), ('status', 'BYPASS')])

    def test_default_validator_still_rejects_historical_source_drift(self):
        result = subprocess.run([sys.executable, '-B', str(verifier.ROOT / 'tools/t12/validate_r1_contracts.py')],
                                capture_output=True, text=True, cwd=verifier.ROOT)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('Source changed after audit:', result.stderr)

    def test_explicit_validator_runs_original_semantic_and_negative_checks(self):
        result = subprocess.run([sys.executable, '-B', str(verifier.ROOT / 'tools/t12/validate_r1_contracts.py'),
                                 '--reviewed-widget-sources'], capture_output=True, text=True, cwd=verifier.ROOT)
        self.assertEqual(result.returncode, 0, result.stderr)
        report = json.loads(result.stdout)
        self.assertEqual(report['scope'], 'ARTIFACT_VALIDATION_ONLY')
        self.assertEqual(report['engineEvaluation'], 'NOT_RUN')
        self.assertEqual(report['counters']['negative_cases_rejected'], 33)
        self.assertEqual(report['counters']['evaluation_cases_checked'], 74)
        self.assertEqual(report['sourceReconciliation']['reconciledSourceFiles'], 5)


if __name__ == '__main__':
    unittest.main()
