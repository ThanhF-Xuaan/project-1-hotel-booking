"""Export deterministic, oracle-free Member 1 synthetic demo data. Never load into production."""
from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCES = {
    "backend/src/test/resources/ai/u22/occupancy-history-420-days.json": "8c1f3e81019238948be47a61c49ae4821b92752381a7d99d70216e8437651c20",
    "backend/src/test/resources/ai/u22/utility-readings-42-days.csv": "48078f6afa5d34ac6c3bf0292813486d33bf10cdc15397085ddd8223b5bb9638",
    "backend/src/test/resources/ai/group-assignment/preview-cases.json": "2dbd7da729831b0f21cc53c21412bb744fd8e620aaa050c6fe9a2b2a89ddc476",
}


def encoded(value) -> bytes:
    return (json.dumps(value, ensure_ascii=False, indent=2) + "\n").encode("utf-8")


def build_files(source_root: Path = ROOT) -> dict[str, bytes]:
    raw = {}
    for path, expected in SOURCES.items():
        content = (source_root / path).read_bytes()
        if hashlib.sha256(content).hexdigest() != expected:
            raise ValueError("Synthetic source hash mismatch; do not replace fixtures with real data")
        raw[path] = content
    occupancy, utility, group = list(raw.values())
    occupancy = json.loads(occupancy)
    group = json.loads(group)
    if occupancy["dataMode"] != "SYNTHETIC" or group["dataMode"] != "SYNTHETIC":
        raise ValueError("Only synthetic fixtures may be exported")
    files = {}
    for hotel in occupancy["hotels"]:
        files[f"occupancy/hotel-{hotel['hotelId']}.json"] = encoded({
            "dataMode": "SYNTHETIC", "schemaVersion": occupancy["schemaVersion"],
            "referenceInstant": occupancy["referenceInstant"],
            "fromInclusive": occupancy["fromInclusive"], "toInclusive": occupancy["toInclusive"], **hotel,
        })
    series = {}
    for row in csv.DictReader(io.StringIO(utility.decode("utf-8"))):
        series.setdefault(row["seriesId"], []).append({
            "hotelId": int(row["hotelId"]), "meterId": int(row["meterId"]), "unit": row["unit"],
            "counterEpoch": row["counterEpoch"], "date": row["date"],
            "cumulativeValue": row["cumulativeValue"], "marker": row["marker"],
        })
    for name, samples in sorted(series.items()):
        files[f"utility/{name}.json"] = encoded({"dataMode": "SYNTHETIC", "seriesId": name,
            "schemaVersion": "u22-utility-anomaly-local-v1", "samples": samples})
    for case in group["cases"]:
        files[f"group/{case['id']}.json"] = encoded({"dataMode": "SYNTHETIC",
            "schemaVersion": group["schemaVersion"], "asOf": group["asOf"], "id": case["id"],
            "request": case["request"], "snapshot": case["snapshot"]})
    files["lodging-review.json"] = encoded({"dataMode": "SYNTHETIC", "officialBcaFormat": "NOT_PROVIDED",
        "notForOfficialSubmission": True, "requiresHumanVerification": True, "drafts": [{
            "draftKey": "MOCK-LODGE-001", "fullName": "SYNTHETIC GUEST 001", "dateOfBirth": "1995-01-01",
            "gender": "MALE", "nationality": "VN", "documentType": "PASSPORT",
            "documentNumber": "A00000001", "permanentAddress": "MOCK_ADDRESS_NO_REAL_LOCATION",
            "humanVerified": False,
        }]})
    files["README.txt"] = (
        "SYNTHETIC DEMO DATA ONLY. No real hotels, guests or bookings.\n"
        "Occupancy: 2 hotels, 420 daily rows each; missing coverage and capacity changes remain explicit.\n"
        "Utility: one independent scenario per file; preserve unit/epoch/marker and decimal strings.\n"
        "Group: one scenario per file; fixed asOf/TTL is for a fixed-clock demo, not current availability.\n"
        "Use the referenceInstant/asOf clock from each source when executing demo services.\n"
        "Lodging: human-review draft only; official BCA format has NOT been supplied.\n"
        "Gold expected labels are excluded. Original frozen evaluation data remains unchanged.\n"
        "Real data requires separate validated adapters, trusted scope and completeness evidence.\n"
    ).encode("utf-8")
    manifest = {"schemaVersion": "m1-v03-mock-pack-v1", "dataMode": "SYNTHETIC",
        "productionReady": False, "officialBcaFormat": "NOT_PROVIDED", "sourceSha256": SOURCES,
        "counts": {"occupancyHotels": len(occupancy["hotels"]),
                   "occupancyRows": sum(len(h["samples"]) for h in occupancy["hotels"]),
                   "utilityScenarios": len(series), "utilityReadings": sum(map(len, series.values())),
                   "groupScenarios": len(group["cases"]), "lodgingDrafts": 1},
        "files": {name: hashlib.sha256(data).hexdigest() for name, data in sorted(files.items())}}
    files["manifest.json"] = encoded(manifest)
    return files


def generate(target: Path, source_root: Path = ROOT) -> dict:
    target = target.resolve()
    allowed_runtime = ROOT / "tools/acceptance/.runtime/mock-data"
    if target.is_relative_to(ROOT) and not target.is_relative_to(allowed_runtime):
        raise ValueError("Output must be outside Git or under .runtime/mock-data")
    if target.exists():
        raise FileExistsError("Refusing to overwrite existing mock data")
    files = build_files(source_root)  # Validate all sources before creating output.
    target.mkdir(parents=True)
    for name, data in files.items():
        path = target / name
        path.parent.mkdir(parents=True, exist_ok=True)
        with path.open("xb") as stream:
            stream.write(data)
    return json.loads(files["manifest.json"])


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output-directory", type=Path, required=True)
    args = parser.parse_args()
    try:
        manifest = generate(args.output_directory)
        print(json.dumps({"status": "PASS", "dataMode": "SYNTHETIC", "counts": manifest["counts"]}))
    except (ValueError, FileExistsError, OSError) as error:
        parser.exit(1, f"Mock export failed: {type(error).__name__}\n")
