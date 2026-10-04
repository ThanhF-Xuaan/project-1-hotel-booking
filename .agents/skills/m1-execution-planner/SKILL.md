---
name: m1-execution-planner
description: Execution plan generator and micro-step tracker for Member 1 (AI & Data Specialist). Use this skill after the Technical Spec is approved to break down the feature into an actionable checklist following the Cascade Workflow and Zero-Placeholder policy.
---

# 📋 M1 Execution Planner (Cascade Micro-Steps & Execution Tracker)

This skill translates an approved **Technical Specification** into a step-by-step **Execution Plan**, ensuring strict adherence to the **Cascade Workflow** and **Zero-Placeholder Policy**.

---

## 🎯 When to Activate This Skill

Activate immediately after the Technical Specification is approved by the user, before writing any implementation code.

---

## ⛓️ Mandatory Cascade Workflow Order

Every execution plan must strictly follow the architectural cascade without skipping layers:

### 1. Backend & Data Submodule Cascade (T04, T05):
```text
[1. Entity & Liquibase Migration]
              │
              ▼
    [2. DTOs & Search DTO]
              │
              ▼
    [3. MapStruct Mapper]
              │
              ▼
    [4. Repository Layer]
              │
              ▼
 [5. Service Interface & Impl]
              │
              ▼
[6. Controller Layer & OpenAPI]
              │
              ▼
  [7. Unit & Integration Tests]
```

### 2. AI & LLM Core Cascade (T12, U13, U14, U22):
```text
[1. Config & AI Provider Credentials]
                  │
                  ▼
[2. Vector Store & Document Ingestion]
                  │
                  ▼
[3. Tool / Function Calling Definitions]
                  │
                  ▼
 [4. System Prompts & Core AI Service]
                  │
                  ▼
[5. REST Gateway & Streaming Controller]
                  │
                  ▼
   [6. AI Evaluation & Unit Tests]
```

---

## 🚫 Zero-Placeholder Policy Enforcement

Every step in the generated plan must guarantee:
1. **No Incomplete Code:** Never generate files containing `// TODO: Implement logic later`, `// ... remaining code ...`, or dummy stub methods.
2. **Immediate Compilation:** Every generated file must have full imports, valid type signatures, and compile cleanly against Java 21 / Spring Boot 4.0.7.
3. **Strict Layer Ordering:** Lower layers (Entity, DTO, Repository) must compile successfully before initiating higher layers (Service, Controller).

---

## 📑 Actionable Execution Plan Template

When executing this skill, output the plan in Markdown Checklist format:

```markdown
# 🚀 Execution Plan: [FEATURE_NAME]

## 📌 Architecture Track: [Track A: Data Submodule / Track B: AI Core]
## 🎯 Target Git Branch: feature/[TASK_ID]-[short-description]

### Step 1: Database Schema & Entity Modeling
- [ ] Create DDL / Liquibase migration under `backend/src/main/resources/db/...` (if new tables/columns needed)
- [ ] Create Entity extending `BaseEntity`, configure `@ManyToOne`, `@Enumerated(EnumType.STRING)`
- [ ] Add domain-specific error codes in `vn.edu.utc.hotel_booking.common.exception.ErrorCode`

### Step 2: DTO Contracts & Mapping Layer
- [ ] Create `<Resource>CreateRequest.java` with validation constraints (`@NotBlank`, `@NotNull`, `@Min`)
- [ ] Create `<Resource>UpdateRequest.java`
- [ ] Create `<Resource>SearchDto.java` supporting pagination (`page`, `pageSize`, `keyword`)
- [ ] Create `<Resource>Response.java` (strictly exclude sensitive internal columns)
- [ ] Create MapStruct `<Resource>Mapper.java` with `componentModel = "spring"`

### Step 3: Persistence Layer (Repository)
- [ ] Create `<Resource>Repository.java` extending `JpaRepository`
- [ ] Implement search query filtering by criteria with `isDeleted = false`
- [ ] Implement uniqueness check query: `existsBy...AndIsDeletedFalse(...)`

### Step 4: Business Logic Layer (Service)
- [ ] Create interface `<Resource>Service.java` defining business methods
- [ ] Implement `<Resource>ServiceImpl.java`:
  - [ ] Class-level annotations: `@Service`, `@RequiredArgsConstructor`, `@Transactional(readOnly = true)`
  - [ ] Implement `search(SearchDto)` returning `PageResponse<Response>`
  - [ ] Implement `getById(id)` with not-found exception
  - [ ] Implement `create(CreateRequest)` with `@Transactional`
  - [ ] Implement `update(id, UpdateRequest)` with `@Transactional`
  - [ ] Implement unified `delete(List<Id> ids)` (Soft Delete & Batch Delete) with `@Transactional`

### Step 5: Web & Security Layer (Controller)
- [ ] Create `<Resource>Controller.java`:
  - [ ] Annotations: `@RestController`, `@RequestMapping("/api/v1/<resource>")`, `@Tag(...)`
  - [ ] RBAC enforcement: `@PreAuthorize("hasAnyRole('ROLE_...')")` on each endpoint
  - [ ] OpenAPI documentation: `@Operation`, `@ApiResponse`
  - [ ] Endpoints: `POST /filter`, `POST /create`, `PUT /update/{id}`, `DELETE /delete`

### Step 6: Testing & Verification Layer
- [ ] Create `<Resource>ServiceTest.java` (JUnit 5 + Mockito):
  - [ ] Happy Path: valid input returns expected DTO
  - [ ] Validation / Duplicate Path: throws domain `AppException`
  - [ ] Edge Cases: empty list, soft delete flag verification

### Step 7: Delivery & Definition of Done Review
- [ ] Execute `mvn clean compile` or `mvn test` to verify zero errors
- [ ] Verify checklist against project Definition of Done (DoD)
- [ ] Package Git Commit following Conventional Commits format
```

---

## ⚡ Execution Protocol

1. Present the completed plan to the user.
2. Await user confirmation before proceeding with implementation.
3. Update checkboxes `[x]` incrementally as each micro-step completes.
