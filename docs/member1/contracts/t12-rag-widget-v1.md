# T12 RAG widget v1 — contract delta và test oracle

- Trạng thái: **C01 historical baseline; T12-01 local implementation theo owner decision 29/10/2026.** Xem mục7 cho contract hiện hành.
- Baseline **CONFIRMED**: `5509af2e26daca6dd67a32acb987a86648808df2`, ngày 2026-10-28.
- Owner: Member 1 API; Member 4 widget/navigation; Member 2 Core/security/DB. Data mode: **synthetic-only**, offline cho nghiệm thu local.
- C01 không sửa frozen R1 OpenAPI/examples/gold, U14 contract hoặc frontend. FAQ không bổ sung vào `AdvisorResultDto.sources` bằng một thay đổi ngầm.

## 1. Bằng chứng tại baseline

| Source từ repository root | Contract đối chiếu |
|---|---|
| `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/advisor/controller/PublicRoomAdvisorController.java` | Chat/search/refresh, owner, rate limit và error handlers |
| `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/advisor/dto/AdvisorResultDto.java` | Response R1, knowledgeStatus DISABLED và sources rỗng mặc định |
| `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/advisor/error/RoomAdvisorErrorResponse.java` | Frozen advisor error envelope |
| `docs/t12/r1-00/v2.0.0-draft.1/openapi.json` và `schemas/advisor.schema.json` | Chỉ ba route R1; không có knowledge/ask |
| `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/rag/controller/PublicRagKnowledgeController.java` | Hai alias /knowledge/ask, lỗi 400/503 |
| `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/rag/dto/RagQueryRequestDto.java` và `RagQueryResultDto.java` | DTO FAQ riêng |
| `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/rag/dto/RagCitationDto.java` | Citation source/version/chunk/scope metadata |
| `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/rag/service/ScopedRagRetrievalService.java` | Scope/date/version precedence; topK và threshold |
| `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/rag/store/InMemoryScopedVectorStore.java` | Store memory; chọn ACTIVE/SUPERSEDED trong date window |
| `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/rag/service/RagGroundedAnswerService.java` | Guardrails, scope-required, grounded/insufficient response |
| `backend/src/main/java/vn/edu/utc/hotel_booking/common/config/SecurityConfig.java` | Public prefix permitAll; alias non-public cần JWT |
| `frontend/src/modules/booking-portal/services/roomAdvisorService.ts` | Axios credentials; chỉ chat/refresh helpers, chưa knowledge helper |
| `frontend/src/modules/booking-portal/components/RoomAdvisor/RoomAdvisorWidget.tsx` | Session/version chat/refresh; chưa tích hợp FAQ |

## 2. R1 hiện tại — CONFIRMED, giữ nguyên

| POST route | Request |
|---|---|
| `/api/v1/public/ai/room-advisor/chat` | `contractVersion="2.0"`, `message`, optional `sessionId`, `expectedSessionVersion`, `criteriaPatch` |
| `/api/v1/public/ai/room-advisor/search` | `contractVersion="2.0"`, `criteria`, optional `sessionId`, `expectedSessionVersion` |
| `/api/v1/public/ai/room-advisor/refresh` | `contractVersion="2.0"`, `sessionId`, `expectedSessionVersion` theo frozen schema |

Ràng buộc chi tiết dùng frozen schema/example gốc: `docs/t12/r1-00/v2.0.0-draft.1/examples/chat-request.json`, `search-request.json`, `refresh-request.json`, `results-response.json`, `error-response.json`. Không coi field Java thiếu annotation là đã bỏ schema/service validation.

Success: `ApiResponse<AdvisorResultDto>` = `code=200`, `message`, `result`. Result có contractVersion/requestId/sessionId/sessionVersion/status/reply/criteria, missingFields/recommendations/coverage/knowledgeStatus/sources/warnings/criteriaProvenance/ambiguities. R1 status: RESULTS, NEED_INFO, NO_MATCH, INCOMPLETE_SEARCH, TEMP_UNAVAILABLE, OUT_OF_SCOPE. Card có room/price/reasons/action/stay; chỉ `VIEW_ROOM_DETAILS` dùng path đã được Core/navigation xác minh. Không tính lại quote; một phòng/option; nhiều phòng cần làm rõ. Outage không được đổi thành NO_MATCH.

