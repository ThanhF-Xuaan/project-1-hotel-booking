"""Tamper checks for composing old provenance with fixed reviewed current source."""
import pathlib
import unittest
from unittest.mock import patch
import verify_current_core_reaudit as current
import verify_widget_source_reaudit as widget


class CurrentReauditTest(unittest.TestCase):
    def test_current_sources_pass(self):
        self.assertEqual(current.verify_current()['status'], 'PASS')

    def test_every_reviewed_source_mutation_is_rejected(self):
        original = pathlib.Path.read_bytes
        for relative in widget.PRIOR_COMMITS:
            target = widget.ROOT / relative
            with self.subTest(path=relative):
                def reader(path, target=target):
                    raw = original(path)
                    return raw + b'\n// unreviewed mutation\n' if path == target else raw
                with patch.object(pathlib.Path, 'read_bytes', reader):
                    with self.assertRaises(ValueError):
                        current.verify_current()

    def test_current_read_dependency_mutation_is_rejected(self):
        original = pathlib.Path.read_bytes
        target = widget.ROOT / widget.READ_DEPENDENCIES[0]
        def reader(path):
            raw = original(path)
            return raw + b'\n// changed provider\n' if path == target else raw
        with patch.object(pathlib.Path, 'read_bytes', reader):
            with self.assertRaises(ValueError):
                current.verify_current()

    def test_u14_route_mutation_is_rejected(self):
        original = pathlib.Path.read_bytes
        target = widget.ROOT / 'frontend/src/App.tsx'
        def reader(path):
            raw = original(path)
            return raw + b'\n// changed route\n' if path == target else raw
        with patch.object(pathlib.Path, 'read_bytes', reader):
            with self.assertRaises(ValueError):
                current.verify_current()


if __name__ == '__main__':
    unittest.main()
