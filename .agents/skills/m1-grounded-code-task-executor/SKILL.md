---
name: m1-grounded-code-task-executor
description: Execute Member 1 coding tasks with strict repository grounding, evidence mapping, minimal scoped planning, source-accurate implementation, and truthful verification. Use when Gemini/another coding model must read existing code before deciding what to change. Do not use this skill to bypass owner approvals or repository rules.
---

# M1 Grounded Code Task Executor

Use this skill to prevent "plausible but wrong" coding. The model must understand the current code path before it proposes or changes implementation.

This skill is especially suitable for fast models such as Gemini Flash, where disciplined evidence collection is more important than broad speculative reasoning.

## Inputs

- The user's exact task and acceptance intent.
- Current repository/worktree.
- `AGENTS.md`, `AGENTS.member1.md`, `.agents/rules/08-evidence-grounding-and-no-fabrication.md`, and the relevant domain rule/skill.
- Existing source, tests, migrations/config, contracts, and docs for the affected path.

## Core algorithm

Follow these phases in order. Do not skip directly from user request to code.

### Phase 0 — Preflight

Inspect and record:

```text
repo
branch
HEAD
base SHA if relevant
git status
existing dirty/untracked files
requested task type: inspect | plan | implement | review | fix
owner/domain
```

If the working tree contains unrelated user changes, protect them and work around them. Never normalize the tree destructively.

### Phase 1 — Translate the request into acceptance criteria

Rewrite the task internally as concrete statements, not vague intent.

For each criterion specify:

```text
AC-ID
required behavior
must-not-change behavior
observable result
verification method
```

Do not invent missing product requirements. If a missing detail can be derived from approved contracts or current conventions, verify it. Otherwise mark it `UNKNOWN` or `BLOCKED`.

### Phase 2 — Repository reconnaissance

Find the actual implementation path before planning changes.

Use targeted search, then read the relevant code in context. At minimum inspect:

1. entry point/controller/handler;
2. request/response DTOs and validation;
3. service/orchestrator/domain logic;
4. repository/client/adapter dependencies;
5. entity/schema/config/dependency definitions when relevant;
6. all important callers/references;
7. tests/evals for the same behavior;
8. rules/spec/ADR governing the feature.

For bugs, reproduce the failing path from code/tests when possible. For new behavior, identify the nearest existing analogous implementation instead of generating framework boilerplate from memory.

### Phase 3 — Build the evidence ledger

Create a compact ledger before making decisions:

```text
[CONFIRMED] <fact> — <path:symbol/test/command>
[PROPOSED]  <new behavior or new identifier>
[UNKNOWN]   <not yet verified>
[BLOCKED]   <requires owner/contract/decision>
```

Critical facts that must be explicitly grounded when relevant:

- exact API route/method/request/response/error envelope;
- exact Java/TypeScript symbol names and signatures;
- authorization roles and real Spring Security mapping;
- tenant/hotel/session ownership rules;
- database table/column/type/constraint and migration state;
- date/timezone semantics;
- money/currency/tax/fee semantics;
- side effects/transactions/concurrency/idempotency;
- external dependency version/API;
- AI model/tool/retrieval trust boundary;
- expected tests/eval and existing commands.

Do not proceed on a critical `UNKNOWN` if repository inspection can resolve it.

### Phase 4 — Gap analysis

Compare `CURRENT` to `TARGET` criterion by criterion.

For each acceptance criterion identify:

```text
CURRENT: verified behavior
TARGET: required behavior
GAP: exact difference
CHANGE: minimal implementation needed
FILES: exact allowlist
RISK: ownership/security/data/compatibility
VERIFY: exact command/test
```

No gap means no code change for that criterion.

### Phase 5 — Minimal implementation plan

Order changes so that each slice can be verified independently.

Preferred slice pattern:

1. failing/coverage test when appropriate;
2. smallest production change;
3. targeted verification;
4. re-read diff and context;
5. next slice.

Do not include unrelated cleanup. Do not edit another owner's files to make integration convenient. Use approved interfaces/fixtures and report the dependency instead.

### Phase 6 — Implement source-accurately

While editing:

- copy exact local conventions from neighboring production code;
- use verified method/field/route/config names;
- preserve established envelopes and error semantics;
- keep behavior deterministic where business truth is required;
- validate all model output/retrieved text before it can affect application behavior;
- never fabricate authoritative hotel facts or business state;
- never broaden privileges, tenant scope, database writes, or external tool access unless the accepted task explicitly requires and authorizes it.

For Member 1/T12 specifically, obey the existing zero-business-write boundary and Core ownership rules. This skill does not supersede `.agents/rules/07-member-1-architecture-pipeline.md`.

### Phase 7 — Verification ladder

Run only commands that are relevant and safe for the current environment, but report truthfully.

Recommended order:

1. narrow test for touched behavior;
2. related test package/module;
3. compile/typecheck/lint;
4. broader regression suite required by repository guidance;
5. security/architecture/eval checks if the change affects those boundaries;
6. diff/status checks.

For every command capture:

```text
command
result: PASS | FAIL | NOT RUN
important output
```

If dependencies/network/environment prevent execution, do not retry by changing project versions or bypassing checks unless authorized. Mark `NOT RUN` and explain the exact blocker.

### Phase 8 — Adversarial self-review

Review the final diff as if trying to prove it wrong.

Search specifically for:

- invented or mistyped symbols;
- code paths not covered by the plan;
- happy-path-only handling;
- null/empty/boundary errors;
- inconsistent status/error envelopes;
- hidden write side effects;
- authorization/tenant/session bypass;
- stale price/availability or RAG overriding Core truth;
- hard-coded production facts;
- PII/secrets in logs/tests/prompts;
- test weakening;
- unrelated formatting/refactors;
- unexpected files or overwritten user changes.

Re-open changed methods and their direct callers after editing. Do not rely on memory of what was changed.

## Stop conditions

Stop the affected implementation slice and report `BLOCKED` when correctness requires any of the following and evidence/approval is unavailable:

- a missing or contradictory owner-owned API/schema/security contract;
- uncertain pricing, availability, date, currency, tax/fee, tenant, or session semantics;
- a required edit to another member's owned business logic;
- destructive migration or Git operation;
- unknown dependency/provider compatibility that cannot be verified;
- unresolved PII/privacy/retention requirements;
- unexpected worktree changes that overlap the intended files.

Do not "make a reasonable guess" for these categories.

## Anti-hallucination micro-checks

Before writing any code line that references an existing repository concept, ask:

```text
Have I seen this exact symbol in source or an approved contract?
Have I verified its signature/type/semantics?
Am I using current code, not an old plan or memory?
```

Before stating a result, ask:

```text
Did I execute the command that proves this?
If not, have I labeled it NOT RUN/HISTORICAL instead of PASS?
```

Before adding a business fact, ask:

```text
What is its authoritative source?
If the answer is LLM memory, sample data, assumption, or RAG without approval: do not use it.
```

## Output contract

### Before implementation

Give a short grounded plan only when useful:

```text
Grounded facts
- [CONFIRMED] ... — path:symbol

Unknowns/blockers
- [UNKNOWN/BLOCKED] ...

Plan
1. <file(s)> — <minimal change> — verify with <command/test>
```

### After implementation

```text
Implemented
- <AC-ID -> exact change>

Changed files
- <path> — <why>

Verification
- PASS: ...
- FAIL: ...
- NOT RUN: ...

Evidence-based risks / blockers
- ...

Git state
- branch / HEAD / status / commit-push-merge state
```

Keep claims proportional to evidence. If verification is incomplete, the final conclusion must also be incomplete.
