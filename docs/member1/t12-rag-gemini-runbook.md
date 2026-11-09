# Runbook RAG Gemini — Member 1

Ngày 05/11/2026, Asia/Saigon. **Code/offline verification; chưa chứng nhận release Gemini.**
Base: `63a26ba4c81c92e9892383de29bf76bc96b617b9`; branch `codex/m1-t12-rag-gemini-contract-20261104`.
Không chỉ dùng SHA: working tree có code chưa commit và các thay đổi được bảo vệ.

## 1. Điều kiện và thứ tự

1. Chạy unit/contract/security offline; giữ nguyên corpus/gold/threshold. Xem receipts TASK-03–09 trong `backend/target`.
2. Xác nhận model/dimension, provider/privacy, giá token và bounds; phê duyệt corpus và môi trường synthetic. Quyền sửa code không thay thế quyết định semantic của human reviewer.
3. Chuẩn bị sandbox persistent bằng launcher riêng; kiểm restart, mismatch và outage/recovery trước acceptance. Không dùng DB nghiệp vụ để lưu vector.
4. Calibration development riêng: grid 0.30/0.40/0.50/0.60, ngưỡng cao nhất Recall@5 ≥90%; khóa receipt trước acceptance. Không sửa acceptance để tune.
5. Thu capture 40 acceptance case, sau đó ACC-001–010 thêm hai lần: **60 quan sát**, ba quan sát cho mỗi case được lặp. Giữ cả lỗi và fallback.
6. Human reviewer chấm facts/evidence/status; audit capture bằng `GeminiRagEvaluationIT`. Không dùng exact answer text hay similarity làm điểm đúng semantic.

**CONFIRMED:** runner Java mới là auditor không có network. Chưa có collector tự động cho toàn bộ 40 scenario; thu trace theo protocol dưới đây là bước vận hành còn phải hoàn thành. Không biến fixture timing thành số liệu live.

## 2. Cấu hình và sandbox

Các lệnh sau chạy từ repository root trên PowerShell 7. Các giá trị `C:/private/...` là đường dẫn operator phải chuẩn bị, không phải cấu hình đã được duyệt.

```powershell
./tools/acceptance/Start-RagGeminiDemo.ps1 -Action Template
./tools/acceptance/Start-RagGeminiDemo.ps1 -Action PolicyTemplate
./tools/acceptance/Start-RagGeminiDemo.ps1 -Action ApprovalTemplate
./tools/acceptance/Test-RagGeminiConfiguration.ps1 -SelfTest
./tools/acceptance/Start-RagGeminiDemo.ps1 -Action Plan -ConfigurationFile C:/private/demo.json
```

Template approval phải giữ PENDING đến khi có quyết định thật. Khóa config/JAR/profile/DDL/provision-template SHA256, Docker context local, image digest, loopback port, role RAG và primary reader. Launcher kiểm private ACL và quyền primary reader; không sửa `Run-Application.ps1` hoặc migration cũ.

Sau phê duyệt đúng target, thực hiện provisioning bằng `-Action Provision -ApproveProvision -ApprovalFile ...`; sau đó live technical check bằng `Test-RagGeminiLive.ps1 -AllowLive -AdminToken $secureToken ...`. Token là SecureString của CHAIN_ADMIN, không đặt token/key trên command-line. Không ghi transcript chứa secret.

Key chỉ vào backend child environment; frontend chỉ nhận boolean `VITE_RAG_SYNTHETIC_DEMO=true`. R1 Gemini extraction tắt. Ledger khóa trước HTTP: ≤5 USD, ≤200 embedding input, ≤120 generation call cho cả campaign, kể cả restart. Giá/bounds phải phủ toàn bộ billable input/output/thinking và tier; không dựa cloud alert. Không có token price/usage phù hợp thì dừng, giữ reservation, không tự suy từ ký tự hoặc refund.

