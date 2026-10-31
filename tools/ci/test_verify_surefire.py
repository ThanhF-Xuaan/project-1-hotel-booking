import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

from verify_surefire import summarize


class SurefireEvidenceTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.directory = Path(self.temp.name)

    def write(self, content, name="TEST-example.xml"):
        (self.directory / name).write_text(content, encoding="utf-8")

    def suite(self, case="<testcase name='synthetic'/>", **counts):
        attrs = dict(tests=1, failures=0, errors=0, skipped=0)
        attrs.update(counts)
        return "<testsuite name='required' " + " ".join(
            f"{key}='{value}'" for key, value in attrs.items()
        ) + "><properties><property name='secret' value='SENSITIVE_MARKER'/></properties>" + case + "</testsuite>"

    def test_valid_reports_pass_without_exporting_properties_or_case_names(self):
        self.write(self.suite())
        report = summarize(self.directory, 1, ["required"])
        self.assertEqual(report["status"], "PASS")
        self.assertEqual(report["tests"], 1)
        self.assertNotIn("SENSITIVE_MARKER", json.dumps(report))
        self.assertNotIn("synthetic", json.dumps(report))

    def test_missing_reports_and_missing_required_suite_fail(self):
        self.assertEqual(summarize(self.directory, 1, ["required"])["status"], "FAIL")
        self.write(self.suite().replace("name='required'", "name='other'"))
        self.assertIn("Required suite missing", summarize(self.directory, 1, ["required"])["issues"])

    def test_failures_errors_and_skips_each_fail(self):
        for tag, count in (("failure", "failures"), ("error", "errors"), ("skipped", "skipped")):
            with self.subTest(tag=tag):
                self.write(self.suite(f"<testcase><{tag}>SENSITIVE_MARKER</{tag}></testcase>", **{count: 1}))
                report = summarize(self.directory, 1, ["required"])
                self.assertEqual(report["status"], "FAIL")
                self.assertNotIn("SENSITIVE_MARKER", json.dumps(report))

    def test_malformed_inconsistent_empty_and_negative_reports_fail(self):
        for content in ("<invalid", self.suite(tests=2), self.suite("", tests=0), self.suite(tests=-1), "<testsuites/>"):
            with self.subTest(content=content):
                self.write(content)
                self.assertEqual(summarize(self.directory, 1, ["required"])["status"], "FAIL")

    def test_duplicate_suite_and_low_count_fail(self):
        self.write(self.suite())
        self.assertEqual(summarize(self.directory, 2, ["required"])["status"], "FAIL")
        self.write(self.suite(), "TEST-duplicate.xml")
        self.assertEqual(summarize(self.directory, 1, ["required"])["status"], "FAIL")

    def test_cli_saves_failure_summary_and_nonzero_exit(self):
        output = self.directory / "out" / "summary.json"
        result = subprocess.run([sys.executable, str(Path(__file__).with_name("verify_surefire.py")),
                                 str(self.directory), "--minimum", "1", "--output", str(output)],
                                capture_output=True, text=True)
        self.assertEqual(result.returncode, 1)
        self.assertEqual(json.loads(output.read_text())["status"], "FAIL")


if __name__ == "__main__":
    unittest.main()
