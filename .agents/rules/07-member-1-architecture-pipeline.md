---
trigger: always_on
description: Architecture pipeline and mandatory development lifecycle strictly governing Member 1 (AI & Data Specialist) features (T04, T05, T12, U13, U14, U22).
---

# 🛡️ Member 1 (AI & Data Specialist) Architecture Pipeline & Lifecycle Standards

> **Scope:** Strictly governs development for Member 1 (AI & Data Specialist).  
> **Tech Stack:** Java 21, Spring Boot 4.0.7, Spring AI / LangChain, PostgreSQL 16 (PGVector), Redis 7, Keycloak IAM.  
> **Governing Skills:** `m1-spec-designer`, `m1-execution-planner`, `m1-ai-rag-developer`, `m1-data-processing-developer`, `m1-quality-and-git-delivery`.

---

## 1. Scope of Responsibility & Strict Boundary Enforcement

The AI Agent must strictly restrict its actions to tasks and modules owned by **Member 1**. Never modify or cross over into architectural boundaries owned by other members without explicit instruction:

### ✅ Permitted Member 1 Feature Scope:
* **T04: Utility & Master Data BE**
  * Utility meter and reading management (`UtilityMeter`, `UtilityReading`).
  * Master data catalog CRUD (`Region`, `Property`, `RoomType`).
* **T05: Lodging Queue & Payroll Import BE**
  * `StayGuest` collection and lodging queue management.
  * Vietnamese Police Lodging Declaration Export (BCA police format - XML/CSV/text).
  * Payroll summary streaming import with row-level validation.
* **T12: RAG Chatbot AI Core**
  * Multi-tier RAG Chatbot pipeline (Spring AI / LangChain + PGVector).
  * Knowledge base ingestion (hotel rules, amenities, check-in/out policies).
  * Real-time room availability and rate consultation via Function Calling.
* **U13: AI OCR Passport/CCCD Parser**
  * Vision OCR model integration for automated identity extraction (CCCD 12-digit, Passport).
* **U14: AI Guest Personalization Engine**
  * Stay history analytics, dynamic rate plan recommendations, and personalized upsells.
* **U22: AI Predictive Analytics & Anomaly Detection**
  * 30-day occupancy forecasting model and utility leakage Z-Score anomaly detection.

### 🚫 Prohibited Cross-Module Interference (Zero Tampering):
* **Do NOT modify Member 2's Core:** Core Booking Engine (FIT/GIT), 15-minute Redis session hold locking, Pricing Pipeline Pattern, Keycloak Realm infrastructure.
* **Do NOT modify Member 3's Modules:** Payment Gateway Webhooks (VNPay/MoMo), POS (F&B/Laundry), Maintenance tickets (OOO/OOS).
* **Do NOT modify Member 4's Frontend:** App shell layout, Room Timeline Grid, Guest Booking UI flow, BI Executive Charts.

---

## 2. Mandatory 5-Stage Architecture Pipeline

Every new feature or modification undertaken by Member 1 MUST sequentially execute through this 5-stage pipeline:

```text
[STAGE 1: SPEC DESIGN] ──► [STAGE 2: EXECUTION PLAN] ──► [STAGE 3: CASCADE DEV]
                                                                  │
                                                                  ▼
[STAGE 5: GIT & DELIVERY] ◄──── [STAGE 4: QA & EVALUATION] ◄──────┘
```

---

### STAGE 1: Requirement Analysis & Technical Spec (`m1-spec-designer`)
Before writing any implementation code:
1. **Classify Track:**
   * **Track A (Data Submodule - T04, T05):** Define PostgreSQL 16 schema, foreign keys, soft delete, DTO contracts (`CreateRequest`, `UpdateRequest`, `SearchDto`, `Response`), and external file schemas (BCA format, Payroll CSV/Excel).
   * **Track B (AI Core - T12, U13, U14, U22):** Define Prompt templates, PGVector chunking, Vector index strategy, Function Calling tool signatures, and fallback strategies.
2. **Define RBAC Matrix:** Map every endpoint to specific Keycloak roles (`ROLE_CHAIN_ADMIN`, `ROLE_PROPERTY_MANAGER`, `ROLE_RECEPTIONIST`, etc.).
3. **Present Spec to User:** Obtain explicit confirmation before proceeding to planning.