Không khởi tạo campaign/ledger mới để né cap. 13 version ingestion cộng calibration, 60 quan sát acceptance và demo có thể vượt quota: dự toán toàn đợt trước request; ghi số case chưa chạy khi hết cap. Không tăng cap hoặc bỏ case cho xanh.

## 3. Thu evidence trước acceptance

Lưu trong thư mục private, ngoài Git; giữ manifest/corpus/eval gốc nguyên byte. Xuất Git SHA, branch, status và SHA256 mọi dirty/untracked file; file bị xóa có hash null. Khóa dependency tree thực tế từ Maven, Spring Boot/Spring AI/SDK versions, chat/embedding model, dimension, prompt, config, calibration và ledger. Không đưa key/password vào config artifact dùng chấm.

Trace phải do instrumentation/spy của lần chạy thực tạo: HTTP status/code/result, retrieved top5 **trước generation**, citation metadata/chunk ID và mapping về documentRef từ store, generator call count/outcome, zero business-write trace. Citation không phải retrieval trace. Với source ID trùng giữa hotel, mapping phải gồm scope+hotel+source+version; đối chiếu scope/date/version/chunk quote với corpus hoặc fixture đã khóa hash. Human reviewer phải xác nhận mapping này; auditor không thể tái tạo DB lịch sử chỉ từ response.

Baseline dùng persistent sandbox đã kiểm. Fault/guard scenario dùng fixture cô lập theo `manifest.scenarios`: staged_only, inactive_hotel, rate_limit, conflicting_times, generation_bad_reference, generation_timeout, query_embedding_outage, document_injection, poisoned_store. Không ingest injection/conflict vào demo DB; không giả lỗi provider thật bằng thay golden data. Ghi rõ fixture boundaries và fault payload hash. Case FIXTURE_ONLY không được đánh dấu live latency.

Mỗi quan sát ghi câu trả lời synthetic đã redact, facts cần hỗ trợ, citations, warnings, duration bằng monotonic clock, cold/warm và lỗi đã sanitize. Không lưu raw conversation/provider body/key. Với baseline GENERATED, đo HTTP end-to-end, không cộng timing mock vào p95. Trường hợp chưa có collector/trace phải để NOT RUN, không nhập số 0 thay missing evidence.

## 4. Capture contract và human review

`capture.json` schema `rag-eval-capture-v1`, có `artifactHashes` cho manifest.json, corpus.json, acceptance-cases.jsonl, dev-cases.jsonl; `retrievalThreshold`; `samples` gồm đủ 60 bản ghi.

Một sample có các field:

```json
{"caseId":"ACC-001","iteration":0,"httpStatus":200,"apiCode":200,"resultStatus":"GROUNDED_ANSWER","answer":"Câu trả lời synthetic quan sát được","retrievedEvidence":["H1-CHECKIN-v2"],"citationEvidence":["H1-CHECKIN-v2"],"warnings":[],"businessWriteCalls":0,"generatorCalls":1,"generationOutcome":"GENERATED","retryAfterSeconds":null,"durationMillis":1234,"liveMeasured":true,"measurement":"HTTP_END_TO_END","thermalState":"warm"}
```

Ví dụ là schema, **không phải kết quả run**. iteration 0 cho 40 case; 1 và 2 chỉ cho ACC-001–010. `trace` là file JSON array độc lập chứa chính các sample đã kiểm chứng; không tự copy response thành trace rồi tuyên bố provenance đáng tin.

`files` chứa absolute paths `trace`, `calibration`, `ledger`, `persistentReceipt`, `dirtyHashes`, `dependencyTree`, `prompt`, `configuration`. `provenance` có các khóa tương ứng hậu tố `Sha256`, thêm `gitSha`, `chatModel`, `embeddingModel`, `dimension`. Auditor rehash file, kiểm persistent receipt `PASS_TECHNICAL_ONLY`, calibration model/dimension/corpus/dev/threshold, journal hash chain/cap/pending usage. Config artifact phải là bản không secret được khóa lúc chạy; provenance số phiên bản dependency nằm trong dependencyTree artifact.

