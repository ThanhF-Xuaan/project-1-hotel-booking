import hashlib
import json
from pathlib import Path
import tempfile
import unittest

from generate_m1_mock_data import ROOT, SOURCES, build_files, generate


class MockDataExportTest(unittest.TestCase):
    def test_deterministic_pack_has_expected_sizes_and_verified_file_hashes(self):
        files = build_files()
        self.assertEqual(files, build_files())
        manifest = json.loads(files["manifest.json"])
        self.assertEqual(manifest["counts"], {"occupancyHotels": 2, "occupancyRows": 840,
            "utilityScenarios": 12, "utilityReadings": 516, "groupScenarios": 7, "lodgingDrafts": 1})
        for name, digest in manifest["files"].items():
            self.assertEqual(hashlib.sha256(files[name]).hexdigest(), digest)

    def test_no_oracle_or_authoritative_mode_in_export(self):
        def inspect(value):
            if isinstance(value, dict):
                self.assertNotIn("expected", value)
                if "dataMode" in value:
                    self.assertEqual(value["dataMode"], "SYNTHETIC")
                for child in value.values(): inspect(child)
            elif isinstance(value, list):
                for child in value: inspect(child)
        for name, content in build_files().items():
            if name.endswith(".json"): inspect(json.loads(content))

    def test_lodging_is_unverified_nonofficial_draft(self):
        draft = json.loads(build_files()["lodging-review.json"])
        self.assertTrue(draft["requiresHumanVerification"])
        self.assertTrue(draft["notForOfficialSubmission"])
        self.assertEqual(draft["officialBcaFormat"], "NOT_PROVIDED")
        self.assertFalse(draft["drafts"][0]["humanVerified"])

    def test_existing_output_is_preserved(self):
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / "pack"
            generate(target)
            original = (target / "manifest.json").read_bytes()
            with self.assertRaises(FileExistsError): generate(target)
            self.assertEqual((target / "manifest.json").read_bytes(), original)

    def test_repository_output_is_rejected_before_writing(self):
        with self.assertRaises(ValueError): generate(ROOT / "must-not-create-mock-output")
        self.assertFalse((ROOT / "must-not-create-mock-output").exists())

    def test_tampered_source_rejected_without_output(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for name in SOURCES:
                path = root / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes((ROOT / name).read_bytes())
            first = root / next(iter(SOURCES))
            first.write_bytes(first.read_bytes() + b"\n")
            target = root / "pack"
            with self.assertRaises(ValueError): generate(target, root)
            self.assertFalse(target.exists())


if __name__ == "__main__":
    unittest.main()
