# T12 RAG Gemini v1 — contract và baseline TASK-01

- Ngày: 04/11/2026, múi giờ báo cáo `Asia/Ho_Chi_Minh` (UTC+07).
- Trạng thái: **PROPOSED** cho phần Gemini; **CONFIRMED** chỉ cho hành vi đọc được từ source tại base dưới đây.
- Owner: Member 1. Shared dependency/config/security/DB: Member 2. Widget: Member 4.
- Phạm vi lượt này: hai tài liệu TASK-01; không triển khai TASK-02–10.
- Approval và điều kiện mở task: [t12-rag-gemini-approvals.md](t12-rag-gemini-approvals.md).

## 1. Baseline và bảo vệ công việc có sẵn

| Thuộc tính | Giá trị / trạng thái |
|---|---|
| Repository | `https://github.com/ThanhF-Xuaan/project-1-hotel-booking.git` — CONFIRMED qua `git remote -v` |
| Workspace | `D:/Project/University/project-1-hotel-booking` |
| Nhánh bắt đầu | `codex/integrate-existing-worktrees-20261104` |
| HEAD bắt đầu | `6236b78bf2d4e5711a740b2e0379819f7d8a8af6` |
| Develop local trước pull | `0b17efa8fed9eecbd073194f644b0c335c8e49c1` |
| Base sau pull / origin/develop đã fetch | `63a26ba4c81c92e9892383de29bf76bc96b617b9` |
| Nhánh TASK-01 | `codex/m1-t12-rag-gemini-contract-20261104` |
| HEAD khi tạo nhánh | `63a26ba4c81c92e9892383de29bf76bc96b617b9` |
| Dirty có sẵn | ` M tools/acceptance/Run-Application.ps1`; không có file untracked ở preflight |
| SHA-256 file dirty | `FB71D39311A3D6F531F31234552AA9E68588CB53FCE65C65CF8715DC2ED2CA90` |

**CONFIRMED:** user yêu cầu pull origin, tạo branch mới và thực hiện đúng TASK-01. Nhánh bắt đầu và origin/develop có 5/6 commit riêng sau fetch. Đã giữ nhánh bắt đầu nguyên trạng, chuyển sang develop và chạy `git pull --ff-only origin develop`, sau đó `git switch -c codex/m1-t12-rag-gemini-contract-20261104`. Không merge/rebase/reset/stash/clean, không tạo merge commit. File dirty được mang theo nguyên byte và kiểm SHA-256 trước/sau.

Base của tài liệu này là develop vừa pull, không phải HEAD khảo sát trong plan cũ. Pull đã cập nhật các file khác theo remote; các cập nhật đó thuộc base, không phải implementation TASK-01. Nhánh cũ vẫn giữ các commit riêng. Một SHA không đại diện cho dirty file: phải dùng cùng status và hash phía trên.

Allowlist TASK-01 chỉ gồm file này và file approvals. Không sửa launcher đang dirty, source, dependency, security, schema, corpus, eval hoặc frozen R1. Chưa stage/commit/push/PR/merge; chưa gọi model hoặc DB. Trước task tiếp theo phải xác minh lại HEAD/status/hash và base được duyệt; không giả định remote đứng yên.

## 2. Evidence map tại base — CONFIRMED từ source

Các đường dẫn bên dưới tương đối từ repository root để đối chiếu đúng Git tree; đây là evidence source, không phải tuyên bố runtime đã pass tại SHA mới.

