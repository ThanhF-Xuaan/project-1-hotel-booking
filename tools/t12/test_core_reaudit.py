"""Provenance regression checks; mutations stay in memory and never edit source/gold."""
import copy
from functools import lru_cache
import json
import unittest

import verify_core_reaudit as audit

# This commit contains exactly the U14 sources AND patches recorded in U14_AUDIT.
# Core sources/dependencies are independently anchored to audit.ACCEPTED.
U14_SNAPSHOT = 'e827013e89be77e3ec85c971ab441e470a297783'


@lru_cache(maxsize=None)
def historical_source(path):
    revision = U14_SNAPSHOT if path in audit.U14_SOURCE_PATHS else audit.ACCEPTED
    return audit.git('show', f'{revision}:{path}')


@lru_cache(maxsize=None)
def historical_u14_patch(comparison, path):
    return audit.git('diff', '--no-ext-diff', '--binary', comparison, U14_SNAPSHOT, '--', path)


class CoreReauditTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.record = json.loads((audit.ROOT / audit.AUDIT).read_text(encoding="utf-8-sig"))
        cls.manifest = json.loads((audit.ROOT / audit.MANIFEST).read_text(encoding="utf-8-sig"))

    def verify_fixture(self, **kwargs):
        kwargs.setdefault('read_working', historical_source)
        kwargs.setdefault('read_u14_patch', historical_u14_patch)
        return audit.verify_audit(**kwargs)

    def test_reviewed_git_sources_and_manifest_match(self):
        result = self.verify_fixture()
        self.assertEqual((result["sources"], result["dependencies"]), (2, 7))

    def test_lf_and_crlf_sources_have_same_meaning(self):
        entry = self.record["entries"][0]
        source = audit.lf(audit.git("show", f'{audit.ACCEPTED}:{entry["path"]}'))
        for working in (source, source.replace(b"\n", b"\r\n")):
            audit.verify_source(working, source, entry["currentHashes"])

    def test_modified_source_is_rejected_even_with_changed_hashes(self):
        entry = self.record["entries"][0]
        source = audit.lf(audit.git("show", f'{audit.ACCEPTED}:{entry["path"]}'))
        modified = source + b"// unreviewed change\n"
        hashes = {"unixLfSha256": audit.sha(modified),
                  "windowsCrlfSha256": audit.sha(modified.replace(b"\n", b"\r\n"))}
        with self.assertRaisesRegex(ValueError, "accepted Git source"):
            audit.verify_source(modified, source, hashes)

    def test_previous_evidence_cannot_be_rewritten(self):
        record = copy.deepcopy(self.record)
        record["entries"][0]["previousSourceEvidence"]["historicalSha256"] = "0" * 64
        with self.assertRaisesRegex(ValueError, "Historical evidence"):
            self.verify_fixture(audit=record)

    def test_unreviewed_commit_cannot_be_selected(self):
        record = copy.deepcopy(self.record)
        record["acceptedDevelopCommit"] = audit.BASE
        with self.assertRaisesRegex(ValueError, "Unreviewed"):
            self.verify_fixture(audit=record)

    def test_unrelated_source_entry_cannot_be_changed(self):
        manifest = copy.deepcopy(self.manifest)
        entry = next(item for item in manifest["files"] if item["path"] not in
                     {audit.ERRORS, audit.AVAILABILITY, *audit.U14_SOURCE_PATHS})
        entry["sha256"] = "0" * 64
        with self.assertRaisesRegex(ValueError, "Unrelated manifest"):
            self.verify_fixture(manifest=manifest)

    def test_u14_source_entry_must_match_versioned_audit(self):
        manifest = copy.deepcopy(self.manifest)
        entry = next(item for item in manifest["files"] if item["path"] == "frontend/src/App.tsx")
        entry["sha256"] = "0" * 64
        with self.assertRaisesRegex(ValueError, "U14 manifest entry"):
            self.verify_fixture(manifest=manifest)

    def test_reconciled_manifest_must_match_current_hash(self):
        manifest = copy.deepcopy(self.manifest)
        entry = next(item for item in manifest["files"] if item["path"] == audit.ERRORS)
        entry["sha256"] = "0" * 64
        with self.assertRaisesRegex(ValueError, "Current manifest"):
            self.verify_fixture(manifest=manifest)

    def test_provider_change_is_rejected(self):
        target = next(item["path"] for item in self.record["dependencies"] if not item["unchangedFromComparison"])

        def read_working(path):
            source = historical_source(path)
            return source + b"// unreviewed provider\n" if path == target else source

        with self.assertRaisesRegex(ValueError, "Reviewed dependency changed"):
            self.verify_fixture(read_working=read_working)

    def test_dependency_scope_or_api_comparison_cannot_be_removed(self):
        for change_path in (False, True):
            with self.subTest(change_path=change_path):
                record = copy.deepcopy(self.record)
                entry = record["dependencies"][0]
                if change_path:
                    entry["path"] = audit.ERRORS
                else:
                    entry["unchangedFromComparison"] = False
                with self.assertRaisesRegex(ValueError, "Dependency evidence scope"):
                    self.verify_fixture(audit=record)

    def test_working_tree_is_not_the_historical_snapshot(self):
        with self.assertRaisesRegex(ValueError, 'U14 current source hash mismatch'):
            audit.verify_audit()

    def test_u14_source_tampering_is_rejected(self):
        target = 'backend/pom.xml'
        def read_source(path):
            raw = historical_source(path)
            return raw + b'<!-- tampered -->\n' if path == target else raw
        with self.assertRaisesRegex(ValueError, 'U14 current source hash mismatch'):
            self.verify_fixture(read_working=read_source)

    def test_u14_patch_tampering_is_rejected(self):
        def read_patch(comparison, path):
            return historical_u14_patch(comparison, path) + b'\n// tampered patch\n'
        with self.assertRaisesRegex(ValueError, 'U14 source patch changed'):
            self.verify_fixture(read_u14_patch=read_patch)

    def test_existing_error_status_cannot_change(self):
        source = audit.git("show", f"{audit.BASE}:{audit.ERRORS}")
        modified = source.replace(b"HttpStatus.INTERNAL_SERVER_ERROR", b"HttpStatus.OK", 1)
        self.assertNotEqual(source, modified)
        with self.assertRaisesRegex(ValueError, "Existing ErrorCode"):
            audit.verify_existing_errors(source, modified)


if __name__ == "__main__":
    unittest.main()
