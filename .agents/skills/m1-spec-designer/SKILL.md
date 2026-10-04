---
name: m1-spec-designer
description: Technical specification and requirement analysis skill for Member 1 (AI & Data Specialist). Use this skill when starting any new feature (T04, T05, T12, U13, U14, U22) to produce structured Technical Specs for either Data Submodules or AI/LLM Pipelines.
---

# 📐 M1 Spec Designer (Technical Specification & Architecture Analysis)

This skill provides standardized guidance for requirement analysis, contract design, and technical specification generation tailored for **Member 1 (AI & Data Specialist)** in the Hotel Booking System project.

---

## 🎯 When to Activate This Skill

Activate whenever initiating a new Feature or User Story owned by Member 1:
- **T04**: Utility & Master Data BE (`UtilityMeter`, `UtilityReading`, `Region`, `Property`, `RoomType`)
- **T05**: Lodging Queue & Payroll Import BE (`StayGuest`, BCA Police lodging declaration export, Payroll summary stream import)
- **T12**: RAG Chatbot AI Core (Spring AI / LangChain, PGVector, room booking consultation, hotel policies)
- **U13**: AI OCR Passport/CCCD Parser (Identity document extraction for StayGuest)
- **U14**: AI Guest Personalization Engine (Rate plan suggestions, personalized service recommendations based on stay history)
- **U22**: AI Predictive Analytics & Anomaly Detection (30-day occupancy forecasting, utility consumption anomalies)

---

## 🧭 Problem Classification Matrix

Before writing the specification, strictly classify the task into one of two tracks:

```text
                                [INCOMING FEATURE TASK]
                                           │
                 ┌─────────────────────────┴─────────────────────────┐
                 ▼                                                   ▼
       [TRACK A: DATA SUBMODULE]                           [TRACK B: AI & LLM CORE]
     (T04 Utility, T05 Payroll/Lodging)                (T12 Chatbot, U13 OCR, U14, U22)
                 │                                                   │
      - Focus: DDL & Liquibase, DTO contracts,            - Focus: Prompt Engineering, Vector Store,
        REST APIs, Batch/Stream I/O,                        Function Calling, JSON Structured Output,
        BCA Police XML/CSV, Payroll schemas                 Fallback, Latency & Token Economics
```

---

## 📝 Required Technical Spec Template

When executing this skill, generate the Technical Specification using the following standard template:

### 1. General Overview
* **Task ID & Name:** (e.g., `T12 - RAG Chatbot AI Core`)
* **Assignee:** Member 1 (AI & Data Specialist)
* **Core Objective:** Concise 1-2 sentence description of the business value.

---

### 2. Detailed Technical Design by Track

#### 🟢 For TRACK A (Data Submodule - T04, T05):
1. **Data Model (Entity & Database Schema):**
   * Target tables and column definitions matching PostgreSQL 16 standards.
   * Foreign keys (`hotel_id`, `guest_id`, etc.), unique constraints, indexes, check constraints.
   * Soft delete column (`is_deleted` boolean) and audit fields (`BaseEntity`: `created_at`, `updated_at`, `created_by`).
2. **REST API Contract:**
   * `POST /api/v1/<resource>/filter`: Search DTO (pagination `page`, `pageSize`, sort, exact match for enums/IDs, case-insensitive partial match for text).
   * `POST /api/v1/<resource>/create`: Create Request DTO with Jakarta Bean Validation (`@NotNull`, `@NotBlank`, `@Size`).
   * `PUT /api/v1/<resource>/update/{id}`: Update Request DTO.
   * `DELETE /api/v1/<resource>/delete`: Unified Batch Delete receiving `List<Integer> ids` (or `List<Long> ids`).
3. **Specialized File Schema (if applicable):**
   * *Payroll Import:* Column specs (Employee Code, Base Salary, Allowance, Workdays), error handling on invalid rows.
   * *BCA Police Declaration Export:* Specialized Vietnamese police report format for guest lodging registration (`StayGuest` attributes: CCCD/Passport, Full Name, DOB, Nationality, Room No, Check-in/Check-out timestamps).

---

#### 🟣 For TRACK B (AI & LLM Core - T12, U13, U14, U22):
1. **AI Framework & Model Selection:**
   * Framework: Spring AI (Java 21) or Python FastAPI microservice.
   * Target Models: Gemini 2.5/Flash, GPT-4o, or localized OCR Vision model.
2. **Knowledge Ingestion & Vector Store Strategy (RAG):**
   * Knowledge Base Sources: Room catalog, static pricing policies, cancellation terms, hotel amenities.
   * Chunking Strategy: 500-800 tokens per chunk with 100-token overlap.
   * Metadata Indexing: Must index `hotel_id` and `category` to allow pre-filtering before cosine similarity search.
   * Vector Store: PostgreSQL `pgvector` extension.
3. **Prompt Specification & Function Calling (Tool Calling):**
   * *System Prompt Template:* Hotel Virtual Concierge persona, strict boundaries against hallucination.
   * *Tool / Function Calling Definitions:* Register explicit tool definitions (e.g., `checkRoomAvailability`, `calculateRealtimePrice`) pointing to Core Backend APIs.
   * *Structured Output:* Enforce JSON Schema outputs for structured extraction.
4. **Resilience, Guardrails & Security:**
   * Fallback strategy when LLM times out (> 10s) or hits rate limits: Return friendly front-desk fallback message.
   * PII Protection: Mask sensitive personal data (CCCD, Passport, Payment cards) before logging or sending to third-party model providers.
   * Jailbreak / Prompt Injection defense.

---

### 3. Role-Based Access Control (RBAC) Matrix
Explicitly define role mappings for every endpoint:
| Endpoint | Method | Required Roles | Description |
| :--- | :--- | :--- | :--- |
| `/api/v1/utility/meters/filter` | POST | `ROLE_CHAIN_ADMIN`, `ROLE_PROPERTY_MANAGER`, `ROLE_RECEPTIONIST` | Search utility meters |
| `/api/v1/utility/meters/create` | POST | `ROLE_CHAIN_ADMIN`, `ROLE_PROPERTY_MANAGER` | Create new meter |
| `/api/v1/utility/meters/delete` | DELETE | `ROLE_CHAIN_ADMIN` | Soft delete meters in batch |
| `/api/v1/ai/chatbot/query` | POST | `ROLE_CUSTOMER`, `ROLE_RECEPTIONIST`, `ROLE_ANONYMOUS` | Consultation query |

---

### 4. Acceptance Criteria (AC)
Define at least 4-6 verifiable acceptance criteria:
- **AC1:** Valid payload returns HTTP 200/201 with standardized response wrapper.
- **AC2:** Duplicate code or constraint violation returns proper `AppException` with domain-specific `ErrorCode` (HTTP 400/409).
- **AC3:** AI Chatbot queries for unknown topics gracefully decline without fabricating facts or room prices.
- **AC4:** Delete operations strictly update `is_deleted = true` without physical deletion from PostgreSQL.
