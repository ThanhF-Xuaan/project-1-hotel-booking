# Member 1 Task Prompts

> These templates implement the risk-based workflow in `AGENTS.member1.md`. Replace bracketed values. A prompt does not expand authorization or turn a proposed contract into an implemented one.

## 1. Audit a task before planning

```text
Audit Member 1 task [T04 | T05 | T12 | U13 | U14 | U22] at [target branch/SHA].
Read AGENTS.md, AGENTS.member1.md, .agents/rules/07-member-1-architecture-pipeline.md,
and only the specialist skills relevant to this task.

Read-only first:
1. Verify repository, remotes, branch/HEAD/base SHA, worktrees and git status.
2. Identify the owner, current source/tests/OpenAPI and actual dependencies.
3. Label every consequential fact CONFIRMED, PROPOSED, UNKNOWN or BLOCKED.
4. List contract gaps and owner approvals; do not invent endpoints, roles, routes, schemas,
   model IDs, eval commands or legal requirements.
5. If the request is audit/plan only, do not edit or mutate Git/DB state.
```
## 2. Design or plan T12

```text
Design/plan T12 as the AI Room Recommendation Assistant.

Binding product rules:
- Java 21/Spring Boot + Spring AI/Google Gemini.
- Gemini proposes typed criteria; Java validates, discovers candidates, hard-filters,
  calls authoritative read-only Availability/Pricing, ranks deterministically and builds cards.
- Return 3-5 verified distinct room-type recommendations when available, otherwise fewer.
- MVP is one room per option. Keep roomsCount; clarify/redirect when roomsCount > 1.
- Release 1 room recommendation works without RAG.
- Release 2 uses approved PostgreSQL/pgvector static knowledge only.
- Zero business writes: never hold/release inventory, mutate bookings, start payments/refunds,
  or mutate guest/business records—even after confirmation. No generic HTTP/SQL/write tools.
- Cards use only a Member 4-approved view-details action; do not invent a route.

Deliver:
1. Evidence-based current baseline and contradictions.
2. ADR plus current-versus-proposed contract table.
3. Member 2/4 contract requests and explicit approval gates.
4. Small implementation slices with file allowlists, fixtures, tests, rollback and stop conditions.
5. Security/no-write and AI evaluation matrix. Draft thresholds remain PROPOSED.

Do not implement unless this request explicitly authorizes implementation.
```

## 3. Implement an approved Member 1 slice

```text
Implement only approved slice [slice ID] from [spec/plan path] in [authorized worktree].

Before editing, verify repository, branch/HEAD/base SHA, status, file allowlist, owners and contracts.
Stop if the base is wrong, unexpected changes exist, or an owner contract is missing.
Use synthetic fixtures only for pending dependencies and never expose fixture facts as production data.

Make the smallest complete change inside the approved file allowlist. Do not modify shared
Booking/Pricing/Inventory/Security/DB/Frontend behavior without the named owner's approval.
Do not commit, push, open a PR, merge, run a migration or call a paid/live model unless this
request explicitly authorizes that specific action.

For T12, prove the read-only dependency allowlist and zero calls to hold/release/booking/payment/guest writes.
Run only relevant existing checks and report each as PASS, FAIL or NOT RUN with evidence.
```

## 4. Draft or run AI evaluation

```text
Evaluate [feature/SHA] using [eval path/version].

First state whether the eval is APPROVED or DRAFT. If DRAFT, do not report release pass/fail
against unapproved thresholds. Never edit the gold cases, answers or thresholds during execution.
Use synthetic data and record prompt/model/embedding/corpus/config/SHA without secrets.

Cover deterministic behavior, model behavior, tenant/session isolation, prompt/document injection,
PII/log safety, outage distinctions and trace-level zero business writes. Separate NOT RUN checks
when a runner, service, contract, approval or paid-model authorization is absent.
Return a PASS/FAIL/NOT RUN scorecard and sanitized representative failures.
```

## 5. Final review and handoff

```text
Review the completed scope [task/slice] without committing, pushing, opening a PR or merging.

1. Compare status/diff to the approved file allowlist and plan.
2. Check owner boundaries, contracts, backward compatibility, security, PII/secrets and placeholders.
3. Run relevant existing validation; do not start Docker/migrations/live models without approval.
4. For AI, preserve approved eval data and report actual scorecard/no-write evidence.
5. Run git diff --check, inspect diff/stat/status and list every changed file.
6. Report base SHA, behavior/contracts, PASS/FAIL/NOT RUN table, unverified areas,
   remaining blockers, M2/M4/human approvals, rollback and Git mutation status.

AI review supplements human peer review and never marks owner approvals as complete.
```