R1 errors dùng `{code,message,error:{contractVersion,requestId,reason,retryable}}`, không có result/data. HTTP 400 INVALID_REQUEST, 403 SCOPE_DENIED, 404 SESSION_NOT_FOUND (missing/expired/foreign cùng response), 409 SESSION_VERSION_CONFLICT, 429 RATE_LIMITED + Retry-After, 503 UPSTREAM_UNAVAILABLE. AUTH_REQUIRED là reason schema/security; không suy diễn toàn bộ OAuth filter dùng local envelope.

Owner do `RoomAdvisorPrincipalExtractor` lấy từ validated JWT sub hoặc signed cookie, không từ body/header role claim. Cookie `t12_session`: HttpOnly/Secure/SameSite=Lax/path `/api/v1/public/ai/room-advisor`, TTL tối đa 30 phút; cookie chỉ phát sau orchestration thành công. SessionId không thay auth. Không thay cookie/CORS/TTL/Redis trong C01. Axios hiện `withCredentials=true`, timeout 45000 ms; không coi 45 giây là latency SLA hoặc backend timeout.

## 3. FAQ hiện tại — CONFIRMED từ source, runtime chưa chạy trong C01

Canonical public route: `POST /api/v1/public/ai/room-advisor/knowledge/ask`. Alias `POST /api/v1/ai/room-advisor/knowledge/ask` có cùng controller nhưng cần JWT theo global filter. Widget dùng public route sau G2; không chuyển chat sang alias này.

| Input field | Kiểu / validation hiện có |
|---|---|
| hotelId | Integer, nullable; chưa có @Min, không tự chứng minh hotel public/active |
| hotelCode | String, nullable; chưa được retrieval dùng để resolve hotelId |
| question | String @NotBlank, @Size(max=2000) |
| language | String nullable; `en` hoặc nhận diện câu tiếng Anh ảnh hưởng trả lời |
| asOfDate | LocalDate ISO, nullable; mặc định LocalDate.now() tại service, chưa khoá timezone/request-date policy |
| topK | Integer @Min(1); DTO builder default 5; retrieval fallback 5 nếu null; chưa có upper bound |

Response result: `{status,answer,citations,warnings,disclaimers}`. Citation: `sourceId`, `version`, `chunkId` UUID, `category`, nullable `hotelId`/`effectiveFrom`, `snippet`, `similarityScore`. Không có booking/guest/payment identifiers. Similarity là retrieval score, không phải confidence xác suất hoặc giá/tồn.

| Tình huống | HTTP / expected result tại baseline |
|---|---|
| Có evidence scoped phù hợp | 200 GROUNDED_ANSWER, citations |
| Không tìm thấy evidence | 200 INSUFFICIENT_EVIDENCE, citations rỗng |
| Hotel-specific question thiếu hotelId | 200 SCOPE_REQUIRED theo heuristic trong service |
| Guardrail code/injection/PII khớp pattern | 200 OUT_OF_SCOPE; không coi regex là bằng chứng chống mọi injection |
| Bean Validation / IllegalArgumentException cục bộ | 400, shared ApiResponse code 400, message |
| Exception từ answer service | 503, code 503, message prefix RAG_TEMP_UNAVAILABLE, **result.status=INSUFFICIENT_EVIDENCE** và disclaimer outage |

Enum còn có `CONFLICTING_SOURCES`, `RAG_TEMP_UNAVAILABLE`, nhưng nhánh controller outage hiện không trả status RAG_TEMP_UNAVAILABLE. Consumer phải phân biệt HTTP 503 với HTTP 200 thiếu evidence; không tự đổi enum hoặc gom hai trường hợp. Catch hiện ghép `e.getMessage()` vào response/log stacktrace, và log câu hỏi đã redact; **UNKNOWN** mức an toàn với mọi exception/PII. Cần review sanitation; C01 không tuyên bố log hoàn toàn an toàn.

Retrieval lấy hotel-scoped đúng hotelId cùng CHAIN_PUBLIC; không hotelId chỉ có chain-public. Store chấp nhận ACTIVE/SUPERSEDED trong inclusive effective dates rồi service chọn scope hotel ưu tiên chain, ACTIVE ưu tiên status khác, effectiveFrom mới hơn, version cao hơn; toàn bộ chunks thuộc version thắng được xét. Threshold dot product 0.30 trên embedding chuẩn hoá; model local hash 256 chiều `local-multilingual-hashing-v1` hiện có, không phải Gemini embedding. Đây không chứng minh public eligibility hoặc authorization của hotelId đã được owner xác nhận.

