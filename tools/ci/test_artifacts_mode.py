import contextlib
import io
import json
from pathlib import Path
import sys
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 't12'))
import validate_r1_contracts as validator


class ArtifactsModeTest(unittest.TestCase):
    def run_validator(self):
        output = io.StringIO()
        with patch.object(sys, 'argv', ['validate', '--artifacts-only']), contextlib.redirect_stdout(output):
            validator.main()
        return json.loads(output.getvalue())

    def test_artifacts_mode_keeps_negative_checks_and_reports_source_not_checked(self):
        report = self.run_validator()
        self.assertEqual(report['liveSourceProvenance'], 'NOT_CHECKED')
        self.assertEqual(report['counters']['negative_cases_rejected'], 33)
        self.assertEqual(report['counters']['frozen_artifact_blobs_checked'], 41)
        self.assertNotIn('sourceReconciliation', report)

    def test_frozen_content_change_rejected(self):
        original = Path.read_bytes
        def read(path):
            value = original(path)
            return value + b' ' if path == validator.EVAL / 'cases.json' else value
        with patch.object(Path, 'read_bytes', read), self.assertRaisesRegex(ValueError, 'Frozen artifact changed'):
            self.run_validator()

    def test_live_app_source_is_not_used_as_historical_evidence(self):
        original = Path.read_bytes
        def read(path):
            if path == validator.ROOT / 'frontend/src/App.tsx':
                raise AssertionError('Live App must be checked by route contracts')
            return original(path)
        with patch.object(Path, 'read_bytes', read):
            self.run_validator()

    def test_modes_are_mutually_exclusive(self):
        with patch.object(sys, 'argv', ['validate', '--artifacts-only', '--reviewed-widget-sources']), contextlib.redirect_stderr(io.StringIO()), self.assertRaises(SystemExit) as error:
            validator.main()
        self.assertEqual(error.exception.code, 2)


if __name__ == '__main__':
    unittest.main()
