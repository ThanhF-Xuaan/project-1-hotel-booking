"""Verify an exact frozen synthetic export directory, read-only. Not a real-data importer."""
from __future__ import annotations

import argparse
import json
import os
from pathlib import Path, PurePosixPath
import stat

from generate_m1_mock_data import build_files

MAX_FILES = 24
MAX_ENTRIES = 27  # 24 files and the three fixed one-level directories; root is excluded.
MAX_BYTES = 1_000_000
REPARSE_POINT = getattr(stat, 'FILE_ATTRIBUTE_REPARSE_POINT', 0x400)


class VerificationError(ValueError):
    """Only fixed reason codes are safe to expose; never attach input names or bytes."""


def _require(condition: bool, reason: str) -> None:
    if not condition:
        raise VerificationError(reason)


def _identity(info: os.stat_result) -> tuple:
    return (info.st_dev, info.st_ino, info.st_mode, info.st_nlink, info.st_size,
            info.st_mtime_ns, getattr(info, 'st_file_attributes', 0))


def _version(info: os.stat_result) -> tuple:
    # Compare ctime only within the same API: Windows stat/fstat may define it differently.
    return (*_identity(info), info.st_ctime_ns)


def _safe(info: os.stat_result, directory: bool) -> None:
    _require(not getattr(info, 'st_file_attributes', 0) & REPARSE_POINT, 'UNSAFE_ENTRY')
    _require((stat.S_ISDIR if directory else stat.S_ISREG)(info.st_mode), 'UNSAFE_ENTRY')
    _require(info.st_ino != 0 and (directory or info.st_nlink == 1), 'UNSAFE_ENTRY')


def _unchanged(path: Path, original: os.stat_result, directory: bool) -> None:
    current = path.lstat()
    _safe(current, directory)
    _require(_version(current) == _version(original), 'PACK_CHANGED')


def _read(path: Path, original: os.stat_result, expected: bytes) -> None:
    _unchanged(path, original, False)
    _require(original.st_size == len(expected), 'CONTENT_MISMATCH')
    flags = os.O_RDONLY | getattr(os, 'O_BINARY', 0) | getattr(os, 'O_NOFOLLOW', 0)
    flags |= getattr(os, 'O_NONBLOCK', 0) | getattr(os, 'O_CLOEXEC', 0)
    fd = os.open(path, flags)
    try:
        opened = os.fstat(fd)
        _safe(opened, False)
        _require(_identity(opened) == _identity(original), 'PACK_CHANGED')
        remaining = len(expected) + 1
        chunks = []
        while remaining:
            chunk = os.read(fd, min(65_536, remaining))
            if not chunk:
                break
            chunks.append(chunk)
            remaining -= len(chunk)
        _require(b''.join(chunks) == expected, 'CONTENT_MISMATCH')
        _require(_version(os.fstat(fd)) == _version(opened), 'PACK_CHANGED')
        _unchanged(path, original, False)
    finally:
        os.close(fd)


def verify_pack(directory: Path) -> dict:
    # The input manifest is never parsed or trusted. Build the complete trusted byte oracle.
    try:
        expected = build_files()
        manifest = json.loads(expected['manifest.json'])
        _require(len(expected) == MAX_FILES and sum(map(len, expected.values())) <= MAX_BYTES,
                 'SOURCE_UNAVAILABLE')
        paths = {PurePosixPath(name) for name in expected}
        _require(all(not p.is_absolute() and '..' not in p.parts and len(p.parts) <= 2 for p in paths),
                 'SOURCE_UNAVAILABLE')
        subdirs = {p.parts[0] for p in paths if len(p.parts) == 2}
        _require(subdirs == {'occupancy', 'utility', 'group'}, 'SOURCE_UNAVAILABLE')
    except (OSError, ValueError, KeyError, TypeError):
        raise VerificationError('SOURCE_UNAVAILABLE') from None
    try:
        _require('..' not in directory.parts, 'UNSAFE_PATH')
        root = directory.absolute()  # Do not resolve away a symlink before checking it.
        for ancestor in reversed(root.parents):
            _safe(ancestor.lstat(), True)
        root_info = root.lstat()
        _safe(root_info, True)
        directories = {root: root_info}
        files = {}
        visited = 0

        def scan(folder: Path, allowed: dict[str, bool]) -> None:
            nonlocal visited
            seen = set()
            with os.scandir(folder) as entries:
                for entry in entries:
                    visited += 1
                    _require(visited <= MAX_ENTRIES and entry.name in allowed and entry.name not in seen,
                             'LAYOUT_MISMATCH')
                    seen.add(entry.name)
                    # Windows DirEntry.stat caches zero inode/device/link count; lstat does not.
                    info = (folder / entry.name).lstat()
                    is_directory = allowed[entry.name]
                    _safe(info, is_directory)
                    (directories if is_directory else files)[folder / entry.name] = info
            _require(seen == set(allowed), 'LAYOUT_MISMATCH')

        scan(root, {**{p.name: False for p in paths if len(p.parts) == 1},
                    **{name: True for name in subdirs}})
        for name in sorted(subdirs):
            folder = root / name
            _unchanged(root, root_info, True)
            _unchanged(folder, directories[folder], True)
            scan(folder, {p.name: False for p in paths if len(p.parts) == 2 and p.parts[0] == name})
        _require(len(files) == MAX_FILES, 'LAYOUT_MISMATCH')
        for name, content in sorted(expected.items()):
            path = root.joinpath(*PurePosixPath(name).parts)
            _unchanged(root, root_info, True)
            _unchanged(path.parent, directories[path.parent], True)
            _read(path, files[path], content)
        for path, info in directories.items():
            _unchanged(path, info, True)
    except VerificationError:
        raise
    except (OSError, ValueError):
        raise VerificationError('PACK_IO_ERROR') from None
    return {'status': 'PASS', 'verificationBasis': 'EXACT_FROZEN_EXPORT_V1',
            'dataMode': 'SYNTHETIC', 'productionReady': False,
            'filesVerified': len(expected), 'bytesVerified': sum(map(len, expected.values())),
            'counts': manifest['counts']}


class _Parser(argparse.ArgumentParser):
    def error(self, message: str) -> None:
        # argparse's default error echoes untrusted CLI values.
        print(json.dumps({'status': 'FAIL', 'reason': 'INVALID_ARGUMENTS'}))
        raise SystemExit(2)


def main(argv: list[str] | None = None) -> int:
    parser = _Parser(description=__doc__)
    parser.add_argument('--input-directory', type=Path, required=True)
    args = parser.parse_args(argv)
    try:
        result = verify_pack(args.input_directory)
    except VerificationError as error:
        print(json.dumps({'status': 'FAIL', 'reason': str(error)}))
        return 1
    print(json.dumps(result))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