Ví dụ request synthetic theo DTO hiện tại:

```json
{"hotelId":1,"question":"Synthetic hotel: mấy giờ nhận phòng?","language":"vi","asOfDate":"2026-10-28","topK":3}
```

Ví dụ response shape thiếu evidence, không khẳng định exact answer text luôn giống nhau:

```json
{"code":200,"message":"Thành công","result":{"status":"INSUFFICIENT_EVIDENCE","answer":"Chưa có thông tin trong corpus synthetic được duyệt.","citations":[],"warnings":[],"disclaimers":["Ví dụ synthetic; không có chính sách thực được xác minh."]}}
```

## 4. Delta cần owner review — PROPOSED

Widget tách intent tìm phòng qua R1 và FAQ qua knowledge/ask; hỏi lại hotel trước FAQ hotel-specific, resolve public/active hotel bằng contract tin cậy. Không suy hotel từ text/role claim hoặc hotelCode chưa resolve. Scope chưa rõ thì không hiển thị thông tin của hotel khác. Đổi hotel phải bỏ FAQ/citations cũ; response async chỉ render khi khớp scope/request hiện tại. Không tái sử dụng FAQ result để tạo room card/giá/khuyến mãi.

UI hiển thị answer + nguồn tĩnh/version và disclaimer; citation chỉ là dữ liệu text, không thực thi HTML/URL/chỉ dẫn. HTTP 200 insufficient khác HTTP 503 outage; FAQ outage không xoá/khoá flow R1. Không gọi admin stage/activate/rollback/delete từ widget. Chưa thêm endpoint, thay DTO R1 hoặc cho LLM write tools.

Đề xuất bound FAQ `topK` tối đa 5, scope/date policy và sanitized generic 503 message cần G2 + Member 2 duyệt trước implementation; hiện chưa enforce. Không đổi response outage enum trong slice này. Mọi schema/role/route delta phải được ghi và review, không sửa frozen R1 schema để bao thêm FAQ.

| Flag / provider | CONFIRMED | PROPOSED từ plan |
|---|---|---|
| `hotel.ai.rag.demo-corpus.enabled` | Có, mặc định false tại `DefaultPublicHotelCorpusInitializer`; chỉ seed synthetic khi bật | Chỉ bật ở sandbox đã duyệt, ghi corpus version |
| Gemini extraction | `HybridRoomAdvisorLlmService` có thể outbound khi `GEMINI_API_KEY` có giá trị | T12-02 thêm explicit opt-in mặc định false; có key chưa đủ bật, launcher xoá key kế thừa |
| RAG persistence | In-memory, restart mất knowledge | Pgvector opt-in sau G3, không thêm migration/config trong C01 |

Không chạy model trong C01, không đọc key. Corpus public/static/approved, không guest/payroll/PII/live price/inventory; không nói local hashing là model production. Session tối thiểu của R1 và lifecycle knowledge được duyệt riêng không cấp quyền ghi business.

## 5. Test oracle

| ID | Scenario | Expected outcome / nguồn |
|---|---|---|
| R01 | Frozen R1 examples và schema | JSON parse, mọi $ref resolve offline, ba POST routes giữ nguyên; `tools/t12/validate_r1_contracts.py` có checks rộng hơn về provenance, không được nới để đạt PASS |
| R02 | RAG stage/activate synthetic h1 rồi public ask h1/h2 | Stage STAGED/version1, activate200; ask h2 INSUFFICIENT_EVIDENCE, citations=[], không có 14h00/12h00 của h1; `frontend/e2e/api/rag-local.spec.ts` |
| R03 | Injection trong Playwright hiện tại | Không lộ hai sentinel secret của fixture; assertion hiện tại không đủ kết luận mọi injection đã bị chặn; bổ sung corpus injection tại SAFE-01 |
| R04 | Blank/>2000 question; topK=0; service outage | 400 validation; outage503/result INSUFFICIENT_EVIDENCE theo controller tests, không no-match |
| R05 | Thiếu hotel cho câu hỏi hotel-specific; ngoài domain; không evidence | SCOPE_REQUIRED / OUT_OF_SCOPE / INSUFFICIENT_EVIDENCE đúng nhánh; không fabricate sources |
| R06 | R1 khi RAG outage | R1 vẫn chạy; `RagIndependentDegradationTest`, `T12RagZeroWriteArchitectureTest` |
| R07 | Session owner khác, stale version, spam | 404 đồng nhất, 409, 429/Retry-After; không chấp nhận sessionId như auth; existing advisor security tests |
| R08 | Browser chat/card/detail hiện có | Navigation `/portal/hotels/{id}/room-types/{id}` kèm dates, không submit booking/payment trong advisor; `frontend/e2e/browser/t12-room-advisor.spec.ts` chưa test FAQ |
| R09 | Đổi hotel, response FAQ đến trễ, citation lạ, 503 | Không render cross-scope/stale response; render text an toàn, giữ R1; test tương lai sau G2, chưa có browser coverage tại C01 |
| R10 | Knowledge static trả price/inventory, document injection, effective/conflicting versions | Không override Core, không cross-scope/business write; threshold và gold mới chỉ tại SAFE-01, không sửa gold cũ |

