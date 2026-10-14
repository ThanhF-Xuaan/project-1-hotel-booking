---
name: m1-ai-rag-developer
description: Build or review Member 1 Spring AI and Google Gemini conversation orchestration, typed criteria extraction, response explanation, and bounded read-only AI integration for T12. Use for the AI-facing orchestration layer; use the recommendation-engine and knowledge-lifecycle skills for deterministic ranking and RAG storage.
---

# M1 AI and RAG Developer

Keep probabilistic language tasks separate from authoritative Java business orchestration.

## Required inputs

- Approved T12 request/response/status contract and Release 1/2 scope.
- Verified Spring Boot/Spring AI/provider versions and an approved credential/configuration approach.
- Typed search-criteria model and fixed-operation read-only interfaces.
- Approved session ownership/TTL design and AI evaluation cases.

## Process

1. **Define the LLM boundary.** Gemini may propose typed criteria and generate explanations from verified facts. Java validates every field and decides whether to clarify, search, or return an error.
2. **Extract typed criteria.** Track user-provided, inferred, and default values. Validate date order, occupants, `roomsCount`, currency/budget basis, required/preferred amenities, and hotel/location ambiguity. Missing blocking fields produce `NEED_INFO` without Core calls.
3. **Merge multi-turn changes safely.** Material changes invalidate previous quotes/cards. Session identifiers do not establish identity or hotel authorization; use the approved owner-binding mechanism and minimal normalized state.
4. **Call only fixed read operations.** Direct Java orchestration is preferred for discovery/filtering/pricing/ranking. If Spring AI tools are used, expose narrow read-only functions only. Function calling is not mandatory.
5. **Compose safely.** Structured cards come from verified Java data. The model may explain supplied facts but cannot create IDs, prices, availability, routes, scores, promotions, or policies.
6. **Degrade distinctly.** Bound model tokens/timeouts/retries/rate limits. Model failure may fall back to a safe clarification or deterministic flow where supported. Never convert outages into `NO_MATCH` or fake recommendations.
7. **Protect data.** Redact logs, minimize provider payloads, treat prompts/documents as untrusted, and record the approved model/prompt/config version without secrets.

## Deliverables

- Typed criteria extraction/validation contract and orchestration code within the authorized AI module.
- Narrow read-only tool/interface allowlist, if tools are used.
- Session/failure/observability behavior and sanitized prompt templates.
- Unit/contract/security tests and evaluation trace metadata.

## Non-negotiable boundaries

- No hold/release, booking create/update/cancel, payment/refund, guest mutation, generic HTTP/SQL executor, arbitrary URL fetching, or broad write-capable dependency.
- No local Pricing/Inventory calculations and no RAG-based live facts.
- No Python/LangChain or new vector service for T12 unless a later ADR explicitly replaces the confirmed stack.
- No real PII/secrets in prompts, logs, fixtures, or model calls without documented approval.

## Verification and stop conditions

Verify schema-invalid model output, clarifications, corrections, session isolation, timeouts/rate limits, prompt injection, sanitized logs, and dependency traces showing zero business writes. Report live-model checks separately from deterministic tests.

Stop if provider/model compatibility, session ownership, read contract, or privacy approval is missing; if a required action belongs to another owner; or if any registered dependency can reach a prohibited write path.
