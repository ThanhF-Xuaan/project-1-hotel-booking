"""Verify the explicitly reviewed T12 widget sources without rewriting historical evidence."""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]
MANIFEST = 'docs/t12/r1-00/v2.0.0-draft.1/baseline-source-evidence.json'
FROZEN_COMMIT = '31276ac0ce062d418a580ceb466a4371bc4235f4'
CODE_COMMIT = '3f0cb2eacab3a5a2ca8c1155f187ff1ae6640afb'
AUDIT = 'docs/member1/contracts/t12-01-source-reaudit-2026-10-29.json'
PRIOR_COMMITS = {
    'backend/src/main/java/vn/edu/utc/hotel_booking/common/exception/ErrorCode.java': 'a87d19668a836cdee54521555c8b056ad0edfe54',
    'backend/src/main/java/vn/edu/utc/hotel_booking/modules/inventory/service/impl/RoomAvailabilityServiceImpl.java': '1854ee46aaff4ec3129daeb552e037115f918ec4',
    'backend/pom.xml': 'e827013e89be77e3ec85c971ab441e470a297783',
    'docker-compose.yml': '26d26eb151ffde268ac9487dd10dd7cf453fe400',
    'frontend/src/modules/booking-portal/pages/SearchPage.tsx': 'e827013e89be77e3ec85c971ab441e470a297783',
}
FROZEN_DIRS = (
    'docs/t12/r1-00/v2.0.0-draft.1',
    'backend/src/test/resources/contracts/t12/r1/v2.0.0-draft.1',
    'eval/t12/r1/v0.1.0-draft',
)
# Verify the transitive group-block read chain as well as the historical manifest.
READ_DEPENDENCIES = (
    'backend/src/main/java/vn/edu/utc/hotel_booking/modules/inventory/service/BlockedInventoryProvider.java',
    'backend/src/main/java/vn/edu/utc/hotel_booking/modules/booking/service/impl/GroupRoomBlockProviderImpl.java',
    'backend/src/main/java/vn/edu/utc/hotel_booking/modules/booking/service/impl/GroupRoomBlockServiceImpl.java',
    'backend/src/main/java/vn/edu/utc/hotel_booking/modules/booking/repository/GroupRoomBlockDailyRepository.java',
)


def require(condition, message):
    if not condition:
        raise ValueError(message)


def lf(raw):
    return raw.replace(b'\r\n', b'\n')


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def git(*args):
    return subprocess.check_output(['git', '-C', str(ROOT), *args])


def unique_pairs(pairs):
    result = {}
    for key, value in pairs:
        require(key not in result, 'Duplicate audit key: ' + key)
        result[key] = value
    return result


def load_audit():
    return json.loads((ROOT / AUDIT).read_text(encoding='utf-8'), object_pairs_hook=unique_pairs)


def patch_bytes(prior, path):
    return lf(git('diff', '--no-ext-diff', '--no-textconv', '--no-color', '--no-renames',
                  '--unified=3', prior, CODE_COMMIT, '--', path))


def verify_reconciliation(audit=None, read_working=None):
    # Import here so the artifact validator can invoke this verifier without an import cycle.
    from validate_r1_contracts import verify_source_evidence_entry
    from verify_core_reaudit import verify_existing_errors
    audit = load_audit() if audit is None else audit
    read_working = (lambda path: (ROOT / path).read_bytes()) if read_working is None else read_working
    require(audit['status'] == 'REVIEWED_SOURCE_ONLY', 'Unexpected audit status')
    require(audit['manifestPath'] == MANIFEST and audit['frozenCommit'] == FROZEN_COMMIT
            and audit['reviewedCodeCommit'] == CODE_COMMIT, 'Unreviewed audit identity')
    git('merge-base', '--is-ancestor', CODE_COMMIT, 'HEAD')
    git('merge-base', '--is-ancestor', FROZEN_COMMIT, CODE_COMMIT)

    frozen_paths = git('ls-tree', '-r', '--name-only', FROZEN_COMMIT, '--', *FROZEN_DIRS).decode().splitlines()
    actual_paths = {p.relative_to(ROOT).as_posix() for directory in FROZEN_DIRS
                    for p in (ROOT / directory).rglob('*') if p.is_file()}
    require(set(frozen_paths) == actual_paths, 'Frozen artifact path drift')
    for path in frozen_paths:
        require(lf(read_working(path)) == lf(git('show', f'{FROZEN_COMMIT}:{path}')),
                'Frozen artifact changed: ' + path)
    manifest_raw = git('show', f'{FROZEN_COMMIT}:{MANIFEST}')
    require(audit['manifestLfSha256'] == sha(lf(manifest_raw)), 'Historical manifest hash mismatch')
    manifest = json.loads(manifest_raw.decode('utf-8'))
    evidence = {entry['path']: entry for entry in manifest['files']}
    require(len(evidence) == len(manifest['files']), 'Duplicate historical evidence')
    rows = audit['files']
    entries = {entry['path']: entry for entry in rows}
    require(set(entries) == set(PRIOR_COMMITS) and len(entries) == len(rows), 'Unreviewed reconciliation scope')
    drift = {path for path, entry in evidence.items()
             if not verify_source_evidence_entry(entry, read_working(path))}
    require(drift == set(entries), 'Reconciliation must cover exactly the historical source drift')
    for path, entry in entries.items():
        prior = PRIOR_COMMITS[path]
        require(entry['priorCommit'] == prior, 'Unreviewed prior commit: ' + path)
        git('merge-base', '--is-ancestor', prior, CODE_COMMIT)
        before = git('show', f'{prior}:{path}')
        current = lf(git('show', f'{CODE_COMMIT}:{path}'))
        require(verify_source_evidence_entry(evidence[path], before)
                or verify_source_evidence_entry(evidence[path], lf(before).replace(b'\n', b'\r\n')),
                'Prior source not covered by historical hash: ' + path)
        require(entry['priorLfSha256'] == sha(lf(before)), 'Prior source hash mismatch: ' + path)
        require(entry['currentLfSha256'] == sha(current), 'Reviewed source hash mismatch: ' + path)
        require(lf(read_working(path)) == current, 'Working source differs from reviewed commit: ' + path)
        require(entry['patchLfSha256'] == sha(patch_bytes(prior, path)), 'Reviewed patch hash mismatch: ' + path)
        require(bool(entry['review']), 'Missing source review: ' + path)
        if path.endswith('/ErrorCode.java'):
            verify_existing_errors(before, current)
    for path in READ_DEPENDENCIES:
        require(lf(read_working(path)) == lf(git('show', f'{CODE_COMMIT}:{path}')),
                'Read dependency changed after review: ' + path)
    return {'reconciledSourceFiles': len(entries), 'frozenFilesVerified': len(frozen_paths),
            'readDependenciesVerified': len(READ_DEPENDENCIES), 'reviewedCodeCommit': CODE_COMMIT}


if __name__ == '__main__':
    print(json.dumps({'status': 'PASS', 'scope': 'SOURCE_PROVENANCE_ONLY', **verify_reconciliation()}, indent=2))