Focused C01 chỉ chạy kiểm tra tài liệu/JSON/ref/example, không dùng local mock làm bằng chứng runtime. Sau implementation có thể chạy Maven `PublicRagKnowledgeControllerTest`, `ScopedRagRetrievalAndGroundedAnswerTest`, `RagIndependentDegradationTest` và các security tests tương ứng; Playwright cần sandbox. `NOT RUN` không tính PASS.

## 6. Gate, owner, decision và stop condition

| Gate / owner | Decision | Stop condition |
|---|---|---|
| G2 — Member 4 + Member 1; Member 2 cho trusted public scope/security delta | **BLOCKED widget integration**, chưa có sign-off | Chốt dispatch, scope, citations, error UI, topK/date policy trước sửa UI/API; thiếu public scope thì fail closed |
| G3 — Member 2 + Member 1 | **BLOCKED persistence/provider** | Chưa duyệt extension/schema/dimension/grants/config/backup thì không migration/pgvector; không hardcode dimension của model khác |
| G6 — owner dữ liệu/corpus + Member 1 | **BLOCKED dữ liệu thật** | Chỉ corpus static được owner duyệt; không thay U14 consent/history contract hoặc mẫu BCA trong C01 |
| G7 — Member 1 | **UNKNOWN sandbox** tại C01 | Không Docker/Redis/Gemini/DB test trước xác minh environment và snapshot/restore |

Rollback C01: sửa riêng tài liệu mới theo review, không sửa artifacts lịch sử. Rollout widget/provider/persistence là subtask sau owner gate; rollback từng adapter/flag, R1 hoạt động độc lập; không reset/clean hoặc import dirty fix Availability từ worktree khác. No-write của Core runtime ở base mới vẫn cần regression riêng; tài liệu này không phê duyệt implementation Core.


## 7. T12-01 implementation — 29/10/2026

Baseline2958ad43c9ed63bd8985b54787918e9bf776713c. User đồng ý tiếp tục T12-01 theo đề xuất và cho phép sửa cross-owner; không giả M4/M2 sign-off. Các mục1–6 giữ làm lịch sử baseline/proposal. OCR-02 không bị mở lại.

G2 local được quyết định: widget có hai mode explicit Tìm phòng và Chính sách & tiện ích. FAQ không sửa criteria/card/price R1. Public hotel list dùng endpoint hiện có, statusACTIVE, pageSize50; backend revalidate ACTIVE/not deleted qua narrow read-only interface. HotelId range1..32767; mã hotelCode chưa resolve bị400, topK1..5. Client không gửi asOfDate; callers cũ vẫn được dùng ISO date như trước, omission theo service clock. Không có date-policy delta.

Khách sạn không đủ eligibility trả200 SCOPE_REQUIRED, không retrieval/citation. Scope lookup/service failure trả503/code503 generic, giữ result.statusINSUFFICIENT_EVIDENCE như legacy để không đổi enum; HTTPstatus là oracle outage. Không trả exception hoặc log question. Không thay role/route/sessionCookie/CORS/TTL hoặc frozen R1 artifacts.

FAQ render text/citations/version/disclaimer. Đổi hotel xóa answer/question/citations và abort request; responsechậm không được render sau scopechange. Citation hotelId khác scope bị consumer từ chối toàn result. Không executeHTML/URL từ citation. Hỏi lỗi FAQ vẫn chuyển sang R1 hoạt động. Sidebar/Portal Search có sửa controlled-open callback để launcher mở được widget thật.

Scope local/synthetic; không providerGemini call, không pgvector/schema migration, không promotion/booking/payment hoặc guest write. Kiểm chứng chi tiết trong docs/member1/t12-01-delivery-2026-10-29.md.
