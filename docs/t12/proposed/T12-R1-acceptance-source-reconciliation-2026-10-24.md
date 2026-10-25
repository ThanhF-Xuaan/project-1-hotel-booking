# T12 R1 source evidence reconciliation — 2026-10-24

**Status:** Reconciled to merged `develop` source; no separate owner sign-off record was found in the repository.

The frozen source baseline remains `0e6d379eba8ab6ac7cf3573728d92d97f3e659e6`. This additive reconciliation records later changes already merged into `develop`; it does not change the frozen OpenAPI, request/response schemas, evaluation cases, expected assertions, or thresholds.

| File | Historical hash retained | Previous recorded source | Current source | Merged source evidence | Owner |
|---|---|---|---|---|---|
| `backend/src/main/java/vn/edu/utc/hotel_booking/common/exception/ErrorCode.java` | `407fb57045db44baadd3665d8b25ee81cb8972439a2480af4b1f7a34a8708a9f` | `b6c113e6dca4514cf335132168d9889e5dc187e421858ff624f88a807ed41f98` | `e08f2eadeb1b82295a2edd758b97e893774f8b7c5778e077672f7cafa5984b88` | Member 2 commit `023933e38092ce27e42635148276d4bacffd5326`, merged by PR #24 at `a6b2088f55d2326a51e895ff2cfffbb4fe70831e` | Member 2 |
| `frontend/src/modules/booking-portal/pages/SearchPage.tsx` | `83d5b3ece20ed6a0175be4893ec2f240d90dc038a0437b8686e2007de46bd0a0` | No later source recorded | `6f01ed80cffefe014a1561c2aa379153fed6dcfbc2c9f75fe8427c173c2587df` | Member 4 commit `98527869021646dc3f198946158457a58a5ec0d2`, merged by PR #23 at `16129e444352e7662a3c15fbecbd6dfa48e2e781` | Member 4 |

The validator compares source bytes to the current hashes while the two historical hashes remain available for audit. A passing artifact validator proves only that the recorded files and artifacts match the checked-out source; it is not a runtime, provider, browser, or release approval.

## Post-review working-tree reconciliation — 2026-10-25

The `SearchPage.tsx` source hash was updated additively after the review fix that preserves confirmed adult, child, infant, and extra-bed counts when opening room details. The previous reconciled hash remains recorded in `previousReconciledSha256`; API schemas and evaluation answers/thresholds were not changed. The frontend build and browser E2E passed in the local acceptance run; the remaining suites are recorded in `docs/member1/acceptance/acceptance-test-report.md`.

## Verification status

- `python -B tools/t12/validate_r1_contracts.py`: PASS; `ARTIFACT_VALIDATION_ONLY`, `engineEvaluation=NOT_RUN`.
- T12 R1 evaluation and security tests: PASS as synthetic local tests; no live Gemini call.
