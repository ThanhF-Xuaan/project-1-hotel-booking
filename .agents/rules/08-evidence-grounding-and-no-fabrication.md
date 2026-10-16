---
trigger: always_on
description: "Strict evidence-grounding and anti-fabrication rule for code-reading, planning, implementation, and verification. Optimized for fast coding models such as Gemini Flash."
---

# Evidence Grounding and No-Fabrication Rule

> **Purpose:** Reduce hallucination by forcing every consequential technical claim and change to be traceable to current repository evidence. This rule supplements `AGENTS.md`, `AGENTS.member1.md`, and domain rules. It does not grant permission to cross ownership, security, schema, API, or Git boundaries.

## 1. Non-negotiable truth policy

Never present a guess as a repository fact.

For every consequential statement about current behavior, contracts, APIs, symbols, schemas, permissions, dependencies, tests, configuration, or business rules, classify it internally as exactly one of:

- `CONFIRMED` — directly supported by current source, test, generated/runtime contract, approved specification, or command output at the target SHA.
- `PROPOSED` — a recommended change that does not exist yet.
- `UNKNOWN` — evidence has not been found or is ambiguous.
- `BLOCKED` — correctness depends on missing/contradictory evidence or an owner decision.

If evidence is missing, say `UNKNOWN` or `BLOCKED`. Do not fill gaps from memory, naming conventions, framework habits, prior branches, comments, examples, or likely intent.

### Forbidden fabrication

Do not invent or silently assume any of the following:

- files, classes, methods, packages, constructors, annotations, DTO fields, enum values, routes, query parameters, headers, response shapes, error codes, roles, permissions, environment variables, configuration keys, database tables/columns/indexes/constraints, migrations, events, message topics, feature flags, library APIs, model capabilities, test results, commands, logs, business rules, pricing/tax semantics, tenant scope, or security behavior;
- production data, hotel facts, policy text, prices, availability, guest data, credentials, or PII;
- the existence of a contract merely because a plan, TODO, old branch, README example, fixture, mock, or generated draft mentions it.

When proposing a new identifier or contract, label it `PROPOSED` until approved and implemented.

## 2. Source-of-truth hierarchy

Use the strongest available source for the type of claim being made. Do not collapse "current implementation" and "desired specification" into one truth.

### Current behavior

Prefer, in order:

1. Current target Git SHA and working tree state.
2. Executed runtime/generated contract or command output when the task specifically concerns runtime behavior.
3. Current implementation and call sites.
4. Current tests that exercise the behavior.
5. Current migrations/configuration/dependency manifests.
6. Approved repository documentation/ADR/specification.
7. Plans, comments, examples, old branches, mocks, fixtures, and prior conversation context only as leads to verify.

### Desired behavior

Prefer approved specification/ADR/acceptance criteria and explicit user requirements. If desired behavior conflicts with current implementation, state both separately:

- `CURRENT:` what the repository does now.
- `TARGET:` what the approved task requires.

Never rewrite current behavior in prose to make it appear compliant with a target specification.

## 3. Mandatory repository grounding before planning or editing

Before a non-trivial code task, inspect enough of the real repository to answer all relevant questions below.

### Git and scope

- repository identity and remote;
- current branch, HEAD SHA, intended base SHA when known;
- `git status` and existing dirty/untracked files;
- task owner and files/modules that are out of scope.

Treat existing user changes as protected. Never reset, clean, stash, overwrite, rebase, or delete them unless the user explicitly authorizes that exact action.

### Code understanding

Do not infer behavior from a filename, class name, method name, test name, or short snippet. Read the complete relevant implementation path.

For changed behavior, trace as applicable:

```text
entry point / controller / handler
  -> DTO and validation
  -> service/orchestration
  -> domain logic
  -> repository/client/adapter
  -> entity/schema/configuration
  -> callers and side effects
  -> tests/eval that define expected behavior
```

Search both **definition and references** for important symbols. Inspect adjacent code when it changes semantics, especially security, transactions, concurrency, serialization, date/time, money, tenant scope, retries, persistence, external APIs, and AI tool boundaries.

### Exact identifiers

Copy identifiers from source. Do not reconstruct them from memory.

If line numbers are available, cite `path:line-range`. If line numbers are unavailable, cite `path` plus exact class/method/symbol. Never invent line numbers.

## 4. Evidence map before implementation

For any task that changes behavior, build a compact internal evidence map before editing:

| Claim / requirement | Status | Evidence | Consequence |
|---|---|---|---|
| What currently happens | `CONFIRMED/UNKNOWN` | file + symbol/test/command | preserve/change |
| What user requires | `CONFIRMED` | current task | acceptance criterion |
| Contract dependency | `CONFIRMED/PROPOSED/BLOCKED` | owner source | integration gate |
| Security/data rule | `CONFIRMED/UNKNOWN` | config/rule/test | implementation constraint |
| Verification path | `CONFIRMED/UNKNOWN` | existing scripts/tests | completion gate |

