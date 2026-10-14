---
name: m1-execution-planner
description: Convert an approved Member 1 specification into safe, dependency-aware implementation slices with file allowlists, verification, rollback, and Git/worktree safeguards. Use after spec approval and before coding; do not use to imply implementation authorization.
---

# M1 Execution Planner

Create an executable plan that protects other contributors and adapts sequencing to real dependencies rather than enforcing a universal cascade.

## Required inputs

- Approved specification/ADR and acceptance criteria.
- Exact repository, current branch/HEAD/status, verified intended base SHA, and worktree inventory.
- Owner-approved contracts or clearly named synthetic test fixtures for pending dependencies.
- Existing repository scripts and relevant tests.

## Process

1. **Preflight:** verify repository identity, remotes, SHA/status, protected changes, owners, and contract state. Do not change Git during a plan-only request.
2. **Choose isolation:** for authorized implementation, propose a unique branch/worktree from the verified base. Never reset, clean, stash, overwrite, or import the old T12 branch wholesale.
3. **Map dependencies:** distinguish tasks Member 1 can complete with fixtures from Member 2/4 approval gates. Mark each dependency `READY`, `PROPOSED`, `UNKNOWN`, or `BLOCKED`.
4. **Create small vertical slices:** each slice lists outcome, precise file allowlist, owner, inputs, implementation steps, verification commands, compatibility, risk, rollback, and stop condition.
5. **Sequence by risk:** contract/fixture work precedes integration; compile/test early. A schema, security, shared dependency, paid provider, or production call is a separate approval gate.
6. **Close with delivery evidence:** include final diff/status/whitelist checks and a human handoff. Commit, push, PR, merge, and migration commands remain unexecuted unless separately authorized.

## T12 release planning

Plan Release 1 independently from Release 2:

- **R1 slices:** approved chat/card contracts and fixtures; typed extraction/criteria; Java candidate discovery and hard filters; read-only Availability/Pricing adapters; deterministic ranking/cards; safe session/failure behavior; security/no-write and evaluation.
- **R2 slices:** corpus approval; scoped schema/pgvector proposal; model/dimension compatibility PoC; versioned ingestion; scoped retrieval/grounding; RAG outage behavior and evaluation.

Do not make vector ingestion or LLM function calling a prerequisite for deterministic room discovery. UI/detail work belongs to Member 4; Core API/schema/security work belongs to Member 2.

## Deliverable format

For every slice provide:

- Goal and owner.
- Verified inputs and pending approvals.
- Files permitted to change.
- Steps with no executable placeholder behavior.
- Tests/eval and expected evidence.
- Backward-compatibility and rollback.
- Go/no-go and stop condition.

## Verification and stop conditions

Validate all planned paths and commands against the repository. Ensure no slice silently edits another owner's module, weakens security, changes gold answers, or enables T12 business writes.

Stop when the base SHA is unverified, a worktree contains unowned changes, a required owner contract is missing, or a plan would require destructive Git/DB actions. Report the exact blocker; do not fall back to a dirty checkout.