| Evidence | Điều được xác nhận |
|---|---|
| `backend/pom.xml` | Java 21, Spring Boot 4.0.7; chưa có Spring AI dependency |
| `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/rag/controller/PublicRagKnowledgeController.java` | Hai alias ask; kiểm hotel eligibility, validation và generic 503 |
| `backend/src/main/java/vn/edu/utc/hotel_booking/common/config/SecurityConfig.java` | Public prefix permitAll; request khác cần authentication; bật method security |
| `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/rag/controller/AdminRagLifecycleController.java` | Lifecycle route riêng; annotation `hasRole('ROLE_CHAIN_ADMIN')` |
| `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/rag/dto/RagQueryRequestDto.java` | Input và Bean Validation như mục 3 |
| `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/rag/dto/RagQueryResultDto.java` và `RagCitationDto.java` | Output/citation fields như mục 3 |
| `backend/src/main/java/vn/edu/utc/hotel_booking/common/dto/ApiResponse.java` | Success code 200, message và result; null fields được bỏ khi serialize |
| `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/rag/embedding/HybridRagEmbeddingService.java` | Local hashing, 256 chiều, `local-multilingual-hashing-v1`; không phải Gemini embedding |
| `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/rag/service/ScopedRagRetrievalService.java` | Candidate scope/date/version trước scoring Java; threshold 0.30, topK tối đa 5 |
| `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/rag/service/RagGroundedAnswerService.java` | Guards, evidence/governance/time conflict và ghép nội dung; chưa sinh answer bằng Gemini |
| `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/rag/service/RagKnowledgeLifecycleService.java` | Governance, paragraph-v1, embedding và stage; activate/rollback/delete qua store |
| `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/rag/store/PostgresPgVectorStore.java` | Model/dimension pinned; candidate cap 1.000; parameterized RAG SQL và version lifecycle |
| `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/rag/config/RagVectorStoreConfiguration.java` | Pgvector opt-in, lazy, datasource riêng và URL loopback t12_pgvector_* |
| `backend/src/main/resources/db/sqlFile/incremental/2026/20261030-230000-create-rag-pgvector-store.sql` | Vector column không cố định dimension, kiểm vector_dims và FK model/dimension |
| `frontend/src/modules/booking-portal/services/hotelKnowledgeService.ts` | Public ask, zod validation, chặn citation khác hotel; timeout 15 giây |
| `frontend/src/modules/booking-portal/components/RoomAdvisor/HotelKnowledgePanel.tsx` | Hỏi đáp từng lượt; đổi hotel xóa kết quả và abort request; render text/citations |
| `tools/acceptance/Run-Application.ps1` | Source local được bảo vệ: launcher offline xóa key Gemini/Google và tắt provider R1 |

Service frontend tại base mới ưu tiên `VITE_API_BASE_URL`, fallback `VITE_API_URL`, bỏ suffix `/api`; không dùng mô tả base URL từ plan cũ làm oracle. Model, database và quyền của deployment thật vẫn **UNKNOWN**; không suy ra từ source hoặc báo cáo acceptance lịch sử.

## 3. Public và admin contract hiện tại — CONFIRMED từ source

### 3.1 Route, input và scope

- Canonical: `POST /api/v1/public/ai/room-advisor/knowledge/ask`, public theo security prefix.
- Alias: `POST /api/v1/ai/room-advisor/knowledge/ask`, cần authentication theo filter; không thêm anonymous access.
- Lifecycle: `POST /api/v1/ai/admin/knowledge/stage?version=1`, `/activate`, `/rollback`, `/delete`; giữ annotation role hiện có. Không đưa các thao tác này vào tool/model/widget.
- `hotelId`: Integer nullable, 1..32767 nếu có. Backend kiểm `PublicHotelEligibilityReader.isActivePublicHotel`; hotel không eligible trả 200/SCOPE_REQUIRED, không trả citation.
- `hotelCode`: nullable; nonblank bị 400, chưa có resolver. Không tự dùng text/role claim để xác lập scope.
- `question`: nonblank, tối đa 2.000 ký tự. `topK`: 1..5, mặc định/fallback 5.
- `language`: nullable; source hiện hỗ trợ nhánh `en`/heuristic, chưa có enum validation.
- `asOfDate`: nullable ISO LocalDate; thiếu thì service dùng `LocalDate.now()`. Không có date-policy/timezone delta trong TASK-01. Demo JVM dự kiến dùng `Asia/Ho_Chi_Minh`; UI hiện không gửi ngày này.

Null hotel chỉ truy xuất CHAIN_PUBLIC. Hotel được chọn chỉ lấy HOTEL_SCOPED cùng hotel cộng CHAIN_PUBLIC. Tenant/chain ID độc lập không có trong DTO hiện tại: bản này chỉ dùng sandbox một chuỗi, không tuyên bố hỗ trợ multi-chain isolation. Mở rộng nhiều chuỗi cần contract owner mới.

Retrieval giữ ACTIVE/SUPERSEDED trong inclusive effective window; chọn HOTEL_SCOPED trước CHAIN_PUBLIC cùng sourceId, rồi ACTIVE, effectiveFrom mới hơn, version cao hơn. Giữ tất cả chunk của version thắng. Không đổi mọi SUPERSEDED thành bị cấm vì sẽ phá lịch sử/future-effective replacement.

### 3.2 Output và lỗi

Success envelope là `{code:200,message,result}`. Result giữ `status`, `answer`, `citations`, `warnings`, `disclaimers`; các danh sách phải là array. Citation gồm `sourceId`, `version`, UUID `chunkId`, `category`, nullable `hotelId`/`effectiveFrom`, `snippet`, `similarityScore`. Score là similarity, không phải xác suất đúng.