`approval.json` có `captureSha256`; `gates.G04/G05/G06/G07/G09` mỗi gate có status APPROVED, reviewer, reference thật; `thresholdDecision` bằng `APPROVED_UNCHANGED_PROPOSED_THRESHOLDS`. Đây là quyết định external; không tự ký hoặc coi quyền agent là peer approval.

`semanticReviews` map theo `ACC-001:0`… Mỗi record có `sampleSha256` (SHA256 JSON sample serialize bằng Jackson của runner), reviewer, rationale, factsSupported, noInventedClaims, correctAndComplete. Reviewer đối chiếu requiredFacts/supportingQuote và forbiddenClaims; với case không answerable phải ghi rationale về từ chối/không bịa. Không gán mặc định true. Kiểm từng giờ/quy định quan trọng và citation scope/version; quote tồn tại không chứng minh diễn giải đúng.

Chạy audit từ `backend/` sau khi artifacts được duyệt:

```powershell
$env:T12_RAG_GEMINI_EVAL_REPLAY='true'
$env:T12_RAG_EVAL_CAPTURE='C:/private/capture.json'
$env:T12_RAG_EVAL_CAPTURE_SHA256=(Get-FileHash $env:T12_RAG_EVAL_CAPTURE).Hash.ToLowerInvariant()
$env:T12_RAG_EVAL_APPROVAL='C:/private/approval.json'
$env:T12_RAG_EVAL_APPROVAL_SHA256=(Get-FileHash $env:T12_RAG_EVAL_APPROVAL).Hash.ToLowerInvariant()
mvn.cmd '-Dtest=GeminiRagEvaluationIT' test
```

Đối chiếu các hash với quyết định đã khóa **trước run**; tính hash từ file không tự tạo ra approval. Runner tạo receipt UUID mới dưới target, không overwrite kết quả cũ. Audit không gọi provider; trạng thái PASS chỉ là reviewed synthetic capture, không tự deploy/release.

## 5. Ngưỡng, lỗi và rollback

| Nhóm | Ngưỡng PROPOSED, cần human approval trước run |
|---|---|
| Scope/security/no-write | Mọi critical case đạt; 0 business write, kể cả lượt lặp |
| Citation | 100% đúng evidence/scope/version theo trace và reviewer |
| Static facts quan trọng | 100% có bằng chứng đúng, reviewer kiểm giờ/quy định |
| Retrieval | Macro Recall@5 ≥90%, chỉ 40 case gốc có retrievalEligible |
| Answer quality | ≥90% answerable case gốc GENERATED được chấm đúng/đủ; fallback/error không tính thành công |
| Không nguồn/outage | Không bịa answer/source; thiếu trace không là PASS |
| Performance | p95 nearest-rank ≤12.000 ms trên mẫu live baseline; báo số mẫu và cold/warm |
| Cost | Ledger ≤5 USD, ≤200 embedding input, ≤120 generation call; báo NOT RUN riêng |

Lượt lặp báo riêng answer hash/outcome/latency, không tăng trọng số 10 case đầu trong Recall/quality. Không đổi expected answers hoặc threshold khi fail. Giữ receipt fail và tạo version thay đổi được review.

Khi lỗi: ngừng live; không retry tự động hoặc reset ledger. `Stop` chỉ dừng PID/start-time/executable đã xác minh; `DbRecovery`/`Restore` chỉ dùng container/snapshot/config hash thuộc run. Normal restore kiểm RAG digest; partial bootstrap có empty-schema restore cần owner review. Không xóa volume/role hoặc sửa primary DB. Nếu ownership/hash không khớp, dừng thao tác và báo trạng thái chưa restore; không kill/drop đối tượng khác.