---

### STAGE 2: Micro-Step Execution Planning (`m1-execution-planner`)
1. **Cascade Layering:** Formulate the step-by-step checklist strictly respecting dependency order:
   * *Track A:* `Entity/Migration` ➔ `DTO & Search DTO` ➔ `Mapper` ➔ `Repository` ➔ `Service` ➔ `Controller` ➔ `Unit Test`.
   * *Track B:* `Config/Credentials` ➔ `Vector Store Ingestion` ➔ `Tool Calling` ➔ `AI Service` ➔ `Controller` ➔ `Evaluation`.
2. **Zero-Placeholder Guarantee:**
   * No `// TODO`, `// implement later`, or dummy stubs permitted in generated code.
   * Every file must compile immediately with full imports and type safety.

---

### STAGE 3: Cascade Implementation
Execute code strictly adhering to Member 1's domain rules:

#### A. Data Submodule Standards (`m1-data-processing-developer` & `hotel-api-development`):
* **Soft Delete:** All select and search queries must include `is_deleted = false`.
* **Unified Batch Delete:** Implement `DELETE /api/v1/<resource>/delete` taking `List<Integer> ids` (or `List<Long> ids`), setting `is_deleted = true`.
* **Search DTO:** Multi-criteria search endpoints must use `POST /api/v1/<resource>/filter` with `@RequestBody` search DTO.
* **Streaming I/O:** Large files (Payroll, BCA export) must use streaming parsers (e.g., SAX, FastCSV) wrapped in `try-with-resources` to avoid Out-Of-Memory (OOM) errors.
* **BCA Lodging Export:** Follow Vietnamese Police regulations for `StayGuest` export; purge temporary files immediately after streaming over HTTPS.

#### B. AI Core Standards (`m1-ai-rag-developer`):
* **Mandatory Function Calling:** Never let LLM hallucinate room availability or rates. Real-time rates and vacancies MUST be fetched by invoking registered Core Backend tools (`checkRoomAvailability`, `calculateRoomPrice`).
* **Vector Store Partitioning:** Always pre-filter PGVector similarity queries by `hotel_id`.
* **Resilience:** Apply a 10-second timeout, circuit breaker, and polite fallback responses when the AI model is unreachable.
* **Zero Secret Leakage:** Never hardcode API keys or log personal identification data (CCCD, Passport, Bank card) to logs.

---

### STAGE 4: Quality Assurance & Evaluation (`m1-quality-and-git-delivery`)
Before preparing deliverables, inspect against the 3-tier checklist:
1. **Backend Verification:** Validate `@PreAuthorize`, `@Transactional`, proper `AppException` with `ErrorCode`.
2. **AI Evaluation:** Verify groundedness (zero invented prices), test model downtime fallback, verify PII masking.
3. **Definition of Done (DoD):**
   * Compilation: `mvn clean compile` succeeds with zero errors.
   * Tests: Unit tests cover Happy path, Validation failure, and Edge cases.
   * Cleanliness: Zero lint errors, zero unused imports, zero placeholder comments.

---

### STAGE 5: Git Standards & Handoff (`m1-quality-and-git-delivery`)
1. **Branch Naming Standard:**
   * Format: `feature/<task-code>-<short-description>`
   * Examples: `feature/T04-utility-clean`, `feature/T05-lodging-payroll`, `feature/T12-rag-chatbot`, `feature/U13-ocr-passport-parser`.
2. **Conventional Commits Standard:**
   * Format: `<type>(<scope>): <short description>`
   * Valid types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`.
   * Member 1 scopes: `(operation)`, `(lodging)`, `(payroll)`, `(ai)`, `(ocr)`, `(analytics)`.
   * Examples:
     * `feat(operation): implement CRUD APIs and soft delete for UtilityMeter`
     * `feat(ai): integrate PGVector and Spring AI for RAG Chatbot`
     * `feat(lodging): export stay guest declaration in BCA police format`
3. **Handoff:** Verify git diff and request user confirmation before pushing to remote or opening PR to Member 2 (Project Lead).