| Tình huống hiện tại | HTTP / result |
|---|---|
| Đủ evidence hợp lệ | 200 / GROUNDED_ANSWER + citations |
| Thiếu evidence | 200 / INSUFFICIENT_EVIDENCE, không bịa nguồn |
| Thiếu scope theo guard hoặc hotel không eligible | 200 / SCOPE_REQUIRED |
| Guard ngoài phạm vi/PII/injection | 200 / OUT_OF_SCOPE; regex không chứng minh chặn mọi tấn công |
| Mâu thuẫn giờ nhận/trả phòng được phát hiện | 200 / CONFLICTING_SOURCES |
| DTO/IllegalArgumentException tại public controller | 400 / code 400, message, result có thể vắng |
| Exception lookup/retrieval/answer | 503 / code 503, message RAG_TEMP_UNAVAILABLE, result.status vẫn INSUFFICIENT_EVIDENCE theo legacy |

Enum có RAG_TEMP_UNAVAILABLE nhưng controller outage chưa dùng nó làm result.status. Consumer phải phân biệt bằng HTTP 503; TASK-01 không đổi enum/envelope. Security 401/403 thuộc global filter; không khẳng định đều dùng envelope local.

Ví dụ request **synthetic**, chỉ hợp lệ về hình dạng; hotelId=1 không khẳng định tồn tại/công khai trong môi trường bất kỳ:

```json
{"hotelId":1,"question":"Mấy giờ nhận phòng tại khách sạn synthetic?","language":"vi","asOfDate":"2026-11-04","topK":3}
```

Ví dụ thiếu evidence; nội dung message minh họa, không là exact-text oracle:

```json
{"code":200,"message":"Thành công","result":{"status":"INSUFFICIENT_EVIDENCE","answer":"Chưa có đủ tài liệu để xác nhận.","citations":[],"warnings":[],"disclaimers":[]}}
```

## 4. Delta Gemini v1 — PROPOSED, chưa triển khai

Mục tiêu sản phẩm đã được user chọn: RAG Gemini thật, hỏi đáp độc lập từng lượt, 3–5 ngày, synthetic được duyệt. Không thêm history/session, upload tài liệu, admin UI hoặc thay flow tìm phòng R1.

### 4.1 Provider, embeddings và grounding

- Đề xuất Spring AI BOM 2.0.2, modules `spring-ai-google-genai` và `spring-ai-google-genai-embedding`, giữ Boot 4.0.7. Bean riêng lazy/conditional; không bật provider chỉ vì có key. Member 2 phải duyệt dependency/config; compile/context compatibility chưa kiểm trong TASK-01.
- Chat candidate `gemini-3.5-flash-lite`; embedding candidate `gemini-embedding-001`, yêu cầu 768 chiều. Đây là model/dimension **PROPOSED**, không phải production default hay kết quả PoC. Model khả dụng trên tài khoản, output dimension/cardinality và hỗ trợ adapter là **UNKNOWN** cho đến PoC được phép.
- Key riêng `RAG_GEMINI_API_KEY`, chỉ backend. Flag provider mặc định tắt. Không tái dùng cờ/key để vô tình bật Gemini extraction của R1. Không tự đổi model/provider/Boot khi PoC lỗi.
- Thêm document/query operation tương thích vào RagEmbeddingPort: RETRIEVAL_DOCUMENT khi ingest, RETRIEVAL_QUERY khi hỏi. Kiểm vector count/dimension, finite/nonzero; L2-normalize trước store/scoring.
- Giữ paragraph-v1. Gemini giới hạn mỗi đoạn 2.000 ký tự và 64 chunk/version; reject trước outbound nếu quá giới hạn, không truncate. Local behavior không bị đổi ngầm. Stage chỉ sau khi toàn bộ embedding hợp lệ; không mở transaction DB suốt outbound.
- Không dùng threshold local 0.30 làm bằng chứng chất lượng Gemini. Dùng development set riêng chọn ngưỡng trong 0.30/0.40/0.50/0.60, cao nhất đạt recall >=90%; khóa trước acceptance. Không đạt thì sửa thiết kế bằng review, không tune acceptance/gold.
- Java giữ scope/date/version selection, topK <=5 và candidate cap 1.000. Không thêm Spring VectorStore/ANN thay thế lifecycle hiện có trong đợt này.
- Chuẩn hóa/che PII trước mọi outbound, gồm query embedding và chat. Không log/persist raw chat. Corpus và evidence đều qua governance; context tối đa 5 chunk/10.000 ký tự.
- Output nội bộ dự kiến: answer và references `{chunkId,supportingQuote}`. Java kiểm references là tập con của evidence, quote tồn tại, schema/finish-state hợp lệ; Java dựng citations, không dùng ID/URL/version do model bịa. Kiểm quote không thay thế đánh giá semantic groundedness.
- Không đủ nguồn, ngoài phạm vi, scope thiếu hoặc nguồn mâu thuẫn thì không gọi generator. Guard hiện tại không được dùng thay authorization và no-write architecture.

