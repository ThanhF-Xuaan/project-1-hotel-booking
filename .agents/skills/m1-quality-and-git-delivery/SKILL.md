---
name: m1-quality-and-git-delivery
description: Perform final Member 1 diff review, relevant build/test/eval verification, privacy and ownership checks, and prepare a safe Git/PR handoff. Use when authorized implementation or documentation changes are ready for review; do not commit, push, open, or merge without explicit authorization.
---

# M1 Quality and Git Delivery

Provide an evidence-based handoff while preserving Git state and human review authority.

## Required inputs

- Exact branch/HEAD/base SHA and current worktree status.
- Approved scope/file allowlist, spec/plan, contracts, and Definition of Done.
- Relevant repository commands and, for AI work, approved eval version/runner/thresholds.

## Process

1. **Audit scope:** inspect status and diff; compare changed paths and behavior to the authorized plan. Treat unexpected or unowned changes as protected and stop that part.
2. **Review quality:** check validation, errors, tenant/RBAC, money/date semantics, side effects, compatibility, secrets/PII, placeholders, and unrelated refactors.
3. **Run proportional verification:** targeted tests first, then relevant regressions/build/lint. Run AI evaluation/security checks only with approved inputs and authorization. Do not run migrations or live/paid calls implicitly.
4. **Inspect delivery state:** run `git diff --check`, review stats/full diff, verify only expected paths, and ensure no generated/secrets/data artifacts entered the change.
5. **Prepare handoff:** summarize behavior, contracts, tests/eval, privacy/security, known risks, rollback, approvals, and unverified items. Suggest a focused Conventional Commit message only if useful.

## T12 release review

- Confirm one-room option and 0..5 verified card behavior, authoritative price/availability, deterministic ranking, approved detail action, and RAG-independent Release 1.
- Inspect dependency/tool/network traces for zero hold/release/booking/payment/guest writes, including adversarial flows.
- Confirm session/tenant/PII controls, outage distinctions, candidate bounds, and Release 2 corpus/model/schema approval evidence.
- Preserve approved evaluation answers and thresholds. Draft or unexecuted evaluation remains `NOT RUN`.

## Deliverables

- Complete changed-file list and behavioral/contract impact.
- `PASS`/`FAIL`/`NOT RUN` table with exact command/file and notes.
- Findings by severity, remaining blockers, owner sign-off checklist, rollback and Git mutation status.
- Optional PR description/commit proposal; neither is execution authority.

## Stop conditions

Stop if the repository/base is wrong, unexpected changes appear, owner approval is missing, validation would mutate an unapproved environment, tests fail materially, secrets/real PII are present, or T12 exposes a business write path.

Never reset/clean/stash/rebase/force-push to make a diff look clean. Never stage, commit, push, create a PR, or merge unless the user explicitly authorizes that specific action.
