# Hotel Booking System

## Project identity

This repository is an internal hotel-chain management and reservation platform. The backend is a Java 21 and Spring Boot modular monolith in backend; the frontend is a React, TypeScript, and Vite application in frontend.

Current backend modules are common, iam, organization, property, inventory, pricing, audit, and aiassistant. Keep feature code in its owning module. Do not assume planned modules, a JPA-first persistence layer, or a mature frontend architecture exists.

## Source of truth

Use this precedence when sources disagree:

1. Current source code
2. Automated tests
3. Flyway migrations and the live-schema assumptions they encode
4. OpenAPI and API contracts
5. ADRs and maintained project documentation, especially backend/README.md and docs/ai/
6. The task description

Report a material conflict instead of silently choosing a stale document. Root README.md and the legacy database scripts contain historical material; do not treat them as the current schema or container bootstrap contract without checking the backend and compose configuration.

## Architecture and API

- Keep controllers focused on HTTP mapping, input validation, and response shaping. Put use-case logic, authorization decisions, and transaction boundaries in services.
- Follow the local module pattern before adding abstractions. Existing persistence is JdbcTemplate-centric; do not introduce JPA entities, JpaRepository, MapStruct, or service interfaces merely by convention.
- Cross-module access goes through the owning module's public service API, never directly through its persistence code. Avoid dependency cycles.
- Preserve the existing API namespace and response envelope: /api/v1 and ApiResponse with code, message, and result. Locate all callers before changing a shared request or response.
- The frontend is currently a Vite starter. Do not presume a router, API client, Keycloak adapter, form library, or feature-folder structure until the task adds it deliberately.

## Spring Boot, database, and security

- Use constructor injection, Bean Validation at the request boundary, explicit transaction intent, and the project's error handling pattern. Do not swallow exceptions or weaken validation to make a build pass.
- Put mutable schema changes in backend/src/main/resources/db/migration as a new, forward-only Flyway migration. Never edit a released migration; inspect the highest version first.
- Treat V001 as a fresh-database baseline. The root database directory is legacy snapshot/manual seed material and is not mounted by the current Compose setup.
- Review indexes, constraints, query impact, and PostgreSQL integration coverage for schema changes.
- Authentication is Keycloak JWT validation. Authorization is server-side and database-backed through StaffAccessService with CHAIN, REGION, or PROPERTY scope. Do not bypass it, trust a client-supplied actor or hotel scope, log tokens/secrets, or reduce scope checks.

## AI and RAG

- Keep AI work in aiassistant and preserve the application feature flag: AI can be disabled and must fail safely without a provider.
- Ground knowledge answers with the existing scoped retrieval flow. Citations must refer to retrieved, still-authorized sources; never fabricate them.
- Keep deterministic facts, especially room quotes, in owning services. AI code must not access SQL or repositories directly, and RoomQuoteTool must continue through PricingQuoteService.
- Preserve document publication, revocation, actor, and scope checks. Do not claim roadmap capabilities, such as multi-turn persistence or provider-backed CI smoke tests, already exist.
- Never put secrets or sensitive customer data into prompts, fixtures, logs, or committed configuration.

## Verification

Run the smallest relevant checks first, then broaden verification for cross-module, migration, security, or AI/RAG changes.

- Backend unit or context tests: mvn -f backend/pom.xml test
- Backend full verification, including Testcontainers PostgreSQL and pgvector integration: mvn -f backend/pom.xml verify
- Frontend: npm --prefix frontend run lint and npm --prefix frontend run build
- Compose syntax when a valid local .env and Docker are available: docker compose config --quiet
- Always run git diff --check and review git diff before handoff.

The checked-in Windows Maven wrapper is currently not a reliable local verification path. Use the global Maven command above until the wrapper is repaired in a separately scoped change. Full backend verification requires a working Docker daemon; state plainly when it could not run.

## Git and agent workflow

- One focused task maps to one branch and one pull request. Use ai/AIC-<id>-short-description, feature/<task-id>-short-description, or fix/<task-id>-short-description when no established convention supersedes them.
- Preserve unrelated working-tree changes. Do not force-push, rewrite history, delete volumes, commit secrets, or commit/push unless the user asks.
- Before changing code: inspect the relevant module, a similar implementation, affected contracts, migrations, permissions/scopes, and consumers. Plan non-trivial work.
- After changing code: run targeted verification, run broader checks proportional to risk, review the diff, confirm acceptance criteria, and report any unverified item.
- Delegate independent exploration, review, or verification for substantial work. Do not send concurrent agents to edit the same scope without an explicit ownership split.

See docs/codex/README.md for the repository workflow and available skills.
