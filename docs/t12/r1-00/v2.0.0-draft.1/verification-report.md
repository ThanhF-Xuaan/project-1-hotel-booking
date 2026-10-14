# Báo cáo verification và handoff — R1-00

**Kết quả:** bộ draft R1-00 sẵn sàng review; chưa Go cho integration/release. Worktree `D:\Project\University\worktrees\hotel-t12-room-advisor`, branch `feature/T12-ai-room-advisor-v2`, base/HEAD `afbac57ae463ee2dc8705a2b3e9a7a8b5fca85bb`.

Contract artifact `2.0.0-draft.1`, wire `2.0`, evaluation `0.1.0-draft`. DTO là contract specifications và JSON Schema, chưa có runtime Java DTO/controller/engine. Không Spring AI dependency hoặc live Gemini call.

## Những thay đổi sau review file mới

Đã thêm provenance/ambiguity cho typed criteria, stay trên cards, CR mapping trong approval register và 18 draft evaluation cases. Tổng 74 cases map 35 yêu cầu Release 1 của draft evaluation v2. 56 expected assertions ban đầu giữ nguyên; 23 file governance/skills/ADR/tracker/evaluation của người dùng giữ nguyên SHA256. Tài liệu chi tiết: [governance alignment](governance-alignment.md).

## Verification

| Check | Kết quả | Evidence/giới hạn |
|---|---|---|
| Repo/remote/base preflight | PASS | Remote Hotel Booking; fetch + ls-remote xác nhận origin/develop=HEAD SHA trên; kiểm tra lại remote trước handoff. |
| Source contract audit | PASS | [Source evidence](baseline-source-evidence.json): 21 file tồn tại, không diff so với HEAD; không coi proposed endpoint/route là API thực tế. |
| JSON Schema + references + OpenAPI | PASS | 3 Draft 2020-12 schemas; 52 refs; 1 OpenAPI 3.1 document, validate bundle trong memory, không resolve qua network. |
| Synthetic fixture/response invariants | PASS | 26 native price/availability + normalized quote triplets; 6 response examples; counts, IDs, stay, fee UNKNOWN và card actions. |
| Dataset/reference validation | PASS | 74 cases, 69 patch targets, 35 R1 requirement mappings; không chạy expected assertions trên engine. |
| Negative artifact cases | PASS | 33 trường hợp rejected: owner/role/scope giả, action/link cấm, duplicate IDs, multi-room cards, hard-budget chưa xác minh, stay/provenance/coverage sai. |
| User-file preservation | PASS | [Protected snapshot](protected-user-files.json): 23 SHA256 khớp; legacy eval và v2 draft nguyên vẹn. |
| Vietnamese documentation lint | PASS với warnings | 9 Markdown docs; 0 errors. NUM004 hiểu identifiers Member 2 như đơn vị diện tích; giữ nguyên contract IDs. Các cảnh báo superlative đã sửa bằng mô tả đếm được. |
| Whitespace/scope | PASS | `git diff --check`; scan cả file untracked của R1-00; backend source/pom, Frontend và Compose không diff; index không staged changes ở worktree này. |
| Live Core OpenAPI/integration | NOT RUN | localhost API docs không khả dụng; không start backend/Docker/DB. |
| Engine/AI evaluation/security traces | NOT RUN | Chưa có engine/runner và owner-approved contracts; không có runtime no-write/PII/tenant proof hoặc accuracy/relevance/latency/token/cost metrics. |
| Maven/Frontend build | NOT RUN | Không sửa runtime source/dependencies; proportional checks là artifact validation. |

Command chính đã chạy từ worktree:

```powershell
& "$env:TEMP\t12-r100-validation-venv\Scripts\python.exe" -X utf8 -B tools/t12/validate_r1_contracts.py --json-report "$env:TEMP\t12-r100-validation-report.json"
git diff --check
git diff --name-only -- backend/src/main backend/pom.xml frontend docker-compose.yml
git diff --cached --name-only
git ls-remote origin refs/heads/develop
```

Vietnamese lint chạy `validate_copy.py <file> --register eng-impersonal` trong skill vietnamese-tech-writing cho 9 Markdown docs của task. Python validation libraries cài vào venv TEMP, không đổi dependencies Backend/Frontend. [Kết quả machine-readable](verification-results.json) chứa counts, categories và digest dataset.

Lần validation đầu trước khi file báo cáo tồn tại đã FAIL ở Markdown reference `verification-report.md`; reference đã được hoàn thiện và chạy lại PASS. Không sửa golden answers hoặc nới schema để bỏ qua lỗi.

## Danh sách toàn bộ file của R1-00

43 file mới trong bốn cây task. Những thay đổi tracked tại governance và docs của người dùng vẫn hiện trong `git diff --stat`, nhưng không thuộc change set bên dưới và không được task này sửa. Các file TEMP authoring/venv không nằm trong repository.

