"""Read-only verification of the reviewed Core sources merged before PR #33."""
import hashlib
import json
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[2]
BASE = "368f6e466cacfd5f7836e9d24b0908b9e1c1a132"
ACCEPTED = "96d7815b4e5d13f5052ca0fa6a6c414c8297ea1e"
AUDIT = "docs/t12/re-audits/2026-10-26-core-integration-v1.json"
MANIFEST = "docs/t12/r1-00/v2.0.0-draft.1/baseline-source-evidence.json"
ERRORS = "backend/src/main/java/vn/edu/utc/hotel_booking/common/exception/ErrorCode.java"
AVAILABILITY = "backend/src/main/java/vn/edu/utc/hotel_booking/modules/inventory/service/impl/RoomAvailabilityServiceImpl.java"
DEPENDENCIES = {
    "backend/src/main/java/vn/edu/utc/hotel_booking/modules/inventory/dto/response/RoomAvailabilityResponse.java": True,
    "backend/src/main/java/vn/edu/utc/hotel_booking/modules/inventory/dto/request/RoomAvailabilitySearchDto.java": True,
    "backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/advisor/adapter/PublicBookingCoreAdapter.java": True,
    "backend/src/main/java/vn/edu/utc/hotel_booking/modules/inventory/service/BlockedInventoryProvider.java": False,
    "backend/src/main/java/vn/edu/utc/hotel_booking/modules/booking/service/impl/GroupRoomBlockProviderImpl.java": False,
    "backend/src/main/java/vn/edu/utc/hotel_booking/modules/booking/service/impl/GroupRoomBlockServiceImpl.java": False,
    "backend/src/main/java/vn/edu/utc/hotel_booking/modules/booking/repository/GroupRoomBlockDailyRepository.java": False,
}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def lf(value):
    return value.replace(b"\r\n", b"\n")


def sha(value):
    return hashlib.sha256(value).hexdigest()


def git(*args):
    return subprocess.check_output(["git", "-C", str(ROOT), *args])


def verify_source(current, accepted, hashes):
    canonical = lf(current)
    require(canonical == lf(accepted), "Working source differs from accepted Git source")
    require(sha(canonical) == hashes["unixLfSha256"], "LF source hash mismatch")
    require(sha(canonical.replace(b"\n", b"\r\n")) == hashes["windowsCrlfSha256"], "CRLF source hash mismatch")


def error_tuples(source):
    pattern = r'^\s*([A-Z][A-Z0-9_]*)\((\d+),\s*("(?:[^"\\]|\\.)*"),\s*(HttpStatus\.[A-Z_]+)\)'
    return {name: (code, message, status) for name, code, message, status in
            re.findall(pattern, source.decode("utf-8"), flags=re.MULTILINE)}


def verify_existing_errors(before, current):
    old, new = error_tuples(before), error_tuples(current)
    require(len(old) > 50, "Unexpected ErrorCode source shape")
    require(all(new.get(name) == values for name, values in old.items()), "Existing ErrorCode tuple changed")
    return len(new.keys() - old.keys())


