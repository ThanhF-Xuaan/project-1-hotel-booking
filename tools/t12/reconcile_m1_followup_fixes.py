"""Pin the six follow-up fixes before composing immutable historical source audits.

This proves provenance, not runtime correctness or human peer approval.
"""
from functools import lru_cache
import hashlib
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]
BASE = '332dfbca444012a1bc7eb1ce084c8c0f751c5098'
REVIEWED = '3429a94e4768370b8f98577c8c45edc4c4f001ad'
RECEIPT = 'docs/member1/contracts/m1-followup-source-reaudit-2026-11-03.json'
RECEIPT_SHA = '3f81c722e885fd8678e7c6482ae3992432ecc90aa03d1f30d633ba7828c786c0'

PATHS = frozenset((
    'backend/src/main/java/vn/edu/utc/hotel_booking/common/exception/ErrorCode.java',
    'backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/advisor/controller/PublicRoomAdvisorController.java',
    'backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/advisor/service/AdvisorDependencyUnavailableException.java',
    'backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/advisor/service/RoomAdvisorOrchestrationService.java',
    'backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/advisor/session/InMemoryRoomAdvisorSessionStore.java',
    'backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/advisor/session/RedisRoomAdvisorSessionStore.java',
    'backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/advisor/session/RoomAdvisorSnapshots.java',
    'backend/src/main/java/vn/edu/utc/hotel_booking/modules/booking/entity/StayGuest.java',
    'backend/src/main/java/vn/edu/utc/hotel_booking/modules/booking/repository/StayGuestRepository.java',
    'backend/src/main/java/vn/edu/utc/hotel_booking/modules/finance/service/impl/PayrollSummaryServiceImpl.java',
    'backend/src/main/java/vn/edu/utc/hotel_booking/modules/operation/repository/UtilityMeterRepository.java',
    'backend/src/main/java/vn/edu/utc/hotel_booking/modules/operation/repository/UtilityReadingRepository.java',
    'backend/src/main/java/vn/edu/utc/hotel_booking/modules/operation/service/impl/StayGuestServiceImpl.java',
    'backend/src/main/java/vn/edu/utc/hotel_booking/modules/operation/service/impl/UtilityMeterServiceImpl.java',
    'backend/src/main/java/vn/edu/utc/hotel_booking/modules/operation/service/impl/UtilityReadingServiceImpl.java',
    'backend/src/main/java/vn/edu/utc/hotel_booking/modules/operation/service/support/ActiveStayDedup.java',
    'backend/src/main/java/vn/edu/utc/hotel_booking/modules/operation/service/support/ActiveStaySchemaVerifier.java',
    'backend/src/main/java/vn/edu/utc/hotel_booking/modules/operation/service/support/StayGuestTimeValidator.java',
    'backend/src/main/java/vn/edu/utc/hotel_booking/modules/operation/service/support/UtilityWriteLock.java',
    'backend/src/main/resources/db/changelogs/incremental/2026/20261103-160000-active-stay-dedup.xml',
    'backend/src/main/resources/db/sqlFile/incremental/2026/20261103-160000-active-stay-dedup.sql',
    'frontend/src/modules/booking/components/StayGuestsModal.tsx',
    'frontend/src/modules/operation/services/stayGuest.service.ts',
))


def lf(raw):
    return raw.replace(b'\r\n', b'\n')


def sha(raw):
    return hashlib.sha256(lf(raw)).hexdigest()


def require(condition, message):
    if not condition:
        raise ValueError(message)


def git(*args):
    return subprocess.check_output(['git', '-C', str(ROOT), *args])


@lru_cache(maxsize=None)
def blob(commit, path):
    return lf(git('show', f'{commit}:{path}'))


def load_receipt(raw=None):
    raw = (ROOT / RECEIPT).read_bytes() if raw is None else raw
    require(sha(raw) == RECEIPT_SHA, 'Review-fix receipt drift')
    audit = json.loads(raw)
    require(audit['baseCommit'] == BASE and audit['reviewedCommit'] == REVIEWED
            and audit['status'] == 'AI_TECHNICAL_REVIEW_ONLY', 'Review-fix identity drift')
    git('merge-base', '--is-ancestor', BASE, REVIEWED)
    git('merge-base', '--is-ancestor', REVIEWED, 'HEAD')
    paths = git('diff', '--name-only', BASE, REVIEWED, '--', 'backend/src/main', 'frontend/src').decode().splitlines()
    require(set(paths) == PATHS and len(audit['files']) == len(paths)
            and {r['path'] for r in audit['files']} == PATHS,
            'Review-fix scope drift')
    return audit


def verify_row(row, actual):
    path = row['path']
    after = blob(REVIEWED, path)
    require(sha(after) == row['afterLfSha256'] and lf(actual) == after, 'Review-fix source drift: ' + path)
    exists = subprocess.run(['git', '-C', str(ROOT), 'cat-file', '-e', f'{BASE}:{path}'],
                            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL).returncode == 0
    before = blob(BASE, path) if exists else None
    require(row['beforeLfSha256'] == (sha(before) if exists else None), 'Review-fix baseline drift: ' + path)
    delta = git('diff', '--no-ext-diff', '--no-textconv', '--no-color', '--no-renames', '--unified=3', BASE, REVIEWED, '--', path)
    require(sha(delta) == row['patchLfSha256'] and bool(row['review']), 'Review-fix patch drift: ' + path)
    return before


def reconcile(path, actual):
    if path not in PATHS:
        return actual
    audit = load_receipt()
    row = next((r for r in audit['files'] if r['path'] == path), None)
    if row is None:
        return actual
    before = verify_row(row, actual)
    return actual if before is None else before


def verify_current():
    audit = load_receipt()
    for row in audit['files']:
        verify_row(row, (ROOT / row['path']).read_bytes())
    return {'status': 'PASS', 'scope': 'SOURCE_PROVENANCE_ONLY', 'files': len(audit['files']),
            'reviewedCommit': REVIEWED, 'humanPeerApproval': False}


if __name__ == '__main__':
    print(json.dumps(verify_current(), indent=2))
