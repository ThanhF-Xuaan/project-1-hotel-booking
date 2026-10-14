---
name: m1-ai-evaluation-security
description: Design or execute Member 1 AI evaluation and security checks for typed extraction, deterministic recommendation, scoped RAG, tenant/session isolation, PII safety, prompt injection, and trace-level zero business writes. Use for T12 release gates and other AI reviews; preserve approved gold data.
---

# M1 AI Evaluation and Security

Produce reproducible evidence without editing gold answers or turning a proposed threshold into a pass claim.

## Required inputs

- Immutable approved eval/gold version, or an explicitly labelled draft awaiting approval.
- Exact Git SHA and prompt/model/embedding/corpus/config versions.
- Executable runner or documented repeatable manual protocol.
- Synthetic fixtures, expected Core responses, trace capture, and approved thresholds.

## Process

1. **Protect evaluation integrity.** Hash or otherwise identify the gold version before execution. Never change cases, answers, or thresholds to improve results. Draft additions go in a new versioned file.
2. **Separate deterministic and model checks.** Test validators, filters, money/date mapping, ranking, DTO/card schema, and side effects with code assertions. Use human/LLM judging only where necessary and disclose sampling/judge limits.
3. **Evaluate conversation behavior.** Typed extraction, missing-field clarification, multi-turn corrections, one-room-only handling, and invalid dates/scope.
4. **Evaluate recommendation truth.** Public eligibility, distinct room types, capacity, required/preferred amenities, every occupied night, exact quote/currency/fees/budget semantics, deterministic ranking, fewer matches, verified IDs, detail navigation and stale-price warnings.
5. **Evaluate resilience.** Candidate/fan-out bounds, partial failures, rate limits, and distinct `NO_MATCH` versus `TEMP_UNAVAILABLE` for model/RAG/Core outages.
6. **Evaluate security.** Owner-bound sessions, cross-hotel isolation, prompt/document injection, untrusted model output, PII/log leakage, arbitrary URL/SQL denial, and dependency/network traces proving zero hold/release/booking/payment/guest writes.
7. **Evaluate Release 2 separately.** Approved corpus/versioning, scoped retrieval, groundedness/citations, effective-version behavior, embedding dimension/cardinality/model compatibility, activation and rollback.
8. **Report truthfully.** For every check provide `PASS`, `FAIL`, or `NOT RUN`, command/protocol, evidence, and limitation. Record latency/cost only from observed approved runs.

## Deliverables

- Versioned evaluation plan/scorecard and failure taxonomy.
- Sanitized representative failures with expected versus actual behavior.
- Trace-level no-write and tenant/PII/security evidence.
- Approval/blocker checklist and reproducibility metadata.

## Verification and stop conditions

A draft dataset is `DRAFT / NOT APPROVED / NOT RUN`. Numeric thresholds stay `PROPOSED` until a human approver records approval. Synthetic-only results cannot establish real-world quality.

Stop if the gold set, runner, model/corpus/config identity, owner-approved thresholds, privacy basis, or trace visibility is missing. Do not call paid/live models or external systems without authorization. A security failure blocks release and must not be hidden by aggregate metrics.
