# Consolidated Acceptance Test Report

- **Project:** `hotel-m1-acceptance`
- **Environment:** Local Acceptance Sandbox
- **Execution Time:** 2026-10-25 13:29:16 +07:00
- **Backend URL:** `http://localhost:18080`
- **Frontend URL:** `http://localhost:13000`
- **Result:** **PASS**

## Test Execution Summary

| Category | Suite | Command | Status | Duration (s) | Details |
|---|---|---|---|---:|---|
| Backend | Maven Core Modules | `mvnw test (T04/T05/T12/U13/U14/RAG)` | **PASS** | 18.14 | 338 tests executed, 0 failures, 4 skipped |
| Contracts | T12 Contract Schema | `python tools/t12/validate_r1_contracts.py` | **PASS** | 1.34 | 184 instances, 74 cases, 23 protected hashes matched |
| Contracts | Portability Validation | `python tools/t12/test_portability_validation.py` | **PASS** | 1.92 | 11 unit tests passed |
| Frontend | ESLint Verification | `npm run lint -- --max-warnings=0` | **PASS** | 7.93 | 0 errors, 0 warnings |
| Frontend | Vite Build & Types | `npm run build` | **PASS** | 6.42 | TypeScript 6 + Vite 8.2 bundle compiled cleanly |
| Integration | Playwright API Suite | `npm run test:api` | **PASS** | 7.67 | 11/11 tests passed across 6 roles, OCR, T04, T05, U14, RAG |
| E2E Browser | Playwright Browser Suite | `npm run test:e2e` | **PASS** | 8.59 | 4/4 tests passed (T12 conversational flow and no-write state check, session isolation, rate limiting) |

## Summary Counters

- **Total Test Suites:** 7
- **Executed:** 7
- **Passed:** 7
- **Failed:** 0
- **Skipped:** 0
- **Pass Rate (executed suites):** 100%