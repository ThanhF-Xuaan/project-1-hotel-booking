# Consolidated Acceptance Test Report

- **Project:** `hotel-m1-acceptance`
- **Environment:** Local Acceptance Sandbox
- **Execution Time:** 2026-10-25 17:02:19 +07:00
- **Backend URL:** `http://localhost:18080`
- **Frontend URL:** `http://localhost:13000`
- **Result:** **PARTIAL**

## Test Execution Summary

| Category | Suite | Command | Status | Duration (s) | Details |
|---|---|---|---|---:|---|
| Backend | Maven Core Modules | `mvnw test` | **SKIPPED** | 0 | Skipped via -SkipMaven |
| Contracts | T12 Contract Schema | `python tools/t12/validate_r1_contracts.py` | **PASS** | 1.41 | 184 instances, 74 cases, 23 protected hashes matched |
| Contracts | Portability Validation | `python tools/t12/test_portability_validation.py` | **PASS** | 2.02 | 11 unit tests passed |
| Frontend | ESLint Verification | `npm run lint -- --max-warnings=0` | **PASS** | 10.45 | 0 errors, 0 warnings |
| Frontend | Vite Build & Types | `npm run build` | **PASS** | 6.78 | TypeScript 6 + Vite 8.2 bundle compiled cleanly |
| Integration | Playwright API Suite | `npm run test:api` | **PASS** | 8.94 | 11/11 tests passed across 6 roles, OCR, T04, T05, U14, RAG |
| E2E Browser | Playwright Browser Suite | `npm run test:e2e` | **PASS** | 10.76 | 4/4 tests passed (T12 conversational flow and no-write state check, session isolation, rate limiting) |

## Summary Counters

- **Total Test Suites:** 7
- **Executed:** 6
- **Passed:** 6
- **Failed:** 0
- **Skipped:** 1
- **Pass Rate (executed suites):** 100%