Tài liệu tham khảo để PoC đối chiếu, không phải chứng nhận deployment: [Spring AI compatibility](https://docs.spring.io/spring-ai/reference/getting-started.html), [Google GenAI chat](https://docs.spring.io/spring-ai/reference/api/chat/google-genai-chat.html), [Google GenAI embedding](https://docs.spring.io/spring-ai/reference/api/embeddings/google-genai-embeddings-text.html), [Gemini embedding](https://ai.google.dev/gemini-api/docs/embeddings), [chat candidate](https://ai.google.dev/gemini-api/docs/models/gemini-3.5-flash-lite).

### 4.2 Fallback, lỗi và compatibility

Giữ public request, result fields, alias/auth, citations và frozen R1. Không có route/role/schema mới được coi là đã duyệt.

| Delta đề xuất | Hành vi / gate |
|---|---|
| Generation timeout, refusal, JSON/citation sai, truncated output | Nếu đã có evidence hợp lệ: 200/GROUNDED_ANSWER, answer trích evidence, warning GENERATION_FALLBACK_EXTRACTIVE; user đã chọn fallback, API/UI delta vẫn cần owner review |
| DB hoặc query embedding outage | 503 theo legacy ở mục 3; không fallback hash, không tạo nguồn giả |
| Rate/concurrency hết hạn mức | HTTP 429, Retry-After số giây dương; code 429, generic message; không có result; Member 2 duyệt, Member 4 duyệt UI |
| Admin embedding provider outage | Đề xuất generic 503 riêng tại stage; validation vẫn 400; active version cũ nguyên vẹn; Member 2 review lỗi API |
| Generation fallback trên UI | Hiển thị “Đang hiển thị trích đoạn tài liệu do dịch vụ AI tạm gián đoạn.”; không hiển thị warning code như thông điệp cho khách |

Ví dụ **PROPOSED** fallback synthetic; UUID/facts chỉ là fixture minh họa, không phải nguồn thực đã ingest:

```json
{"code":200,"message":"Thành công","result":{"status":"GROUNDED_ANSWER","answer":"Khách sạn synthetic nhận phòng từ 14:00.","citations":[{"sourceId":"SYNTHETIC-CHECKIN","version":1,"chunkId":"8d952c36-d35e-4a4e-90e2-8c682c2f24c9","category":"POLICY","hotelId":1,"effectiveFrom":"2026-01-01","snippet":"Khách sạn synthetic nhận phòng từ 14:00.","similarityScore":0.8}],"warnings":["GENERATION_FALLBACK_EXTRACTIVE"],"disclaimers":["Dữ liệu giả lập phục vụ nghiệm thu kỹ thuật."]}}
```

Ví dụ **PROPOSED** HTTP 429 với header `Retry-After: 60` (giá trị minh họa, runtime phải tính theo limiter):

```json
{"code":429,"message":"Đã vượt giới hạn tra cứu. Vui lòng thử lại sau."}
```

### 4.3 Quota, chi phí và privacy

- **PROPOSED:** demo một JVM, tối đa 2 request ask đồng thời và 20 request/phút toàn instance. Không tuyên bố distributed fairness/production rate limit; không thêm Redis/session/cookie.
- **PROPOSED:** query embedding timeout 3 giây, generation 7 giây, tổng ask deadline 12 giây; max output 768 token. Không retry tự động. Slot chỉ release khi operation thực sự kết thúc; hủy provider request khi deadline hết, không tạo background leak. Frontend timeout hiện có 15 giây giữ nguyên.
- **CONFIRMED user preference, chưa là quyền chi tiền:** tối đa 5 USD cho đợt kiểm chứng. **PROPOSED:** tổng 200 embedding input và 120 generation call, tính cả PoC, ingestion, evaluation và lượt demo trong đợt; ledger phải tồn tại qua restart và chặn trước outbound.
- Trước live run phải duyệt provider/privacy/account/quota và cấp phép gọi live riêng. Khóa bảng giá tại run, dự toán upper bound cho input/output/thinking nếu tính phí trước call, ghi usage sau call; không biết giá/bound/usage thì dừng. Budget alert không phải hard cap. Không tắt giới hạn để đạt đủ case.
- Chỉ synthetic/public content được duyệt. Không guest/payroll/CCCD/passport/booking/live price/inventory, secret hoặc internal corpus. Không tự động gửi chat nhạy cảm chỉ vì đã có redaction regex; không cam kết regex xử lý mọi loại PII.
- Prompt/context là untrusted data. Không function calling, generic HTTP/SQL, URL fetching hoặc write-capable callbacks. RAG không hold/release, booking/payment/refund/guest write dù user xác nhận.
- Log tối thiểu requestId/model/prompt/corpus version, duration, usage và outcome; không raw question, document body, prompt, key, token hoặc provider exception body. Synthetic eval chỉ lưu nội dung đã được duyệt; runtime credential và ledger chi tiết không vào Git. Retention/provider processing terms còn chờ duyệt, không tự đặt kỳ hạn.

## 5. Sandbox, corpus, rollout và rollback — PROPOSED

1. Dùng database RAG **mới** `t12_pgvector_gemini_*` trên loopback, port chọn sau kiểm tra xung đột và owner approval. Giữ primary business datasource riêng. Không trộn vector Gemini với local hashing; store hiện pinned model/dimension.
2. Dùng schema incremental đã có sau Member 2 review target/migration/grants. Không sửa migration đã áp, không tự provision extension/quyền. DDL có FK hotels và trigger update_updated_at_column: provisioning phải kiểm prerequisite theo sandbox hiện có, không chạy riêng SQL rồi giả định đủ dependency.
3. Writer chỉ có quyền cần thiết trên RAG tables/sequences; không quyền ghi Booking/Pricing/Inventory/IAM. Bootstrap admin không được truyền vào JVM; kiểm privileges trước start. Không tự tạo production credential.
4. Trước provisioning/ingestion: xác minh container/database/port/process ownership, không có job khác, backup schema/data/sequence/config và manifest/hash. Diễn tập restore trong sandbox và so sánh dữ liệu/sequence/config; không tuyên bố đã snapshot/restore khi chưa chạy.
5. Corpus TASK-02 dự kiến 12 tài liệu cho 2 hotel synthetic và chain-public, có manifest source/scope/version/hash/effective window và replacement fixture. Hotel IDs phải lấy từ sandbox đã xác minh; không mặc định fixture hotel1 là dữ liệu thật. Chưa có corpus/eval mới được duyệt ở TASK-01.
6. Stage toàn bộ version, kiểm embedding/count/metadata rồi mới activate; lỗi replacement giữ active cũ. Không xóa active trước ingest. Delete/rollback theo scope+hotel+source+version; không theo filename hoặc model output.
7. Launcher Gemini opt-in riêng; giữ file launcher offline đang dirty nguyên trạng. Không bật demo initializer in-memory trong persistent mode, không seed/reset mỗi restart.
8. Rollback generation bằng tắt generation và dùng extractive answer từ corpus Gemini còn hợp lệ. Rollback embedding phải chọn lại cả store/corpus offline phù hợp, không chỉ đổi flag trên DB Gemini. Giữ DB/snapshot cũ, không DROP/clearAll hoặc xóa volume tự động.

## 6. Verification và handoff TASK-01

| Kiểm tra | Kết quả / ý nghĩa |
|---|---|
| `git fetch origin`, switch develop, `git pull --ff-only origin develop`, tạo branch mới | PASS; base 63a26ba4c81c92e9892383de29bf76bc96b617b9; không merge commit |
| Hash launcher sau pull/tạo branch | PASS khi SHA-256 khớp mục 1; bảo vệ dirty file |
| Review source/DTO/security tại base | PASS cho evidence map; không thay runtime verification |
| JSON fences, relative links, evidence paths, allowlist, whitespace | Kết quả cuối ghi tại receipt trong file approvals sau kiểm tra thực tế |
| Maven/frontend/browser/pgvector/live Gemini | NOT RUN trong TASK-01: chỉ tài liệu, không đổi executable code; không kế thừa PASS cũ thành PASS của base mới |

Acceptance TASK-01: hai file đúng allowlist; ví dụ JSON parse được; evidence path tồn tại tại base; mọi delta cross-owner có gate; không tự ký approval. Việc thiếu approval không chặn hoàn thành hai tài liệu, nhưng chặn implementation liên quan theo ma trận approvals. Xóa hai file mới là rollback tài liệu; không rollback base đã pull hoặc file dirty của user.
