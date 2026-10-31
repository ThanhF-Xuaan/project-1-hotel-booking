# Member 1 — Review và bàn giao GitHub, 31/10/2026

**CONFIRMED — review kỹ thuật PASS sau fix.** User đã ủy quyền rõ implement → review → fix/refactor → review PASS → commit/push → PR → merge develop. Branch `codex/m1-feature-completion-20261031`, base `20e06e5135def269b91531b3997b784b271e17c8`. Không giả human peer approval; trạng thái commit/push/merge thực tế được ghi trong PR và receipt sau merge. Báo cáo này bổ sung trạng thái Git cho các report local trước đó, không viết lại lịch sử.

## Phạm vi tích hợp

Tích hợp103file đã nghiệm thu local: Gemini extraction opt-in; pgvector datasource/lifecycle riêng; history/forecast và utility anomaly; Group preview chỉ đọc; SAFE/PERF/V02 và runners. Delta cuối bổ sung regression RAG, parser policy-time thống nhất, Unicode/credential guard và bảo toàn byte fixture trên Windows. U22/Group không được wire bằng mock hoặc bật API business khi thiếu nguồn thật. Không thay Core booking/pricing/inventory/auth hoặc mặc định production.

## Review → fix → review

| Finding trong phạm vi | Baseline | Sửa và trạng thái cuối |
|---|---|---|
| GITREV-F001 — giờ policy và chủ đề | HHhmm không được so mâu thuẫn/normalize cùng HH:mm; giờ không hợp lệ hoặc giờ checkout bị dùng cho check-in | CLOSED: parser chung theo label/newline, validate hour/minute và boundary, normalize HH:mm; conflict fail-closed |
| GITREV-F002 — Unicode/credential | Guard tiếng Việt viết hoa, PII/secret/indirect injection có thể lọt; word-boundary sai sau dấu credential assignment | CLOSED: Unicode-aware case/character classes và credential assignment pattern riêng, giữ reject/sanitize ở application layer |
| GITREV-F003 — fixture checkout | Git core.autocrlf=true làm sai SHA của3fixture U22/Group | CLOSED:2.gitattributes chỉ định đúng3file -text; repro checkout3/3sai trước fix và3/3đúng sau fix, byte/gold không đổi |

Test mới `RagReviewRegressionTest`:14case,13FAIL/0ERROR trước sửa; sau fix toàn related RAG97/97PASS. Không sửa32case frozen, thresholds hoặc assertions cũ. Scope/citation/date/topK, private RAG datasource, no-business-write, authorization trước/sau read và deterministic oracle tiếp tục được kiểm. Rà soát diff không thêm live provider, callback ghi nghiệp vụ, generic SQL/URL executor hoặc PII thật.

## Bằng chứng trên mã cuối

| Lệnh/nhóm | Kết quả |
|---|---|
| Maven clean test và context smoke riêng do Verify gọi | 1.399/1.399,0skip trong backend.json tổng hợp |
| U14 PostgreSQL opt-in trên2DBtrống riêng | 6/6PASS, database tạm xóa sau test |
| Pgvector opt-in riêng | 11/11PASS, backup hash/data-sequence restore và dừng container |
| Surefire aggregate của các lệnh riêng | 1416test, 179suite;0failure/error/skip; không gọi là một lệnh Maven |
| Related RAG sau fix | 97/97PASS, gồm14new và32frozen |
| Frontend lint/build | PASS,0error/warning |
| Playwright API/browser | 25/25 và27/27PASS,0skip |
| Benchmark hai vòng, concurrency1/5 | 1.200measured+240warm-up, 0error; mọi cell đạt p95<2s/success>=99% |
| p95 cao nhất từng endpoint | Advisor 109.16ms; FAQ 10.57ms; OCR 1186.96ms |
| Verify supervisor | PASS; snapshot restored, owned app/container đã dừng |

Run `20261031-203018-616-8cf95b7e`. Lệnh: `.\tools\acceptance\Verify.ps1 -IncludeM1Benchmark -IncludeM1AdditionalTests`; pgvector `.\mvnw.cmd -Dtest=PostgresPgVectorStoreIT -DfailIfNoTests=true test`; RAG `.\mvnw.cmd '-Dtest=RagReviewRegressionTest,RagPolicyTimeEvidenceTest,RagSafetyEvaluationSuiteTest,*Rag*,DefaultPublicHotelCorpusInitializerTest,!PostgresPgVectorStoreIT' test`. Hash Java được khóa trước Verify và đối chiếu sau. Gold32case SHA `df744d01fabd19923ef53faa2ed16e2de3577093f62817bbe3ca6726b7a245bc`.

Chỉ synthetic sandbox `m1-final-20261031-*`, đã kiểm ports/volume, snapshot trước migration; không production hoặc dữ liệu khách thật. Pgvector test dùng container riêng và restore riêng. Main Boot HTTP dùng in-memory RAG; không báo persistent HTTP hoặc Gemini thật đã test. Benchmark giữ rate limiter, cold anonymous sessions và pacing thật, không retry/loại lỗi; không phải200CCU hoặc5CCUbão hòa. OCR giữ engine/quality/deadline/max2process và human verification. Runtime thực JDK21.0.12; Node PATH java probe25 là probe khác được phân biệt trong evidence.

## Reviewability và giới hạn còn giữ

Source của worktree cũ/repoD được bảo vệ, không reset/clean/stash/rebase/force-push. Index chỉ nhận allowlist của task. Repo chưa có workflow GitHub Actions trong tree tại thời điểm kiểm; kết quả local không được gọi CI PASS. Trước merge phải kiểm actual PR head/base/checks/reviews và dùng expected head SHA, không bypass rule.

V03 business integration và format vẫn NOT READY: historical daily capacity/actual stays/completeness; utility physical unit/reading-day/reset/replacement epoch; Group physical beds/floors/all-night quotas/availability/IAM; mẫu BCA chính thức. Quyền user đã đủ cho thay đổi code, nhưng không tạo bằng chứng dữ liệu/format ngoài hệ thống. Không gỡ gate bằng dữ liệu giả. OCR-02 giữ COMPLETE/MERGED trước đó; không mở lại từ giới hạn production. Không tuyên bố toàn SRS hoặc production hoàn tất.

Evidence: C:/Users/admin/.codex/outputs/member1-github-delivery-2026-10-31/ và tools/acceptance/.runtime/runs/20261031-203018-616-8cf95b7e/. Giữ review-baseline.log với13FAIL, review-fix.log, fixture-eol-verification.json và logs cuối. Snapshot/runtime/token không commit. Rollback: revert commit qua PR theo quy trình team, giữ provider/PG opt-in tắt; không reset dirty work hoặc rollback DB không được phê duyệt.
