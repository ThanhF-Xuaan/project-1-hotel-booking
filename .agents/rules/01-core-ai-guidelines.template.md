---
trigger: always_on
description: "Project-wide safeguards for evidence-based changes, contract integrity, ownership, security, Git collaboration, and truthful verification."
---

# Core AI Engineering Guidelines — Hotel Booking

> **Status:** PROPOSED shared rule; Member 2 approval is required before merge. This compact always-on rule supplements `AGENTS.md`. Domain rules and skills provide conditional detail. Surface conflicts instead of silently overriding human-owned contracts.

## 1. Ground decisions in evidence

- Inspect real files, symbols, DTOs, routes, auth mappings, database fields, tests, dependency versions, and call sites on the target SHA. Old branches, plans, examples, and generated OpenAPI proposals are not proof of current behavior.
- Mark consequential facts `CONFIRMED`, `PROPOSED`, `UNKNOWN`, or `BLOCKED` and cite evidence.
- Prices, availability, bookings, permissions, tenant scope, regulated formats, and shared contracts come only from their approved owners. Never invent endpoints, fields, roles, legal claims, or production data.

## 2. Match workflow to authorization and risk

- Audit/review is read-only. Plan/design produces contracts, dependencies, tests, owners, and rollback without implementation. Implementation permits only scoped edits.
- A small local change needs targeted inspection and verification. A new/high-risk feature needs contract/spec, owner approval, small slices, verification, and handoff.
- Preserve existing behavior and unrelated comments/files. Do not perform speculative refactors, dependency upgrades, migrations, or cross-owner changes.

## 3. Protect Git and environments

- Before edits, verify repository, remotes, branch, HEAD/base SHA, worktrees, and status. Local `develop` may not equal `origin/develop`.
- Treat dirty/untracked files as protected. Never silently reset, clean, stash, overwrite, rebase, force-push, or merge them.
- Commit, push, PR creation, merge, destructive DB/service commands, paid model calls, and production access require explicit authorization for that action.
- Never expose or commit secrets, credentials, identity/customer/payment/payroll data, or raw sensitive prompt content. Use synthetic fixtures.

## 4. Build contract-first, not template-first

Before changing behavior, establish validated inputs/outputs/errors; source of truth and side effects; ownership/RBAC/tenant/privacy; dependencies and degradation; acceptance, compatibility, and rollback.

Follow actual repository conventions. Add DTOs, mappers, services, repositories, UI, or schema only when needed. Do not require a universal entity-first or vector-first sequence. Shared API/schema/security changes require owner review. Never reproduce Pricing/Booking logic in an AI adapter.

## 5. Enforce trust boundaries

- Validate and bound user input, uploads, model output, retrieved text, batch sizes, tool/network calls, concurrency, retries, timeouts, and cost.
- Treat LLM output and retrieved documents as untrusted data. Never execute model-generated SQL, arbitrary URLs, role claims, or privileged operations.
- **T12:** recommend verified room types and offer approved view-details navigation only. Never hold/release inventory, create/update/cancel bookings, or start payments/refunds. Dynamic price/availability comes from approved read-only Core contracts; RAG cannot override it.
- Verify tenant/session ownership and actual Spring Security mappings. Never trust a client-supplied `hotelId`, role, or `sessionId` as authorization.

## 6. Verify and report truthfully

- Use existing repository scripts. Run targeted checks and relevant regressions; never claim an unexecuted build, test, migration, API, or AI eval passed.
- Report every check `PASS`, `FAIL`, or `NOT RUN` with command and evidence/reason. Do not weaken tests, safety assertions, gold answers, or thresholds.
- Review the final diff for scope leaks, unexpected files, secrets/PII, compatibility, and owner approvals. Handoff exact Git state, changed behavior/contracts, evidence, risks, blockers, and unverified items.
