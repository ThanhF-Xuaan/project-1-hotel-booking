"""Validate four fixed upstream deltas before composing historical source audits."""
from functools import lru_cache
import hashlib
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]
BASE = 'a70b833f8d138621e539fdbcade595eb56165a4c'
REVIEWED = '7d8ee514b1cbc4e6a4aa4889817620a57e80b513'
RECEIPT = 'docs/member1/contracts/deploy-source-reaudit-2026-11-01.json'
RECEIPT_SHA = '632815054005b835d83b365e41170420c26a5c7822f28fa813adbf35da669a3c'
PATHS = frozenset((
    'backend/src/main/java/vn/edu/utc/hotel_booking/common/config/SecurityConfig.java',
    'backend/pom.xml', 'docker-compose.yml',
    'frontend/src/modules/booking-portal/pages/SearchPage.tsx',
))


def lf(raw):
    return raw.replace(b'\r\n', b'\n')


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def require(condition, message):
    if not condition:
        raise ValueError(message)


def git(*args):
    return subprocess.check_output(['git', '-C', str(ROOT), *args])


@lru_cache(maxsize=None)
def blob(commit, path):
    # Cache immutable Git objects only; never cache receipt or working source bytes.
    return lf(git('show', f'{commit}:{path}'))


@lru_cache(maxsize=None)
def delta(path):
    return lf(git('diff', '--no-ext-diff', '--no-textconv', '--no-color', '--no-renames',
                  '--unified=3', BASE, REVIEWED, '--', path))


def load_receipt(raw=None):
    raw = (ROOT / RECEIPT).read_bytes() if raw is None else raw
    require(sha(lf(raw)) == RECEIPT_SHA, 'Unreviewed deployment source receipt')
    audit = json.loads(raw)
    require(audit['status'] == 'REVIEWED_SOURCE_ONLY' and audit['baseCommit'] == BASE
            and audit['reviewedCommit'] == REVIEWED, 'Unreviewed deployment source identity')
    require(len(audit['files']) == len(PATHS) and {r['path'] for r in audit['files']} == PATHS,
            'Unexpected deployment source scope')
    return audit


def reconcile(path, actual):
    if path not in PATHS:
        return actual
    audit = load_receipt()
    git('merge-base', '--is-ancestor', BASE, REVIEWED)
    git('merge-base', '--is-ancestor', REVIEWED, 'HEAD')
    row = next(r for r in audit['files'] if r['path'] == path)
    before, after = blob(BASE, path), blob(REVIEWED, path)
    require(sha(before) == row['beforeLfSha256'] and sha(after) == row['afterLfSha256'],
            'Deployment source Git hash mismatch: ' + path)
    require(sha(delta(path)) == row['patchLfSha256'] and bool(row['review']),
            'Deployment source patch mismatch: ' + path)
    require(lf(actual) == after, 'Source differs from reviewed deployment commit: ' + path)
    return before