def verify_audit(audit=None, manifest=None, read_working=None):
    if audit is None:
        audit = json.loads((ROOT / AUDIT).read_text(encoding="utf-8-sig"))
    if manifest is None:
        manifest = json.loads((ROOT / MANIFEST).read_text(encoding="utf-8-sig"))
    if read_working is None:
        read_working = lambda path: (ROOT / path).read_bytes()
    require(audit["comparisonCommit"] == BASE and audit["acceptedDevelopCommit"] == ACCEPTED,
            "Unreviewed comparison/accepted commit")
    require(audit["manifestPath"] == MANIFEST, "Unexpected source manifest")
    git("merge-base", "--is-ancestor", ACCEPTED, "HEAD")
    old_manifest = json.loads(git("show", f"{BASE}:{MANIFEST}").decode("utf-8-sig"))
    previous = {entry["path"]: entry for entry in old_manifest["files"]}
    current = {entry["path"]: entry for entry in manifest["files"]}
    entries = audit["entries"]
    paths = [entry["path"] for entry in entries]
    require(len(paths) == 2 and set(paths) == {ERRORS, AVAILABILITY}, "Unexpected reconciliation scope")
    require(set(current) == set(previous) and len(current) == len(manifest["files"]), "Source manifest path drift")
    require({k: v for k, v in manifest.items() if k != "files"} ==
            {k: v for k, v in old_manifest.items() if k != "files"}, "Historical manifest metadata changed")
    for path in set(previous) - set(paths):
        require(current[path] == previous[path], f"Unrelated manifest entry changed: {path}")
    new_errors = 0
    for entry in entries:
        path = entry["path"]
        before = lf(git("show", f"{BASE}:{path}"))
        accepted = lf(git("show", f"{ACCEPTED}:{path}"))
        require(entry["previousSourceEvidence"] == previous[path], "Historical evidence snapshot changed")
        require(entry["previousUnixLfSha256"] == sha(before), "Previous Git source hash mismatch")
        known_hashes = {previous[path].get(key) for key in ("sha256", "unixLfSha256", "windowsCrlfSha256")}
        require(sha(before) in known_hashes or sha(before.replace(b"\n", b"\r\n")) in known_hashes,
                "Previous source was not covered by previous manifest")
        verify_source(read_working(path), accepted, entry["currentHashes"])
        patch = lf(git("diff", "--no-ext-diff", "--no-color", "--no-renames", "--unified=3", BASE, ACCEPTED, "--", path))
        require(sha(patch) == entry["patchSha256"], "Reviewed patch changed")
        commits = git("log", "--reverse", "--format=%H", f"{BASE}..{ACCEPTED}", "--", path).decode().splitlines()
        require(commits == entry["sourceChangeCommits"] and commits, "Source commit provenance changed")
        expected = dict(previous[path], sha256=entry["currentHashes"]["windowsCrlfSha256"],
                        **entry["currentHashes"], previousReconciledSha256=previous[path]["sha256"],
                        previousReconciledFromDevelopCommit=previous[path].get("reconciledFromDevelopCommit"),
                        reconciledFromDevelopCommit=ACCEPTED, reconciledSourceCommit=commits[-1],
                        reconciledSourceAudit=AUDIT)
        if "reconciledFromPr" in expected:
            # That PR described the previous hash; do not attribute the new Core source to it.
            expected["previousReconciledFromPr"] = expected.pop("reconciledFromPr")
        require(current[path] == expected, "Current manifest reconciliation differs from reviewed evidence")
        if path == ERRORS:
            new_errors = verify_existing_errors(before, accepted)
    dependency_paths = [entry["path"] for entry in audit["dependencies"]]
    dependency_scope = {entry["path"]: entry["unchangedFromComparison"] for entry in audit["dependencies"]}
    require(len(dependency_paths) == len(set(dependency_paths)) and dependency_scope == DEPENDENCIES,
            "Dependency evidence scope changed")
    for entry in audit["dependencies"]:
        path = entry["path"]
        accepted = lf(git("show", f"{ACCEPTED}:{path}"))
        require(sha(accepted) == entry["unixLfSha256"] and lf(read_working(path)) == accepted,
                f"Reviewed dependency changed: {path}")
        if entry["unchangedFromComparison"]:
            require(lf(git("show", f"{BASE}:{path}")) == accepted, f"Existing read API changed: {path}")
    return {"status": "PASS", "scope": "SOURCE_PROVENANCE_ONLY", "sources": len(entries),
            "dependencies": len(dependency_paths), "addedErrorConstants": new_errors}


if __name__ == "__main__":
    try:
        print(json.dumps(verify_audit()))
    except (ValueError, KeyError, OSError, subprocess.CalledProcessError) as error:
        raise SystemExit(f"FAIL: {error}")
