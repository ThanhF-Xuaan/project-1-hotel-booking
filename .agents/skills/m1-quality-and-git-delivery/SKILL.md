---
name: m1-quality-and-git-delivery
description: Quality assurance, code review, AI evaluation, and Git delivery standards for Member 1 (AI & Data Specialist). Use this skill before committing code or opening a Pull Request to verify DoD, check for regressions, assess AI/Data accuracy, and generate standardized Conventional Commits.
---

# 🚀 M1 Quality & Git Delivery (QA, Code Review, Definition of Done & Git Standards)

This skill governs quality assurance, code review, AI/Data validation, and standardized Git delivery procedures for **Member 1 (AI & Data Specialist)**.

---

## 🎯 When to Activate This Skill

Activate upon completing feature implementation, prior to staging Git commits or submitting a Pull Request (PR) to Member 2 (Project Lead).

---

## 🔍 1. Self-Review Checklist

All code changes must pass this 3-tier inspection before delivery:

### A. Backend & Data Standards:
- [ ] **Soft Delete Enforcement:** All select/filter queries strictly include `is_deleted = false`.
- [ ] **Unified Batch Delete:** Delete operations use `DELETE /delete` taking a list of IDs rather than fragmented single-item endpoints.
- [ ] **Search DTO Pattern:** Multi-field search endpoints use `POST /filter` with `@RequestBody` search DTO.
- [ ] **RBAC Annotations:** Every controller method has explicit `@PreAuthorize("hasAnyRole(...)")`.
- [ ] **Transaction Demarcation:** All mutating methods (create, update, delete) have `@Transactional`; class is annotated with `@Transactional(readOnly = true)`.
- [ ] **No Silent Catch:** Custom `AppException` thrown with appropriate domain `ErrorCode`.

### B. AI & Data Integrity Standards:
- [ ] **Groundedness Verification:** Chatbot invokes registered tool calling for real-time rates and room inventory; does not hallucinate numbers.
- [ ] **Resilience & Fallback:** System gracefully handles LLM API downtime or quota exhaustion without crashing the server.
- [ ] **PII Protection:** No raw personal identification numbers (CCCD, Passport, Bank card) logged to console.
- [ ] **Credential Security:** Zero hardcoded API keys or secrets in source code or YAML files.

### C. Resource & Memory Protection:
- [ ] All `InputStream`, `OutputStream`, and `Reader` handles wrapped in `try-with-resources`.
- [ ] Large file operations (Excel/CSV/BCA) use streaming parsers rather than full in-memory buffering.

---

## 🏆 2. Definition of Done (DoD)

A task is strictly considered **DONE** only when satisfying 100% of the following criteria:
1. **Compilation:** `mvn clean compile` succeeds with zero errors.
2. **Lint & Cleanliness:** Zero unresolved compiler warnings, no unused imports, zero `// TODO` placeholders.
3. **Unit Tests:** Service and Controller unit tests execute successfully, covering Happy Path, Validation Failure, and Edge Cases.
4. **API Documentation:** Swagger / OpenAPI docs accurately reflect endpoints, request bodies, and response schemas.

---

## 🌿 3. Git Branching & Conventional Commits

Per the v4.0 project plan, Member 1 must follow these Git standards:

### A. Branch Naming Convention:
Format: `feature/<task-code>-<short-description>`
* T04: `feature/T04-utility-clean` or `feature/T04-master-data`
* T05: `feature/T05-lodging-payroll`
* T12: `feature/T12-rag-chatbot`
* U13: `feature/U13-ocr-passport-parser`
* U14: `feature/U14-guest-personalization`
* U22: `feature/U22-predictive-analytics`

### B. Conventional Commits Standard:
Commit messages must strictly follow the format:
```text
<type>(<scope>): <clear descriptive summary>

[Optional detailed body explaining context and rationale]
[Optional issue/task reference]
```

* **Allowed Types:**
  * `feat`: New feature or capability
  * `fix`: Bug fix
  * `refactor`: Code restructuring without functional change
  * `test`: Adding or updating test cases
  * `docs`: Documentation updates
  * `chore`: Build configuration or dependency maintenance

* **Standard Scopes for Member 1:**
  * `(operation)`: Utility meters and readings
  * `(lodging)`: Lodging queue and BCA police export
  * `(payroll)`: Payroll import pipeline
  * `(ai)`: RAG Chatbot, vector embeddings, prompt engineering
  * `(ocr)`: Identity document parser
  * `(analytics)`: Forecasting and anomaly detection

* **Commit Examples:**
  ```text
  feat(operation): implement CRUD APIs and soft delete for UtilityMeter
  feat(ai): integrate PGVector and Spring AI for RAG Chatbot
  feat(lodging): export stay guest report in police BCA format
  fix(utility): resolve zero-consumption division in anomaly calculation
  test(operation): add unit tests for UtilityMeterServiceImpl
  ```

---

## 📦 4. Delivery Workflow Execution

When completing a task, the agent assists the user by running or proposing:
1. Review changed files:
   ```bash
   git status -s
   ```
2. Stage and commit changes:
   ```bash
   git add <target_files>
   git commit -m "feat(<scope>): <description>"
   ```
3. Push to remote and prompt user for PR handoff to Member 2:
   ```bash
   git push origin <current_branch>
   ```
