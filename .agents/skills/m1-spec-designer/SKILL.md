---
name: m1-spec-designer
description: Design evidence-based technical specs, ADRs, contract requests, ownership boundaries, and acceptance criteria for Member 1 tasks T04, T05, T12, U13, U14, and U22. Use before a new feature or material redesign; do not use for minor edits with an already approved contract.
---

# M1 Spec Designer

Produce a reviewable specification without treating proposals as implemented facts or expanding Member 1's authority.

## Required inputs

- User-approved objective and task code.
- Exact repository, target SHA/status, relevant source/tests/OpenAPI, and current owner map.
- Existing human decisions, evaluation material, privacy/security constraints, and known dependencies.

If these inputs are unavailable, label them `UNKNOWN` or `BLOCKED`; do not invent them.

## Process

1. **Audit before design.** Record `CONFIRMED`, `PROPOSED`, `UNKNOWN`, and `BLOCKED` facts with file/symbol/contract evidence. An old branch or plan is reference material only.
2. **Define ownership.** Separate Member 1 behavior from Member 2 Core/schema/security, Member 3 Payment/POS, and Member 4 UI/navigation. Convert missing cross-owner behavior into a contract request.
3. **Specify the contract.** Define validated inputs/outputs, identifiers, date/time/money units, response envelope, statuses/errors, authorization/tenant scope, side effects, compatibility, degradation, and rollback.
4. **Specify data safety.** Identify PII, secrets, retention/consent decisions, trusted scopes, synthetic fixtures, and external-provider boundaries.
5. **Define observable acceptance.** Include deterministic tests, relevant regressions, security checks, AI evaluation inputs/approval status, performance/cost limits, and `PASS`/`FAIL`/`NOT RUN` reporting.
6. **Record decisions.** ADRs distinguish accepted product decisions from owner approvals still required. Do not assign an approval date or approver without evidence.

## T12 specification rules

- Product: read-only AI Room Recommendation Assistant.
- Release 1: Gemini proposes typed criteria; Java validates/merges criteria, discovers candidates, filters, calls authoritative read-only Availability/Pricing, ranks deterministically, and returns 3–5 verified room-type cards when available—or fewer.
- MVP: one room per option; keep `roomsCount`, but clarify/redirect when greater than one. Never multiply a quote.
- Release 2: scoped RAG for approved static/public knowledge; recommendation remains usable without RAG.
- Never propose hold/release, booking mutation, payment/refund, guest mutation, generic HTTP/SQL tools, or write-capable callbacks.
- Do not invent public room-type endpoints, detail routes, fee/currency semantics, session ownership, pgvector installation, Gemini model IDs, embedding dimensions, or passed metrics. Request them from their owners.

## Other Member 1 tracks

- `T04`/`T05`: derive schemas and file formats from approved repository/domain contracts; do not invent regulated BCA formats.
- `U13`: human verification, consent/provider and retention decisions are required before identity persistence.
- `U14`: suggestions only; do not mutate Pricing or profile on unapproved sensitive attributes.
- `U22`: time-based backtesting and simple baselines; label synthetic data.

## Deliverables

- Spec or ADR with decision status and evidence.
- Current-versus-proposed API/DTO and ownership matrix.
- Contract requests with owner, examples, edge cases, acceptance and compatibility notes.
- Test/evaluation matrix, risks, rollout/rollback, and explicit stop conditions.

## Verification and stop conditions

Check every path/symbol cited, every proposed side effect, and every owner dependency. The spec passes only when it contains no unlabelled invented contract and keeps T12 zero-business-write.

Stop the affected section when authority, tenant/session scope, money/date semantics, regulated format, privacy/provider approval, or shared schema/security ownership cannot be resolved. A spec does not authorize implementation, branch creation, migration, commit, push, or PR.
