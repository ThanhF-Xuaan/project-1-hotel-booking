# Release 1 evaluation — 0.1.0-draft

## Bổ sung trạng thái chạy harness local — 2026-10-24

`T12Release1EvaluationSuiteTest` đã chạy trên `develop` baseline `a6b2088f55d2326a51e895ff2cfffbb4fe70831e` với `GEMINI_API_KEY` để trống: **74/74 case PASS**, không lỗi hoặc bỏ qua. Đây là kết quả harness synthetic qua deterministic application path, không phải Gemini live, đánh giá chất lượng model, human approval hay release sign-off. Dataset không đổi; SHA-256 vẫn là `8b8f086d5da5f08679f63053e9d17dbc1cbaa510b68a8c16c68e2da680c6bf5c`.

Kết quả harness được giữ tách biệt với trạng thái duyệt gold assertions bên dưới. Không dùng lần chạy này để thay đổi `PENDING` thành approved hoặc kết luận về độ chính xác, latency, token hay cost của provider.

## Trạng thái và thực thi

74 cases trong `cases.json`, contract artifact `2.0.0-draft.1`; **tất cả PENDING human approval**, execution `NOT_RUN_NO_ENGINE`. Không có live Gemini run hoặc kết quả accuracy/latency/token/cost. Validator R1-00 chỉ xác minh dataset structure, case IDs, request schemas, fixture references, patch targets và safety oracle metadata.

`governance-coverage.json` map 35 yêu cầu R1 của draft v2 do người dùng thêm; 036–042 thuộc R2 nên không triển khai ở đây. Mapping giữ source digest và chỉ ghi DESIGN COVERAGE, không execution PASS. 56 expected assertions ban đầu giữ nguyên; 18 case mới bổ sung provenance/ambiguity, eligibility, duplicate catalog, expired session, missing route, stale price/inventory, schema-invalid model output, SQL/URL denial, retry bounds, guest writes và stability.

Categories: hard_constraints, ranking, pricing, multi_turn, security, no_write, errors. Đáp án là draft acceptance assertions, không phải generated model outputs. `expected.businessWriteCalls=0` áp dụng mọi case. Cases ranking dùng policy proposed trong DTO contracts; cần Member 1/M2/4 thống nhất trước freeze.

## Quy ước runner cho task sau

1. Mỗi case độc lập: fixed clock 08/10/2026, allowed hotels 1..6, synthetic owner `owner-a`; không có dữ liệu người thật. Session fixture 00000000-0000-4000-8000-000000000001 thuộc owner-a, criteria đầy đủ như SEARCH example. Session 00000000-0000-4000-8000-000000000002 thuộc owner-b.
2. `context` thiết lập version/provider/RAG/rate limit/runtime cap; request không session bắt đầu conversation mới. Các field không có trong context dùng default của case fixture, không lấy state từ case trước.
   `sessionExpired` và `locationAmbiguity` mặc định false. `detailRouteFixtureApproved` mặc định true **chỉ trong synthetic sandbox**, không thay approval Member 4. `llmOutputOverride` là untrusted test data để mô phỏng model output. `corePricingHttpStatus/retryPolicyFixtureMaxAttempts` chỉ là fault-injection policy synthetic. `bindExplicitSessionToPriorTurn=true` dùng session mới của turn trước; `nativeResponsesForChangedDates=REMAP_OCCUPIED_DATES_KEEP_VALUES` chỉ remap fixture dates, không thực hiện price math hoặc production fallback.
3. Deep-copy fixture bundle, apply fixtureMutations theo thứ tự JSON Patch add/replace/remove; patch value là dữ liệu test, không sửa fixture file. PRICE/AVAILABILITY response null mô phỏng upstream failure; invalid ID/date không được silently sửa. Mutation quote metadata dùng cho boundary validation riêng của adapter output.
4. Run `turns` theo thứ tự; bind cùng owner, inject session/expected version của response trước nếu scenario chỉ định. Turn có sessionId cố định phải dùng session fixture tương ứng. Assertions `status`/top IDs trên turn cuối; invariant no-write/PII/scope và những field lỗi áp dụng toàn trace.
5. Hard constraint violations, unauthorised tenant/session/PII leaks, fake price và writes là critical fail, không được trung bình hoá vào accuracy. Ranking đánh giá trên eligible candidates: exact Top IDs ở oracle rõ ràng, pair order, NDCG@5 khi relevance labels được human duyệt. Dataset hiện chưa có graded labels để tuyên bố NDCG đã đo.
6. SEARCH/REFRESH deterministic core tests không gọi LLM/RAG. CHAT cần separate intent extraction evaluation; không đánh đồng structured fixture pass với AI accuracy. Khi Gemini PoC được phép ở task khác, lưu model/config/version, trials, p95 latency, tokens và cost; ngưỡng hiện PENDING trong manifest.

## Approval và chống thay golden answers

- Người duyệt review input/context/fixtures/assertions, version và SHA256 của cases.json + manifest. Safety/product đã chốt không cần mở lại; những oracle mới phải ký riêng.
- Bản này chưa freeze. Khi approve, lưu signed approval record và digest qua PR/task được cấp phép; không chỉnh status tại R1-00 để giả đã duyệt.
- Không sửa đáp án đã được duyệt để cho implementation pass. Mọi thay đổi expected answers hoặc context của bản đã freeze phải tạo version mới, có rationale/diff và human review; giữ nguyên bản cũ.
- CI runner phải kiểm tra digest/frozen snapshot trước run; manifest nhắc policy không thay thế runtime enforcement. R1-00 không có baseline golden answer đã human-approved nên không sửa bất kỳ bộ cũ nào.

## Go/No-Go

Go cho independent development sau task approval: schema/reference tests pass, assumptions explicit, synthetic fixtures đủ đọc, draft dataset phủ các nhóm trên. No-Go live integration cho đến public contracts/currency/fee/session/detail route được Member 2/4 duyệt. No-Go release nếu có bất kỳ critical fail, incomplete coverage bị che, hoặc thiếu runtime evaluation theo quality gates đã thống nhất.