| File tương đối với root worktree | Change |
|---|---|
| `backend/src/test/resources/contracts/t12/r1/v2.0.0-draft.1/README.md` | NEW — R1-00 |
| `backend/src/test/resources/contracts/t12/r1/v2.0.0-draft.1/availability-request-native.json` | NEW — R1-00 |
| `backend/src/test/resources/contracts/t12/r1/v2.0.0-draft.1/availability-responses-native.json` | NEW — R1-00 |
| `backend/src/test/resources/contracts/t12/r1/v2.0.0-draft.1/detail-proposed.json` | NEW — R1-00 |
| `backend/src/test/resources/contracts/t12/r1/v2.0.0-draft.1/discovery-pages-proposed.json` | NEW — R1-00 |
| `backend/src/test/resources/contracts/t12/r1/v2.0.0-draft.1/discovery-request-proposed.json` | NEW — R1-00 |
| `backend/src/test/resources/contracts/t12/r1/v2.0.0-draft.1/manifest.json` | NEW — R1-00 |
| `backend/src/test/resources/contracts/t12/r1/v2.0.0-draft.1/pricing-request-native.json` | NEW — R1-00 |
| `backend/src/test/resources/contracts/t12/r1/v2.0.0-draft.1/pricing-responses-native.json` | NEW — R1-00 |
| `backend/src/test/resources/contracts/t12/r1/v2.0.0-draft.1/quotes-normalized-proposed.json` | NEW — R1-00 |
| `backend/src/test/resources/contracts/t12/r1/v2.0.0-draft.1/transport-failures.json` | NEW — R1-00 |
| `docs/t12/r1-00/v2.0.0-draft.1/README.md` | NEW — R1-00 |
| `docs/t12/r1-00/v2.0.0-draft.1/baseline-audit.md` | NEW — R1-00 |
| `docs/t12/r1-00/v2.0.0-draft.1/baseline-source-evidence.json` | NEW — R1-00 |
| `docs/t12/r1-00/v2.0.0-draft.1/decisions-and-approvals.json` | NEW — R1-00 |
| `docs/t12/r1-00/v2.0.0-draft.1/dto-contracts.md` | NEW — R1-00 |
| `docs/t12/r1-00/v2.0.0-draft.1/examples/chat-request.json` | NEW — R1-00 |
| `docs/t12/r1-00/v2.0.0-draft.1/examples/error-response.json` | NEW — R1-00 |
| `docs/t12/r1-00/v2.0.0-draft.1/examples/extraction-proposal.json` | NEW — R1-00 |
| `docs/t12/r1-00/v2.0.0-draft.1/examples/incomplete-response.json` | NEW — R1-00 |
| `docs/t12/r1-00/v2.0.0-draft.1/examples/multi-room-response.json` | NEW — R1-00 |
| `docs/t12/r1-00/v2.0.0-draft.1/examples/no-match-response.json` | NEW — R1-00 |
| `docs/t12/r1-00/v2.0.0-draft.1/examples/out-of-scope-response.json` | NEW — R1-00 |
| `docs/t12/r1-00/v2.0.0-draft.1/examples/refresh-request.json` | NEW — R1-00 |
| `docs/t12/r1-00/v2.0.0-draft.1/examples/results-response.json` | NEW — R1-00 |
| `docs/t12/r1-00/v2.0.0-draft.1/examples/search-request.json` | NEW — R1-00 |
| `docs/t12/r1-00/v2.0.0-draft.1/examples/unavailable-response.json` | NEW — R1-00 |
| `docs/t12/r1-00/v2.0.0-draft.1/governance-alignment.md` | NEW — R1-00 |
| `docs/t12/r1-00/v2.0.0-draft.1/member2-backend-review.md` | NEW — R1-00 |
| `docs/t12/r1-00/v2.0.0-draft.1/member4-frontend-review.md` | NEW — R1-00 |
| `docs/t12/r1-00/v2.0.0-draft.1/openapi.json` | NEW — R1-00 |
| `docs/t12/r1-00/v2.0.0-draft.1/protected-user-files.json` | NEW — R1-00 |
| `docs/t12/r1-00/v2.0.0-draft.1/schemas/advisor.schema.json` | NEW — R1-00 |
| `docs/t12/r1-00/v2.0.0-draft.1/schemas/core.schema.json` | NEW — R1-00 |
| `docs/t12/r1-00/v2.0.0-draft.1/verification-report.md` | NEW — R1-00 |
| `docs/t12/r1-00/v2.0.0-draft.1/verification-results.json` | NEW — R1-00 |
| `eval/t12/r1/v0.1.0-draft/README.md` | NEW — R1-00 |
| `eval/t12/r1/v0.1.0-draft/cases.json` | NEW — R1-00 |
| `eval/t12/r1/v0.1.0-draft/evaluation.schema.json` | NEW — R1-00 |
| `eval/t12/r1/v0.1.0-draft/governance-coverage.json` | NEW — R1-00 |
| `eval/t12/r1/v0.1.0-draft/manifest.json` | NEW — R1-00 |
| `tools/t12/requirements-contract-validation.txt` | NEW — R1-00 |
| `tools/t12/validate_r1_contracts.py` | NEW — R1-00 |

## Blockers, risks và approval

[Member 2 review](member2-backend-review.md): public discovery/detail, date/capacity, currency/VAT/service fee/coverage, scope/session/CSRF/TTL và error semantics vẫn PENDING. [Member 4 review](member4-frontend-review.md): detail route, card labels, revalidation/stale UX và credential/version handling vẫn PENDING. Numeric evaluation thresholds/ranking oracle cần human review; dataset chưa frozen.

Fixtures chỉ synthetic test resources. VND và detail path là proposals; chưa được dùng như facts/links production. Không chứng nhận all-in hoặc hard budget khi currency/fee coverage chưa rõ. Privacy/provider/retention runtime controls chưa verified. Rollback chỉ loại đúng file R1-00 khỏi change set khi được cấp phép; không reset/clean toàn worktree hoặc xoá file người dùng.

## Git và điểm dừng

Task chỉ fetch và tạo worktree/branch theo quyền đã cấp trước đó; không reset, stash, rebase, stage, commit, push, PR, merge, migration hoặc business write. Các thay đổi governance được người dùng xác nhận; nhánh/HEAD của worktree không thay đổi. Không gửi thông báo tới Member 2/4.

**STOP:** chờ người dùng review R1-00. Chưa bắt đầu R1-01 hoặc R1-02; bản bàn giao không tự cấp quyền integration hoặc release.
