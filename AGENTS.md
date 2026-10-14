# Repository Guidelines — Hotel Booking

> **Status:** PROPOSED shared guidance on branch `docs/m1-t12-governance-sync`. Member 2 approval is required before merge. This file does not itself approve shared API, schema, security, or ownership changes.

These instructions apply to all contributors and coding agents. Task-specific rules supplement but do not silently override team-approved security, API, or ownership contracts. Surface conflicts for human review.

## Project structure and evidence

- Backend feature code: `backend/src/main/java/vn/edu/utc/hotel_booking/modules/`; shared infrastructure: `backend/src/main/java/vn/edu/utc/hotel_booking/common/`.
- Backend tests: `backend/src/test/java/`; Liquibase resources: `backend/src/main/resources/db/`.
- Frontend features: `frontend/src/modules/`; shared UI/auth/API infrastructure: `frontend/src/core/`.
- Agent rules and skills: `.agents/rules/`, `.agents/skills/`; specifications and evaluation material: `docs/`, `document/`, `eval/`.
- Verify the target Git SHA, real source, OpenAPI/runtime envelopes, installed dependencies, tests, and call sites before asserting that a contract exists. A proposal, old branch, or sample is not proof of `develop` behavior.
- Label consequential facts `CONFIRMED`, `PROPOSED`, `UNKNOWN`, or `BLOCKED`.

## Authorization and ownership

- Match the workflow to the request and risk: targeted inspection for small read-only work; contract/spec, owner approval, small slices, verification, and rollback for new or high-risk changes.
- **Audit/review:** inspect and report; do not edit.
- **Plan/design:** produce architecture, contracts, dependencies, tests, approvals, and rollback; do not implement or mutate Git/DB state.
- **Implementation:** change only authorized files in small, verifiable slices.
- Before editing, establish repository identity, branch/HEAD, intended base SHA, worktree status, responsible owner, and approved contracts.
- Treat dirty and untracked files as protected. Never silently reset, clean, stash, rebase, overwrite, force-push, or merge another contributor's work.
- Do not change another member's Booking, Pricing, Inventory, Payment, IAM, shared database, or frontend business logic without that owner's approval. Submit a contract request and use synthetic test fixtures while a dependency is pending.
- Member 1 tasks `T04`, `T05`, `T12`, `U13`, `U14`, and `U22` must also follow [`AGENTS.member1.md`](AGENTS.member1.md) and [the Member 1 architecture rule](.agents/rules/07-member-1-architecture-pipeline.md).

## Engineering, security, and data

- Follow existing architecture: validated DTOs, thin controllers, cohesive services, dedicated persistence, constructor injection, and established mapping conventions.
- Keep authoritative prices, availability, booking state, tenant scope, and authorization in their owning backend modules. Do not reproduce Pricing or Booking calculations in an AI integration.
- Model output, user input, retrieved documents, uploads, and client-supplied scope are untrusted. Validate in application code. Never accept model-generated SQL, arbitrary URLs, role claims, or privileged tool operations.
- Do not log or commit credentials, raw identity documents, real payroll/customer data, tokens, API keys, or sensitive prompt contents. Use synthetic fixtures and approved retention rules.
- Shared API/schema/security changes require owner review. Liquibase migrations must be incremental, reviewed, and run only against an explicitly approved environment.

### T12 invariant

The AI Room Recommendation Assistant may discover and recommend verified room types and provide view-details navigation. It must never hold/release inventory, create/update/cancel a booking, start a payment/refund, or mutate guest/business records, even after user confirmation. Dynamic price and availability come only from approved read-only Core contracts. RAG contains scoped, approved static knowledge and cannot override live Core facts. Detailed T12 behavior belongs in Member 1 guidance, not in global rules.

## Verification and delivery

- Inspect repository scripts before running them. From `backend/`, use `./mvnw test` or `./mvnw clean verify` when relevant; on Windows use `.\mvnw.cmd`. From `frontend/`, use `npm run lint` and `npm run build` when relevant.
- Docker startup and Liquibase updates can mutate services or databases. Verify the environment, target, backup, and owner approval before execution; never run them for a documentation audit.
- Report commands actually run as `PASS`, `FAIL`, or `NOT RUN` with evidence or reason. Do not weaken tests, gold answers, or thresholds to obtain a passing result.
- Branch creation, commit, push, PR creation, merge, migration execution, and production access require the authorization appropriate to that action. Member 2 coordinates merges to shared branches; AI review does not replace human peer review.
- Handoff must include changed files, behavior/contracts, actual test evidence, unverified areas, privacy/security implications, approvals needed, base SHA, and Git mutation status.
