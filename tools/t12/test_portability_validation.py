"""Unit tests for newline-portable source evidence verification in tools/t12/validate_r1_contracts.py."""
from __future__ import annotations

import json
import pathlib
import subprocess
import sys
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools/t12'))

from validate_r1_contracts import verify_protected_file_entry, verify_source_evidence_entry


class TestPortabilityValidation(unittest.TestCase):
    """Test suite demonstrating CRLF and LF portability and strict tampering rejection."""

    @classmethod
    def setUpClass(cls):
        evidence_path = ROOT / 'docs/t12/r1-00/v2.0.0-draft.1/baseline-source-evidence.json'
        cls.evidence = json.loads(evidence_path.read_text(encoding='utf-8'))
        cls.entries = {e['path']: e for e in cls.evidence['files']}

        cls.sec_entry = cls.entries['backend/src/main/java/vn/edu/utc/hotel_booking/common/config/SecurityConfig.java']
        cls.error_entry = cls.entries['backend/src/main/java/vn/edu/utc/hotel_booking/common/exception/ErrorCode.java']

        # Real disk content
        sec_disk = (ROOT / cls.sec_entry['path']).read_bytes()
        cls.sec_crlf = sec_disk if b'\r\n' in sec_disk else sec_disk.replace(b'\n', b'\r\n')
        cls.sec_lf = sec_disk.replace(b'\r\n', b'\n')

        error_disk = (ROOT / cls.error_entry['path']).read_bytes()
        cls.error_crlf = error_disk if b'\r\n' in error_disk else error_disk.replace(b'\n', b'\r\n')
        cls.error_lf = error_disk.replace(b'\r\n', b'\n')

        protected_path = '.agents/rules/00-INDEX.template.md'
        protected_doc = json.loads((ROOT / 'docs/t12/r1-00/v2.0.0-draft.1/protected-user-files.json')
                                  .read_text(encoding='utf-8'))
        cls.protected_entry = next(e for e in protected_doc['files'] if e['path'] == protected_path)
        protected_disk = (ROOT / protected_path).read_bytes()
        cls.protected_crlf = protected_disk if b'\r\n' in protected_disk else protected_disk.replace(b'\n', b'\r\n')
        cls.protected_lf = protected_disk.replace(b'\r\n', b'\n')

    def test_security_config_crlf_passes(self):
        """Legitimate Windows CRLF bytes for SecurityConfig.java must pass."""
        self.assertTrue(verify_source_evidence_entry(self.sec_entry, self.sec_crlf))

    def test_security_config_canonical_lf_passes(self):
        """Legitimate Canonical LF bytes for SecurityConfig.java must pass."""
        self.assertTrue(verify_source_evidence_entry(self.sec_entry, self.sec_lf))

    def test_security_config_tampered_crlf_fails(self):
        """Tampering content in CRLF representation must fail verification."""
        tampered = self.sec_crlf.replace(b'Retry-After', b'Retry-Never')
        self.assertFalse(verify_source_evidence_entry(self.sec_entry, tampered))

    def test_security_config_tampered_lf_fails(self):
        """Tampering content in LF representation must fail verification."""
        tampered = self.sec_lf.replace(b'Retry-After', b'Retry-Never')
        self.assertFalse(verify_source_evidence_entry(self.sec_entry, tampered))

    def test_security_config_extra_whitespace_tamper_fails(self):
        """Adding extra trailing whitespace or newline must fail verification."""
        tampered_crlf = self.sec_crlf + b'\r\n// unauthorized trailing comment\r\n'
        tampered_lf = self.sec_lf + b'\n// unauthorized trailing comment\n'
        self.assertFalse(verify_source_evidence_entry(self.sec_entry, tampered_crlf))
        self.assertFalse(verify_source_evidence_entry(self.sec_entry, tampered_lf))

    def test_unaffected_file_crlf_passes(self):
        """Unaffected baseline files with recorded hashes must pass."""
        self.assertTrue(verify_source_evidence_entry(self.error_entry, self.error_crlf))

    def test_unaffected_file_tampering_fails(self):
        """Tampering content in unaffected baseline files must fail."""
        tampered = self.error_crlf.replace(b'ErrorCode', b'TamperedCode')
        self.assertFalse(verify_source_evidence_entry(self.error_entry, tampered))

    def test_unaffected_file_lf_fails_when_not_designated(self):
        """Unaffected file without designated canonical LF hash fails if converted to LF (strictness proof)."""
        # ErrorCode.java does NOT designate unixLfSha256 / canonicalSha256, so strict byte hash applies
        self.assertFalse(verify_source_evidence_entry(self.error_entry, self.error_lf))

    def test_protected_file_accepts_canonical_lf_and_windows_crlf(self):
        """Protected files retain content checks while tolerating Git's Windows line-ending conversion."""
        self.assertTrue(verify_protected_file_entry(self.protected_entry, self.protected_lf))
        self.assertTrue(verify_protected_file_entry(self.protected_entry, self.protected_crlf))

    def test_protected_file_content_tampering_fails_after_line_ending_normalization(self):
        tampered = self.protected_crlf.replace(b'template', b'tampered', 1)
        self.assertFalse(verify_protected_file_entry(self.protected_entry, tampered))

    def test_full_contract_validator_execution(self):
        """The full offline validator command must execute and report PASS."""
        res = subprocess.run(
            [sys.executable, '-B', str(ROOT / 'tools/t12/validate_r1_contracts.py')],
            capture_output=True,
            text=True,
            cwd=str(ROOT),
        )
        self.assertEqual(res.returncode, 0, f"Validator failed: {res.stderr}\n{res.stdout}")
        parsed = json.loads(res.stdout)
        self.assertEqual(parsed['status'], 'PASS')


if __name__ == '__main__':
    unittest.main()
