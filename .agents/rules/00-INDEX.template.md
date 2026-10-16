---
trigger: always_on
description: "Index and risk-based router for Hotel Booking repository rules and installed specialist skills."
---

# Hotel Booking Rules — Index and Router

> **Status:** PROPOSED shared rule; Member 2 approval is required before merge. `AGENTS.md` defines repository-wide authorization. `AGENTS.member1.md` applies additionally to Member 1 work. A rule or skill does not grant permission to edit another owner's module.

## Rules map

| Area | File | Load when |
|---|---|---|
| Project-wide safeguards | [`01-core-ai-guidelines.template.md`](01-core-ai-guidelines.template.md) | Always |
| Backend conventions | [`02-backend-standards.template.md`](02-backend-standards.template.md) | Backend/API work |
| Frontend conventions | [`03-frontend-standards.template.md`](03-frontend-standards.template.md) | Frontend/UI work |
| QA | [`04-qa-testing-standards.template.md`](04-qa-testing-standards.template.md) | Test design/execution |
| Design system | [`05-design-system-tokens.template.md`](05-design-system-tokens.template.md) | UI styling/components |
| Prompt library | [`06-prompts-library.template.md`](06-prompts-library.template.md) | When an existing prompt pattern is relevant |
| Member 1 boundaries | [`07-member-1-architecture-pipeline.md`](07-member-1-architecture-pipeline.md) | `T04`, `T05`, `T12`, `U13`, `U14`, `U22` |
| Evidence grounding / anti-fabrication | [`08-evidence-grounding-and-no-fabrication.md`](08-evidence-grounding-and-no-fabrication.md) | Always; especially code-reading, planning, implementation, debugging, and review |
| Pricing/concurrency | [`hotel-concurrency-and-pricing.md`](hotel-concurrency-and-pricing.md) | Owner-approved Pricing/Inventory concurrency work |

## Risk-based routing

- **Question/audit/review:** inspect only; load the smallest relevant rules and report evidence.
- **Plan/design:** load the owner/domain rules, document contracts/approvals/tests, and do not implement.
- **Implementation:** load rules and only the specialist skills needed for the authorized scope; make small verifiable changes.
- High-risk work involving shared APIs/schema/security, PII, payments, inventory writes, or model-provider data requires explicit owner decisions. Do not force every task through an entity-first, frontend-first, function-calling, or vector-first cascade.

## Member 1 skill discovery

Discoverable skills live under `.agents/skills/<skill-name>/SKILL.md`. Exactly these nine `m1-*` skills are intended:

1. `m1-spec-designer`
2. `m1-execution-planner`
3. `m1-ai-rag-developer`
4. `m1-data-processing-developer`
5. `m1-quality-and-git-delivery`
6. `m1-room-recommendation-engine`
7. `m1-rag-knowledge-lifecycle`
8. `m1-ai-evaluation-security`
9. `m1-grounded-code-task-executor`

Activate only skills whose description matches the current task. Missing skills or referenced files must be reported, not invented. Detailed procedures belong in skills, not in this always-on index.

## Universal safeguards

- Verify source and contracts at the target SHA; label proposals and unknowns.
- Preserve dirty/untracked work and ownership boundaries.
- Use synthetic data; protect secrets and PII.
- T12 has zero business write capability: no hold/release, booking mutation, payment/refund, or guest/business mutation.
- Keep production changes complete and minimal; no fake executable placeholders.
- Run only relevant existing checks and report `PASS` / `FAIL` / `NOT RUN` truthfully.
