import hashlib
from contextlib import contextmanager
import json
import os
from pathlib import Path
import stat
import subprocess
import sys
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import patch

from generate_m1_mock_data import build_files, generate
import verify_m1_mock_pack as verifier


class MockPackVerifierTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.pack = self.root / 'pack'
        generate(self.pack)
        self.expected = build_files()

    def reject(self, reason=None):
        with self.assertRaises(verifier.VerificationError) as raised:
            verifier.verify_pack(self.pack)
        if reason:
            self.assertEqual(str(raised.exception), reason)

    def cli(self, *args):
        return subprocess.run([sys.executable, '-B', str(Path(verifier.__file__)), *args],
                              capture_output=True, text=True)

    def test_exact_pack_is_deterministic_read_only_and_synthetic(self):
        before = {name: ((self.pack / name).read_bytes(), (self.pack / name).stat().st_mtime_ns)
                  for name in self.expected}
        first = verifier.verify_pack(self.pack)
        self.assertEqual(first, verifier.verify_pack(self.pack))
        self.assertEqual((first['status'], first['filesVerified'], first['bytesVerified']), ('PASS', 24, 248104))
        self.assertEqual(first['dataMode'], 'SYNTHETIC')
        self.assertFalse(first['productionReady'])
        self.assertEqual(first['counts'], json.loads(self.expected['manifest.json'])['counts'])
        self.assertEqual(before, {name: ((self.pack / name).read_bytes(), (self.pack / name).stat().st_mtime_ns)
                                  for name in self.expected})

    def test_every_file_tamper_rejected_even_when_length_is_unchanged(self):
        for name, content in self.expected.items():
            with self.subTest(name=name):
                p = self.pack / name
                p.write_bytes(bytes([content[0] ^ 1]) + content[1:])
                self.reject('CONTENT_MISMATCH')
                p.write_bytes(content)

    def test_modified_payload_and_rehashed_manifest_cannot_self_approve(self):
        name = 'occupancy/hotel-1.json'
        raw = self.expected[name].replace(b'SYNTHETIC', b'LIVE_DATA')
        (self.pack / name).write_bytes(raw)
        manifest = json.loads(self.expected['manifest.json'])
        manifest['files'][name] = hashlib.sha256(raw).hexdigest()
        (self.pack / 'manifest.json').write_text(json.dumps(manifest), encoding='utf-8')
        self.reject('CONTENT_MISMATCH')

    def test_relabel_counts_or_oracle_injection_never_accepted(self):
        for change in ('mode', 'ready', 'counts', 'oracle'):
            with self.subTest(change=change):
                value = json.loads(self.expected['manifest.json'])
                if change == 'mode': value['dataMode'] = 'REAL'
                if change == 'ready': value['productionReady'] = True
                if change == 'counts': value['counts']['groupScenarios'] = 0
                if change == 'oracle': value['expected'] = {'assignedCount': 2}
                (self.pack / 'manifest.json').write_text(json.dumps(value), encoding='utf-8')
                self.reject('CONTENT_MISMATCH')

    def test_manifest_paths_are_not_used_to_open_outside_pack(self):
        secret = self.root / 'PRIVATE_MARKER.json'
        secret.write_text('SYNTHETIC_PRIVATE_MARKER', encoding='utf-8')
        value = json.loads(self.expected['manifest.json'])
        value['files']['../PRIVATE_MARKER.json'] = hashlib.sha256(secret.read_bytes()).hexdigest()
        (self.pack / 'manifest.json').write_text(json.dumps(value), encoding='utf-8')
        real_open = os.open
        def guarded(path, flags, *args, **kwargs):
            self.assertTrue(Path(path).is_relative_to(self.pack))
            return real_open(path, flags, *args, **kwargs)
        with patch.object(verifier.os, 'open', side_effect=guarded):
            self.reject('CONTENT_MISMATCH')

    def test_duplicate_keys_or_invalid_json_manifest_rejected(self):
        for content in (b'{"dataMode":"REAL","dataMode":"SYNTHETIC"}', b'{invalid', b'\xef\xbb\xbf{}'):
            (self.pack / 'manifest.json').write_bytes(content)
            self.reject('CONTENT_MISMATCH')

    def test_every_missing_file_rejected(self):
        for name, content in self.expected.items():
            with self.subTest(name=name):
                p = self.pack / name
                p.unlink()
                self.reject('LAYOUT_MISMATCH')
                p.write_bytes(content)

    def test_extra_file_or_nested_directory_is_rejected_before_payload_read(self):
        for p in (self.pack / 'extra.json', self.pack / 'group' / 'nested'):
            if p.suffix: p.write_bytes(b'PRIVATE_MARKER')
            else: p.mkdir()
            with patch.object(verifier.os, 'open', side_effect=AssertionError('Payload must not be opened')):
                self.reject('LAYOUT_MISMATCH')
            if p.is_dir(): p.rmdir()
            else: p.unlink()

    def test_missing_directory_rejected(self):
        for p in (self.pack / 'group').iterdir(): p.unlink()
        (self.pack / 'group').rmdir()
        self.reject('LAYOUT_MISMATCH')

    def test_oversized_and_truncated_file_rejected_without_open(self):
        p = self.pack / 'README.txt'
        for size in (0, 2_000_000):
            with p.open('wb') as stream: stream.truncate(size)
            with patch.object(verifier.os, 'open', side_effect=AssertionError('Oversize file opened')):
                self.reject('CONTENT_MISMATCH')

    def directory_alias(self, link, target):
        self.assertTrue(link.absolute().is_relative_to(self.root))
        self.assertTrue(target.absolute().is_relative_to(self.root))
        if os.name == 'nt':
            result = subprocess.run(['cmd', '/d', '/c', 'mklink', '/J', str(link), str(target)], capture_output=True)
            self.assertEqual(result.returncode, 0, 'Cannot create owned test junction')
        else:
            link.symlink_to(target, target_is_directory=True)

    def test_real_directory_alias_root_ancestor_and_child_are_rejected_without_read(self):
        alias = self.root / 'alias'
        self.directory_alias(alias, self.pack)
        ancestor = self.root / 'ancestor'
        self.directory_alias(ancestor, self.root)
        saved = self.root / 'saved-group'
        original = self.pack / 'group'
        self.assertTrue(original.resolve().is_relative_to(self.root))
        self.assertTrue(saved.absolute().is_relative_to(self.root))
        original.rename(saved)
        self.directory_alias(original, saved)
        for target in (alias, ancestor / 'pack', self.pack):
            with self.subTest(target=target.name), patch.object(verifier.os, 'open', side_effect=AssertionError('Link followed')):
                with self.assertRaisesRegex(verifier.VerificationError, 'UNSAFE_ENTRY'):
                    verifier.verify_pack(target)

    def test_hard_link_rejected_without_read(self):
        p = self.pack / 'README.txt'
        outside = self.root / 'hard-linked-readme'
        os.link(p, outside)
        with patch.object(verifier.os, 'open', side_effect=AssertionError('Hard link read')):
            self.reject('UNSAFE_ENTRY')

    def test_reparse_and_special_mode_guards(self):
        info = (self.pack / 'README.txt').stat()
        for mode, attributes in ((stat.S_IFLNK, 0), (stat.S_IFIFO, 0), (stat.S_IFSOCK, 0),
                                 (stat.S_IFREG, verifier.REPARSE_POINT)):
            fake = SimpleNamespace(st_mode=mode, st_ino=info.st_ino, st_nlink=1, st_file_attributes=attributes)
            with self.subTest(mode=mode, attributes=attributes), self.assertRaisesRegex(verifier.VerificationError, 'UNSAFE_ENTRY'):
                verifier._safe(fake, False)

    def test_actual_nonregular_file_is_rejected_before_open(self):
        p = self.pack / 'README.txt'
        p.unlink()
        if hasattr(os, 'mkfifo'):
            os.mkfifo(p)  # Exercise a real FIFO on POSIX; never block waiting for a writer.
        else:
            p.mkdir()  # Windows has no filesystem FIFO; a directory is also not a regular file.
        with patch.object(verifier.os, 'open', side_effect=AssertionError('Nonregular entry opened')):
            self.reject('UNSAFE_ENTRY')

    def test_no_stat_identity_fails_closed(self):
        with self.assertRaisesRegex(verifier.VerificationError, 'UNSAFE_ENTRY'):
            verifier._safe(SimpleNamespace(st_mode=stat.S_IFREG, st_ino=0, st_nlink=1), False)

    def test_replace_between_stat_and_open_rejected_before_payload_read(self):
        real_open = os.open
        def replaced(path, flags, *args, **kwargs):
            p = Path(path)
            replacement = self.pack / 'replacement.tmp'
            replacement.write_bytes(p.read_bytes())
            os.replace(replacement, p)
            return real_open(path, flags, *args, **kwargs)
        with patch.object(verifier.os, 'open', side_effect=replaced), patch.object(verifier.os, 'read', side_effect=AssertionError('Replacement was read')):
            self.reject('PACK_CHANGED')

    def test_changes_during_read_are_detected(self):
        real_read = os.read
        changed = False
        def mutated(fd, size):
            nonlocal changed
            data = real_read(fd, size)
            if not changed:
                changed = True
                p = self.pack / 'README.txt'
                raw = p.read_bytes()
                p.write_bytes(bytes([raw[0] ^ 1]) + raw[1:])
            return data
        with patch.object(verifier.os, 'read', side_effect=mutated):
            self.reject('PACK_CHANGED')

    def test_directory_changes_during_read_are_detected(self):
        real_read = os.read
        def changed(fd, size):
            data = real_read(fd, size)
            (self.pack / 'extra-after-scan').touch()
            return data
        with patch.object(verifier.os, 'read', side_effect=changed):
            self.reject('PACK_CHANGED')

    def test_partial_reads_and_readonly_open_flags(self):
        real_read, real_open = os.read, os.open
        def readonly(path, flags, *args, **kwargs):
            self.assertEqual(flags & (os.O_WRONLY | os.O_RDWR), 0)
            return real_open(path, flags, *args, **kwargs)
        with patch.object(verifier.os, 'read', side_effect=lambda fd, size: real_read(fd, min(size, 1024))), patch.object(verifier.os, 'open', side_effect=readonly):
            self.assertEqual(verifier.verify_pack(self.pack)['status'], 'PASS')

    def test_scandir_supplies_names_only_and_lstat_supplies_real_identity(self):
        real_scandir = os.scandir
        @contextmanager
        def names_only(path):
            with real_scandir(path) as entries:
                yield (SimpleNamespace(name=entry.name) for entry in entries)
        with patch.object(verifier.os, 'scandir', side_effect=names_only):
            self.assertEqual(verifier.verify_pack(self.pack)['status'], 'PASS')

    def test_stat_fstat_ctime_variation_is_not_identity_drift(self):
        real_fstat = os.fstat
        def other_ctime(fd):
            info = real_fstat(fd)
            values = {name: getattr(info, name) for name in ('st_dev', 'st_ino', 'st_mode', 'st_nlink', 'st_size', 'st_mtime_ns', 'st_ctime_ns')}
            values['st_file_attributes'] = getattr(info, 'st_file_attributes', 0)
            values['st_ctime_ns'] += 1000
            return SimpleNamespace(**values)
        with patch.object(verifier.os, 'fstat', side_effect=other_ctime):
            self.assertEqual(verifier.verify_pack(self.pack)['status'], 'PASS')

    def test_source_error_and_size_budget_fail_before_input_access(self):
        for value in (ValueError('PRIVATE_MARKER'), OSError('PRIVATE_MARKER')):
            with patch.object(verifier, 'build_files', side_effect=value), patch.object(Path, 'lstat', side_effect=AssertionError('Input accessed')):
                self.reject('SOURCE_UNAVAILABLE')
        huge = dict(self.expected)
        huge['README.txt'] = b'x' * verifier.MAX_BYTES
        with patch.object(verifier, 'build_files', return_value=huge), patch.object(Path, 'lstat', side_effect=AssertionError('Input accessed')):
            self.reject('SOURCE_UNAVAILABLE')

    def test_path_parent_segments_and_missing_root_fail_without_leaking(self):
        with self.assertRaisesRegex(verifier.VerificationError, 'UNSAFE_PATH'):
            verifier.verify_pack(self.pack / '..' / 'pack')
        with self.assertRaisesRegex(verifier.VerificationError, '^PACK_IO_ERROR$'):
            verifier.verify_pack(self.root / 'PRIVATE_MARKER_DO_NOT_ECHO')

    def test_cli_pass_fail_and_usage_are_sanitized_json(self):
        result = self.cli('--input-directory', str(self.pack))
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(json.loads(result.stdout)['status'], 'PASS')
        self.assertEqual(result.stderr, '')
        missing = self.root / 'PRIVATE_MARKER_DO_NOT_ECHO'
        result = self.cli('--input-directory', str(missing))
        self.assertEqual(result.returncode, 1)
        self.assertEqual(json.loads(result.stdout), {'status': 'FAIL', 'reason': 'PACK_IO_ERROR'})
        for args in ([], ['--PRIVATE_MARKER_DO_NOT_ECHO'], ['--input-directory', str(self.pack), 'PRIVATE_MARKER_DO_NOT_ECHO']):
            result = self.cli(*args)
            self.assertEqual(result.returncode, 2)
            self.assertEqual(json.loads(result.stdout), {'status': 'FAIL', 'reason': 'INVALID_ARGUMENTS'})
            self.assertEqual(result.stderr, '')
            self.assertNotIn('PRIVATE_MARKER', result.stdout)


if __name__ == '__main__':
    unittest.main()
