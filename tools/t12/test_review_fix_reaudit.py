"""Reject source, receipt and scope tampering without changing historical evidence."""
import pathlib
import unittest
from unittest.mock import patch
import reconcile_m1_review_fixes as audit


class ReviewFixReauditTest(unittest.TestCase):
    def test_current_delta_is_pinned(self):
        self.assertEqual(audit.verify_current()['files'], 19)

    def test_every_source_mutation_is_rejected(self):
        original = pathlib.Path.read_bytes
        for row in audit.load_receipt()['files']:
            target = audit.ROOT / row['path']
            with self.subTest(path=row['path']):
                def reader(path):
                    raw = original(path)
                    return raw + b'\n// unreviewed\n' if path == target else raw
                with patch.object(pathlib.Path, 'read_bytes', reader):
                    with self.assertRaises(ValueError):
                        audit.verify_current()

    def test_receipt_mutation_is_rejected(self):
        raw = (audit.ROOT / audit.RECEIPT).read_bytes()
        with self.assertRaises(ValueError):
            audit.load_receipt(raw + b' ')

    def test_line_endings_are_portable_and_unrelated_files_are_unchanged(self):
        row = audit.load_receipt()['files'][0]
        actual = (audit.ROOT / row['path']).read_bytes()
        self.assertEqual(audit.reconcile(row['path'], audit.lf(actual).replace(b'\n', b'\r\n')),
                         audit.blob(audit.BASE, row['path']))
        self.assertEqual(audit.reconcile('unrelated.txt', b'unchanged'), b'unchanged')


if __name__ == '__main__':
    unittest.main()