Do not implement a consequential assumption that remains `UNKNOWN` when it can be verified from the repository with reasonable effort.

## 5. Convert the task into an exact execution contract

Before coding, derive the smallest set of acceptance criteria from the user's request and repository rules.

The execution contract must answer:

- What exact behavior must change?
- What must remain unchanged?
- Which files/modules are allowed to change?
- Which external/member-owned contracts are read-only dependencies?
- What input, output, error, authorization, tenant, date/time, currency/money, privacy, and side-effect semantics apply?
- How will each criterion be verified?

Do not add features merely because they appear useful. No speculative refactors, dependency upgrades, schema changes, cleanup, renaming, architecture rewrites, or "while here" improvements unless required by the task or explicitly approved.

If multiple implementations satisfy the requirement, prefer the smallest change that follows existing repository conventions and preserves compatibility.

## 6. Planning rules

A plan is not implementation evidence. Every plan step must be grounded in files/symbols that actually exist or clearly marked as new `PROPOSED` files.

For each step specify:

1. outcome;
2. evidence/reason;
3. exact file allowlist;
4. change to make;
5. risk/ownership boundary;
6. verification command/test;
7. stop condition.

Do not plan a call to a method, endpoint, table, dependency, or tool until its existence and compatible signature/version are verified, unless the plan explicitly proposes creating it.

## 7. Implementation discipline

During implementation:

- change only files necessary for the accepted task;
- preserve unrelated behavior, formatting, comments, user changes, and public contracts;
- follow current local architecture and naming rather than generic framework templates;
- keep controllers/handlers thin when that is the repository convention;
- validate untrusted input/model/retrieved content in deterministic application code;
- never let an LLM/RAG response establish authoritative price, availability, authorization, tenant, booking, payment, or business state;
- never weaken security or tests to make a task pass;
- never replace a real dependency with a fake production implementation merely to remove a compile error;
- never silently catch an error and claim successful behavior;
- never use TODO/placeholder behavior as if the feature were complete.

After each meaningful slice, re-read the changed code in context and compare it against the execution contract.

## 8. Framework and dependency accuracy

Do not assume a library API from general knowledge.

Before using a framework/library capability:

1. verify the installed dependency and version from the repository;
2. search the current codebase for established usage;
3. inspect local/generated documentation or authoritative external documentation if available and allowed;
4. if compatibility still cannot be verified, mark it `UNKNOWN` and avoid presenting speculative code as production-ready.

This is especially strict for Spring Boot/Security/AI, persistence, database/vector extensions, frontend libraries, authentication, cloud SDKs, and model-provider APIs.

## 9. Test and verification truthfulness

Never claim a command passed unless it was actually executed successfully in the current environment.

Every relevant check must be reported as exactly one of:

- `PASS` — executed and passed;
- `FAIL` — executed and failed;
- `NOT RUN` — not executed, with the exact reason.

Historical reports or existing CI artifacts may be cited only as `HISTORICAL`, never as a current `PASS`.

Verification should include the smallest relevant set of:

- targeted unit/integration/eval tests;
- compile/typecheck/lint/build;
- existing regression suite appropriate to the touched scope;
- API/schema/security contract checks when relevant;
- `git diff --check` or equivalent diff validation;
- final `git diff` and `git status` review for unexpected files and protected changes.

Do not modify expected answers, gold eval data, thresholds, assertions, or test scope merely to convert a failure into a pass.

## 10. Final self-audit before saying "done"

Before completion, answer internally:

1. Did every material repository claim come from inspected evidence?
2. Did I accidentally describe a proposal as existing behavior?
3. Did I use any identifier, API, schema field, role, route, config key, or library feature I did not verify?
4. Did I change anything not required by the task?
5. Did I preserve existing dirty/untracked user work?
6. Did I cross another member's ownership or a security/schema/API boundary?
7. Did I verify failure paths, not only happy paths?
8. Did I truthfully distinguish executed checks from historical/unexecuted checks?
9. Does the final diff implement the user's acceptance criteria exactly, without hidden extra behavior?

If any answer is uncertain, investigate before completion or report the item as `UNKNOWN/BLOCKED/NOT RUN`.

## 11. Required completion format

Keep the final report concise and evidence-based:

```text
Implemented
- <exact behavior changed>

Evidence
- <file:symbol or command supporting the result>

Verification
- PASS: <command/test>
- FAIL: <command/test + reason>
- NOT RUN: <check + exact reason>

Unverified / risks
- <UNKNOWN/BLOCKED items only; write "None known" only if supported>

Git state
- <branch, HEAD, changed files, whether commit/push/merge was performed>
```

Do not use "done", "fixed", "complete", "safe", or "works" as an unsupported summary. The evidence and verification must justify the conclusion.
