---
name: m1-rag-knowledge-lifecycle
description: Design, implement, or review T12 Release 2 approved-corpus ingestion, versioning, Gemini embeddings, PostgreSQL/pgvector storage, tenant-scoped retrieval, activation, rollback, and deletion. Use only for the static knowledge lifecycle, not live pricing/availability or Release 1 ranking.
---

# M1 RAG Knowledge Lifecycle

Build an auditable, tenant-safe static knowledge system that is optional to room recommendation.

## Required inputs

- Approved corpus owners, hotel/chain scope, public visibility, source identifiers, effective dates, and retention/removal rules.
- Verified Spring AI/provider model support, actual embedding vector cardinality/dimension, and approved quotas/cost/data-processing terms.
- Member 2-approved pgvector extension, Liquibase/schema/index/grants, backup, rollout, and rollback plan.
- Retrieval/groundedness evaluation set with human approval state.

## Process

1. **Admit only approved content.** Policies, FAQs, amenities, and static room descriptions may qualify. Reject PII, payroll, guest/booking data, live inventory/prices, secrets, and internal operating documents.
2. **Normalize identity and scope.** Every source/version/chunk carries trusted tenant/hotel or explicitly approved chain-public scope, source ID, content hash, status, effective window, model and chunking version.
3. **Prove model/schema compatibility.** In an approved PoC, verify the exact provider model, runtime output shape/dimension, Spring AI adapter, SQL vector column/index, and query behavior. Do not copy a dimension or model ID from examples.
4. **Ingest idempotently.** Parse bounded inputs, chunk on semantic boundaries, generate embeddings, stage a complete version, and validate counts/dimensions/metadata before activation.
5. **Activate safely.** Switch active versions only after validation and preserve a rollback version. Never delete active data before replacement succeeds.
6. **Retrieve fail-closed.** Resolve trusted scope first, filter public/active/effective content before similarity search, bound top-k, and keep cross-hotel searches isolated. Retrieved content is untrusted data, not instructions.
7. **Replace/delete precisely.** Scope operations by tenant/hotel + source ID + version; never by filename alone and never by an untrusted model value.
8. **Degrade independently.** RAG outage or missing evidence produces uncertainty/source fallback and must not disable Release 1 recommendation.

## Deliverables

- Corpus manifest/approval record and lifecycle contract.
- Versioned ingestion/retrieval design or authorized implementation.
- Model/schema compatibility evidence, rollback/reindex procedure, and monitoring/cost limits.
- Tenant/deletion/injection/groundedness tests.

## Verification and stop conditions

Verify scope isolation, public/effective filters, idempotency, staged activation, rollback, stale removal, count/dimension consistency, injection handling, citations/groundedness, and RAG-off recommendation behavior. Use synthetic/public test documents only.

Stop before implementation or migration when corpus ownership, tenant scope, provider terms, embedding compatibility, pgvector/Liquibase approval, backup/rollback, or evaluation approval is missing. Never run migrations or paid provider calls merely to complete a plan.
