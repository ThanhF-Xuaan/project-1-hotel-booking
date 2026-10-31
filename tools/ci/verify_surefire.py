"""Fail closed on incomplete CI reports; export counts, never test output/properties."""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import xml.etree.ElementTree as ET


def summarize(directory: Path, minimum: int, required: list[str]) -> dict:
    totals = dict(tests=0, failures=0, errors=0, skipped=0)
    suites: set[str] = set()
    report = {"status": "FAIL", "reports": 0, **totals, "issues": []}
    issues = report["issues"]
    files = sorted(directory.glob("TEST-*.xml"))
    if not files:
        issues.append("No Surefire reports found")
    for path in files:
        try:
            root = ET.parse(path).getroot()
            if root.tag != "testsuite":
                raise ValueError("Expected one testsuite per report")
            name = root.attrib["name"]
            if name in suites:
                raise ValueError("Duplicate suite")
            suites.add(name)
            declared = {key: int(root.attrib[key]) for key in totals}
            if any(value < 0 for value in declared.values()):
                raise ValueError("Negative counts")
            cases = root.findall("testcase")
            actual = {
                "tests": len(cases),
                "failures": sum(case.find("failure") is not None for case in cases),
                "errors": sum(case.find("error") is not None for case in cases),
                "skipped": sum(case.find("skipped") is not None for case in cases),
            }
            if declared != actual or not cases:
                raise ValueError("Empty suite or inconsistent counts")
            for key, value in actual.items():
                totals[key] += value
        except (ET.ParseError, KeyError, ValueError, OSError):
            # Do not echo XML, paths, suite names or exception text from test reports.
            issues.append("Malformed, duplicate, empty or inconsistent Surefire report")
    if totals["tests"] < minimum:
        issues.append("Test count below selected-scope minimum")
    if any(totals[key] for key in ("failures", "errors", "skipped")):
        issues.append("Selected tests contain failures, errors or skips")
    if any(name not in suites for name in required):
        issues.append("Required suite missing")
    report.update(totals, reports=len(files), status="FAIL" if issues else "PASS")
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("reports", type=Path)
    parser.add_argument("--minimum", type=int, required=True)
    parser.add_argument("--require-suite", action="append", default=[])
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.minimum < 1:
        parser.error("minimum must be positive")
    report = summarize(args.reports, args.minimum, args.require_suite)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report))
    return 0 if report["status"] == "PASS" else 1


if __name__ == "__main__":
    raise SystemExit(main())
