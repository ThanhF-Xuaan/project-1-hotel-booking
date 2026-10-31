# Member 1 — CI baseline

## Grounding and authority

- CONFIRMED: user authorizes implementation, fixes, automated review, commit, push, PR and merge into develop; authority includes shared files. This is not evidence of a separate human peer review.
- CONFIRMED base: `62b796085618dcb8a76a072db87677660292d57a` (`origin/develop`, PR #42 merged). Clean worktree; branch `codex/m1-ci-20261031`. Owner: Member 1/project delivery, under the user's authority.
- CONFIRMED gap: repository has no GitHub Actions workflow. PR #42 verification is local evidence, not a GitHub check.
- CONFIRMED sources: `backend/pom.xml`, existing OCR and real-Redis tests, `frontend/package.json`/lockfile, `tools/t12/validate_r1_contracts.py`, source provenance auditors and existing unit tests.

## Scope and acceptance

1. Run on pull requests and pushes to develop/main, plus manual dispatch. Read-only repository token; no production credentials, paid providers, migrations or deployment. Pin actions and Redis image by immutable digest/SHA.
2. Backend: Java 21; existing default `*Test` suite with real Tesseract eng/vie and a fresh exclusive Redis service on port 16379. Exclude exactly `HotelBookingApplicationTests`, `LodgingExportPostgresTest`, `OcrMultipartServletRuntimeTest`; these require a separately configured full application/PostgreSQL/Keycloak runtime. Existing `*IT` classes are outside default Surefire discovery. Do not remove or weaken any test.
3. Require at least 1,391 selected test executions, no errors/failures/skips, internally consistent XML counts, and explicit OCR/Redis coverage. Export aggregate counts only; never upload raw Surefire XML, stdout, properties or credentials.
4. Frontend/contracts: Node 24, Python 3.12, npm clean install, strict lint, build, high/critical dependency audit, existing 49 tooling tests, contract validation with the reviewed-widget reconciliation, and current source provenance checks. Fetch full Git history because provenance checks read approved historical commits.
5. Verify workflow syntax and report guard locally; then run actual GitHub PR jobs. Fix genuine failures without lowering thresholds or gold answers. Merge only after the exact final PR head has green checks and the diff review passes. Record actual commands/counts, not predicted results.

## Allowlist, slices and rollback

- `.github/workflows/member1-ci.yml`: two bounded jobs, ephemeral Redis, explicit dependencies, minimal artifact.
- `.gitattributes`: added after actual Linux CI reproduced CRLF-frozen source/governance/evaluation hash failures. Preserve exactly the existing frozen CRLF bytes for enumerated paths; do not edit protected file contents, manifests, assertions, gold answers or minimum count. This scope adjustment uses the user's existing implementation/fix authority.
- `tools/ci/requirements.txt`, `tools/ci/verify_surefire.py`, `tools/ci/test_verify_surefire.py`: CI tooling and fail-closed evidence guard.
- `docs/member1/plans/m1-ci-baseline-2026-10-31.md`, `docs/member1/acceptance/m1-ci-baseline-2026-10-31.md`: plan, scope and observed delivery evidence.
- Canonical local handoff/plan: append a delivery receipt after verification, preserving all previous entries.
- Order: report-guard tests → workflow → local checks/review → PR → actual CI/fixes → final review → merge and verify develop tree.
- Rollback: revert this focused CI change through a reviewed PR; no database rollback, runtime or API change is required.
- Stop affected work for unexpected changes, secrets/real PII, production access, unapproved provider calls, or failing checks. Existing V03 real-data/BCA-format gates stay NOT READY; CI green does not satisfy them.
