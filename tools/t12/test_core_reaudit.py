"""Provenance regression checks; mutations stay in memory and never edit source/gold."""
import copy
import json
import unittest

import verify_core_reaudit as audit


class CoreReauditTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.record = json.loads((audit.ROOT / audit.AUDIT).read_text(encoding="utf-8-sig"))
        cls.manifest = json.loads((audit.ROOT / audit.MANIFEST).read_text(encoding="utf-8-sig"))

    def test_reviewed_git_sources_and_manifest_match(self):
        result = audit.verify_audit()
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
            audit.verify_audit(audit=record)

    def test_unreviewed_commit_cannot_be_selected(self):
        record = copy.deepcopy(self.record)
        record["acceptedDevelopCommit"] = audit.BASE
        with self.assertRaisesRegex(ValueError, "Unreviewed"):
            audit.verify_audit(audit=record)

    def test_unrelated_source_entry_cannot_be_changed(self):
        manifest = copy.deepcopy(self.manifest)
        entry = next(item for item in manifest["files"] if item["path"] not in {audit.ERRORS, audit.AVAILABILITY})
        entry["sha256"] = "0" * 64
        with self.assertRaisesRegex(ValueError, "Unrelated manifest"):
            audit.verify_audit(manifest=manifest)

    def test_reconciled_manifest_must_match_current_hash(self):
        manifest = copy.deepcopy(self.manifest)
        entry = next(item for item in manifest["files"] if item["path"] == audit.ERRORS)
        entry["sha256"] = "0" * 64
        with self.assertRaisesRegex(ValueError, "Current manifest"):
            audit.verify_audit(manifest=manifest)

    def test_provider_change_is_rejected(self):
        target = next(item["path"] for item in self.record["dependencies"] if not item["unchangedFromComparison"])

        def read_working(path):
            source = (audit.ROOT / path).read_bytes()
            return source + b"// unreviewed provider\n" if path == target else source

        with self.assertRaisesRegex(ValueError, "Reviewed dependency changed"):
            audit.verify_audit(read_working=read_working)

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
                    audit.verify_audit(audit=record)

    def test_existing_error_status_cannot_change(self):
        source = audit.git("show", f"{audit.BASE}:{audit.ERRORS}")
        modified = source.replace(b"HttpStatus.INTERNAL_SERVER_ERROR", b"HttpStatus.OK", 1)
        self.assertNotEqual(source, modified)
        with self.assertRaisesRegex(ValueError, "Existing ErrorCode"):
            audit.verify_existing_errors(source, modified)


if __name__ == "__main__":
    unittest.main()
