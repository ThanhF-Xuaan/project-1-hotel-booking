# Kế hoạch triển khai AI chatbot — Tổng quan

Ngày lập: 25/09/2026. Người thực hiện chính: thành viên phụ trách AI chatbot.

Đọc [kế hoạch chi tiết](AI_CHATBOT_IMPLEMENTATION_DETAILS.md) khi bắt đầu một task. Hai tài liệu dùng cùng mã task nhỏ `AIC-001` đến `AIC-036`, mỗi mã tương ứng một branch và một PR. Các mã cũ `AI-01` đến `AI-12` chỉ còn là nhóm việc; xem bảng đối chiếu ở mục 5. Kế hoạch này triển khai chi tiết phần AI của [backlog cả nhóm](../../document/tasks-backend-ai-after-codebase.md).

## 1. Kết quả cần đạt

Hoàn thiện chatbot tiếng Việt cho nhân viên chuỗi khách sạn với ba khả năng: trả lời chính sách từ tài liệu đã duyệt và có citation; tra cứu khách sạn, hạng phòng, tình trạng phòng và giá qua service backend; hỏi lại khi thiếu thông tin hoặc không đủ căn cứ trả lời.

Mốc MVP dùng chat một lượt. Khi thiếu ngày hoặc hạng phòng, FE hiển thị trường cần bổ sung và gửi lại một request đầy đủ; việc này chưa cần lưu lịch sử hội thoại ở server. Người dùng được phép hỏi theo cơ sở đang chọn và phạm vi IAM của mình.

Phần AI tiếp tục nằm trong module `aiassistant` của Spring Boot, dùng Java 21 và Spring AI đang có. PostgreSQL/pgvector lưu tài liệu, phiên bản, job và embedding. Redis phục vụ quota khi triển khai giới hạn dùng chung giữa các instance. Đây là hướng triển khai tiếp từ codebase hiện tại.

## 2. Điểm xuất phát thực tế

| Thành phần | Hiện có | Phần cần hoàn thiện |
|---|---|---|
| Chat API | `POST /api/v1/ai/chat`, JWT nhân viên, giới hạn message 1.500 ký tự | Contract hỏi lại, chọn công cụ từ câu hỏi tự nhiên, lỗi rõ cho FE |
| Trả lời bằng RAG | Embedding câu hỏi → tìm chunk → gọi model → trả citation | Bộ tri thức thực tế, đo chất lượng, kiểm chứng citation và xử lý nguồn mâu thuẫn |
| Quote tool | `roomQuery` có cấu trúc gọi `RoomQuoteTool`, trả `LIVE_QUOTE` | Tool catalog/availability và điều phối tự nhiên; giữ contract quote đang dùng |
| Tài liệu | Nhận text tối đa 20.000 ký tự, tạo version 1 và job ingest | Danh sách/detail, metadata nguồn/ngày hiệu lực, API tạo version tiếp theo |
| Ingest | Job PostgreSQL, retry, lease, ghi chunk trong transaction | Backoff, retry có kiểm soát, chunk theo nội dung và giới hạn tài nguyên |
| Chunking/retrieval | Khoảng 800 ký tự/chunk, không overlap; `topK` mặc định 4, ngưỡng 0,55 trong query | So sánh cấu hình trên tập câu hỏi, đưa tham số cần thiết ra cấu hình |
| Publish/revoke | Có trạng thái version, publish/revoke và kiểm tra lại nguồn | Chọn đúng version được duyệt, xử lý cạnh tranh và ngày hiệu lực |
| Citation | Trả danh sách các chunk được đưa vào context | Chỉ trả nguồn hỗ trợ câu trả lời và đối chiếu source ID ở server |
| Quyền | Permission, scope chuỗi/vùng/cơ sở; lỗi ID trên 127 đã sửa | Regression cho API mới, scope đổi trong request, nguồn hết hiệu lực |
| Vận hành | AI tắt mặc định; có cấu hình provider, semaphore 8 request/instance | Smoke provider thật, timeout có kiểm chứng, quota theo actor, token/chi phí/metrics |

Các trạng thái hiện có là `GROUNDED`, `LIVE_QUOTE`, `NO_SOURCE`. Trạng thái và field bổ sung trong kế hoạch chi tiết là đề xuất cần triển khai cùng FE. Số đo chất lượng, chi phí và thời gian phản hồi chưa phải kết quả đã được xác minh.

## 3. Phạm vi trách nhiệm

| Đầu việc | Người thực hiện chính | Phối hợp |
|---|---|---|
| Corpus, chunking, embedding, retrieval, prompt, citation | AI | Nghiệp vụ duyệt nội dung; QA đánh giá câu trả lời |
| Controller/service/repository và DTO trong `aiassistant` | AI | BE1 review IAM, transaction và migration |
| Adapter gọi service nghiệp vụ, validation tham số tool | AI | BE1 review catalog/availability; BE2 review quote |
| Source API và contract chat/knowledge | AI | FE làm giao diện; QA nghiệm thu luồng |
| Redis quota, quan sát provider và ingest | AI | BE1 phối hợp hạ tầng và health/metrics |
| Pricing, inventory command, booking, payment | Backend | AI dùng service đọc đã công bố |

Có thể làm ngay bằng catalog, availability và quote hiện tại. Booking/payment sandbox không nằm trên đường phụ thuộc của MVP AI. Chưa bổ sung tool đọc booking/payment/PII khi module sở hữu chưa có contract quyền tương ứng.

<a id="branch-task-board"></a>

## 4. Bảng theo dõi: 36 task tương ứng 36 branch

Mỗi dòng là một task để tạo branch/PR. Trạng thái dùng `Todo → Doing → Review → Done`; `Blocked` phải ghi nguyên nhân, người hỗ trợ và task có thể làm thay thế. `Done` nghĩa là PR đã merge vào nhánh tích hợp và đạt tiêu chí của task. Tất cả task dưới đây đang là kế hoạch, chưa bắt đầu thực hiện.

### 1 — Baseline và contract

| Task | Branch | Kết quả chính | Phụ thuộc trực tiếp | Trạng thái |
|---|---|---|---|---|
| [AIC-001](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-001) | `docs/aic-001-baseline-runbook` | Ghi baseline và cách chạy chatbot hiện có | Có thể bắt đầu ngay | Todo |
| [AIC-002](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-002) | `chore/aic-002-provider-smoke` | Kiểm chứng provider thật và cấu hình timeout | `AIC-001` | Todo |
| [AIC-003](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-003) | `docs/aic-003-knowledge-corpus` | Chuẩn bị corpus có nguồn và scope | Có thể bắt đầu ngay | Todo |
| [AIC-004](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-004) | `test/aic-004-evaluation-fixtures` | Tạo bộ câu hỏi và schema dữ liệu đánh giá | `AIC-003` | Todo |
| [AIC-005](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-005) | `feat/aic-005-chat-contract` | Chuẩn hóa DTO và contract chat mở rộng | Có thể bắt đầu ngay | Todo |
| [AIC-006](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-006) | `refactor/aic-006-prompt-resources` | Đưa prompt ra resource có version | `AIC-001` | Todo |

### 2 — Tài liệu và ingest

| Task | Branch | Kết quả chính | Phụ thuộc trực tiếp | Trạng thái |
|---|---|---|---|---|
| [AIC-007](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-007) | `feat/aic-007-document-provenance` | Thêm metadata nguồn và người tạo tài liệu | `AIC-003`, `AIC-005` | Todo |
| [AIC-008](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-008) | `feat/aic-008-document-effective-window` | Áp dụng ngày hiệu lực cho tài liệu | `AIC-007` | Todo |
| [AIC-009](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-009) | `feat/aic-009-knowledge-read-api` | API danh sách, detail và lịch sử version | `AIC-007`, `AIC-008` | Todo |
| [AIC-010](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-010) | `feat/aic-010-explicit-version-publish` | Publish đúng version và serialize publish/revoke | `AIC-008`, `AIC-009` | Todo |
| [AIC-011](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-011) | `feat/aic-011-create-document-version` | Tạo phiên bản tài liệu mới và chống tạo trùng | `AIC-010` | Todo |
| [AIC-012](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-012) | `fix/aic-012-ingestion-lease-backoff` | Củng cố lease và backoff của ingest job | Có thể bắt đầu ngay | Todo |
| [AIC-013](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-013) | `feat/aic-013-ingestion-retry-api` | API retry job FAILED có quyền và audit | `AIC-012`, `AIC-009` | Todo |
| [AIC-014](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-014) | `feat/aic-014-semantic-chunking` | Chia đoạn theo nội dung và lưu vị trí nguồn | `AIC-003`, `AIC-011`, `AIC-012` | Todo |
| [AIC-015](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-015) | `fix/aic-015-embedding-validation` | Kiểm tra embedding ở cả ingest và câu hỏi | Có thể bắt đầu ngay | Todo |
| [AIC-016](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-016) | `feat/aic-016-reindex-document-version` | Reindex bằng version mới có thể chuyển đổi an toàn | `AIC-011`, `AIC-012`, `AIC-014`, `AIC-015` | Todo |

### 3 — Retrieval và citation

| Task | Branch | Kết quả chính | Phụ thuộc trực tiếp | Trạng thái |
|---|---|---|---|---|
| [AIC-017](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-017) | `feat/aic-017-scoped-retrieval` | Tách retrieval và áp chính sách chọn nguồn | `AIC-004`, `AIC-005`, `AIC-008`, `AIC-014`, `AIC-015` | Todo |
| [AIC-018](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-018) | `feat/aic-018-grounded-citations` | Câu trả lời có citation được kiểm tra tại server | `AIC-006`, `AIC-007`, `AIC-014`, `AIC-017` | Todo |

### 4 — Tool và câu hỏi tự nhiên

| Task | Branch | Kết quả chính | Phụ thuộc trực tiếp | Trạng thái |
|---|---|---|---|---|
| [AIC-019](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-019) | `feat/aic-019-intent-parameters` | Nhận diện intent và tham số có cấu trúc | `AIC-005`, `AIC-006` | Todo |
| [AIC-020](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-020) | `feat/aic-020-hotel-catalog-tool` | Tool danh sách khách sạn đúng scope | `AIC-005` | Todo |
| [AIC-021](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-021) | `feat/aic-021-room-availability-tool` | Tool availability với ngày và sức chứa | `AIC-005` | Todo |
| [AIC-022](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-022) | `feat/aic-022-room-search-tool` | Tool tìm hạng phòng và giới hạn fan-out | `AIC-020`, `AIC-021` | Todo |
| [AIC-023](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-023) | `feat/aic-023-chat-tool-dispatch` | Nối câu hỏi tự nhiên với tool và hỏi lại | `AIC-018`, `AIC-019`, `AIC-020`, `AIC-021`, `AIC-022` | Todo |

### 5 — Quyền và giới hạn tài nguyên

| Task | Branch | Kết quả chính | Phụ thuộc trực tiếp | Trạng thái |
|---|---|---|---|---|
| [AIC-024](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-024) | `fix/aic-024-refresh-actor-access` | Kiểm tra lại quyền trước khi trả lời | `AIC-018`, `AIC-023` | Todo |
| [AIC-025](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-025) | `fix/aic-025-prompt-injection-guards` | Củng cố xử lý chỉ dẫn độc hại và source output | `AIC-018`, `AIC-023`, `AIC-024` | Todo |
| [AIC-026](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-026) | `feat/aic-026-request-token-limits` | Cấu hình giới hạn context, output và số lần gọi | `AIC-023` | Todo |
| [AIC-027](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-027) | `feat/aic-027-request-deadline` | Deadline tổng và giải phóng tài nguyên khi timeout | `AIC-002`, `AIC-026` | Todo |
| [AIC-028](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-028) | `feat/aic-028-redis-chat-rate-limit` | Rate limit theo actor bằng Redis | `AIC-026` | Todo |
| [AIC-029](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-029) | `feat/aic-029-ai-usage-metrics` | Đo latency, token và trạng thái ingest | `AIC-018`, `AIC-023`, `AIC-012` | Todo |
| [AIC-030](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-030) | `feat/aic-030-daily-token-budget` | Hạn mức token/ngày và ngân sách provider | `AIC-026`, `AIC-028`, `AIC-029` | Todo |

### 6 — Evaluation, UAT và bàn giao

| Task | Branch | Kết quả chính | Phụ thuộc trực tiếp | Trạng thái |
|---|---|---|---|---|
| [AIC-031](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-031) | `test/aic-031-evaluation-runner` | Runner đánh giá retrieval, answer và tool | `AIC-004`, `AIC-018`, `AIC-023` | Todo |
| [AIC-032](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-032) | `perf/aic-032-retrieval-quality-tuning` | Tuning retrieval trên bộ development | `AIC-002`, `AIC-016`, `AIC-031` | Todo |
| [AIC-033](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-033) | `test/aic-033-ai-resilience-validation` | Kiểm chứng lỗi, cạnh tranh và giới hạn toàn luồng | `AIC-016`, `AIC-025`, `AIC-027`, `AIC-028`, `AIC-030`, `AIC-031` | Todo |
| [AIC-034](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-034) | `test/aic-034-frontend-contract-uat` | Nghiệm thu contract với frontend | `AIC-009`, `AIC-010`, `AIC-011`, `AIC-013`, `AIC-018`, `AIC-023`, `AIC-024`, `AIC-028` | Todo |
| [AIC-035](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-035) | `docs/aic-035-operations-runbook` | Runbook vận hành, reindex và phục hồi | `AIC-002`, `AIC-013`, `AIC-016`, `AIC-027`, `AIC-028`, `AIC-030` | Todo |
| [AIC-036](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#aic-036) | `docs/aic-036-acceptance-report` | Báo cáo nghiệm thu và đóng mốc AI MVP | `AIC-032`, `AIC-033`, `AIC-034`, `AIC-035` | Todo |

## 5. Đối chiếu với 12 nhóm việc cũ

`AI-01` đến `AI-12` trở thành **nhóm việc (epic)** để tra cứu phạm vi. Chỉ các mã `AIC-xxx` được dùng để tạo branch và theo dõi task; không tạo một branch lớn cho cả epic.

| Nhóm cũ | Phạm vi | Các task mới |
|---|---|---|
| AI-01 | Baseline/provider | `AIC-001`, `AIC-002` |
| AI-02 | Corpus/evaluation fixtures | `AIC-003`, `AIC-004` |
| AI-03 | Contract/prompt resources | `AIC-005`, `AIC-006` |
| AI-04 | Quản lý metadata và đọc tài liệu | `AIC-007`, `AIC-008`, `AIC-009` |
| AI-05 | Publish/version | `AIC-010`, `AIC-011` |
| AI-06 | Ingest/retry/chunking/reindex | `AIC-012`, `AIC-013`, `AIC-014`, `AIC-015`, `AIC-016` |
| AI-07 | Retrieval/citation | `AIC-017`, `AIC-018` |
| AI-08 | Intent và tools | `AIC-019`, `AIC-020`, `AIC-021`, `AIC-022`, `AIC-023` |
| AI-09 | Kiểm tra quyền/injection | `AIC-024`, `AIC-025` |
| AI-10 | Giới hạn/metrics/budget | `AIC-026`, `AIC-027`, `AIC-028`, `AIC-029`, `AIC-030` |
| AI-11 | Evaluation và resilience | `AIC-031`, `AIC-032`, `AIC-033` |
| AI-12 | FE/runbook/nghiệm thu | `AIC-034`, `AIC-035`, `AIC-036` |

## 6. Cách chọn task tiếp theo

1. Bắt đầu `AIC-001` để ghi baseline; đồng thời có thể chuẩn bị nội dung của `AIC-003` hoặc contract `AIC-005`. Một người nên giữ một branch code đang Doing để tránh trộn thay đổi.
2. Sau `AIC-003`, làm `AIC-004`; sau `AIC-001`, làm `AIC-006`. Chạy `AIC-002` khi có key. Thiếu key không chặn corpus, contract, phần lớn quản lý tri thức hoặc tool dùng fake model.
3. Nhánh quản lý tài liệu: `AIC-007 → AIC-008 → AIC-009 → AIC-010 → AIC-011`. Nhánh lease (`AIC-012`) và validation embedding (`AIC-015`) có thể bắt đầu độc lập.
4. Nhánh tool: `AIC-019`, `AIC-020`, `AIC-021`, `AIC-022`; chỉ nối điều phối toàn luồng tại `AIC-023` khi các dependency của task đó đã merge.
5. Hoàn thiện guard và tài nguyên, sau đó `AIC-031` đến `AIC-036` để đánh giá/bàn giao. Runner `AIC-031` có thể xây sớm ngay khi đủ dependency của nó.

Không cần chờ backend booking/hold/payment. Nhánh gốc phải có codebase và **toàn bộ PR tiền đề đã merge**, không chỉ có commit nền. Bảng dependency là điều kiện bắt đầu; thứ tự số là gợi ý đọc, không bắt buộc làm tuần tự 001 đến 036.

## 7. Quy tắc một task — một branch — một PR

- Tên chuẩn: `<type>/aic-<số>-<mục-tiêu>`; ví dụ `feat/aic-011-create-document-version`. Các type dùng trong kế hoạch: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `perf`.
- Branch có thể có nhiều commit, nhưng chỉ một kết quả nghiệp vụ/kỹ thuật đủ rõ để review. Không đồng nghĩa một task chỉ sửa một file.
- Migration, API/service, quyền, test và tài liệu cần thiết để tính năng an toàn phải đi cùng branch. Không tách một branch đưa tính năng vào sử dụng trước, rồi mới làm quyền/test ở branch sau.
- Mỗi PR phải merge được mà backend vẫn khởi động và các luồng cũ còn hoạt động. Tính năng chưa đủ thành phần giữ dưới interface nội bộ/feature flag hoặc chưa bật ở endpoint; không trả response giả để lấp chức năng còn thiếu.
- Ước lượng mỗi task khoảng 0,5–2 ngày công; nếu vượt 2–3 ngày hoặc có hai kết quả độc lập, tách task trước khi PR phình lớn.
- Khi PR đang Review, chỉ bắt đầu task độc lập khác. Với dependency chưa merge, ưu tiên chờ/cải thiện tài liệu; chỉ dùng stacked PR khi chủ động ghi rõ base PR và cập nhật lại sau merge.
- Không tạo hàng loạt 36 branch ngay từ đầu. Tạo branch khi chuyển task sang Doing và cập nhật nhánh tích hợp trước mỗi task.

Hiện local branch `ndt/ai-chatbot` chứa commit nền `d73d62e`; local `develop`/`main` chưa chứa commit này tại thời điểm đọc. Dùng `ndt/ai-chatbot` làm nhánh tích hợp tạm cho kế hoạch; khi nhóm đã merge codebase vào `develop`, chuyển nhánh gốc/đích PR sang `develop`. Kiểm tra lại trạng thái remote lúc thực hiện, không coi kiểm tra local này là tình trạng remote.

Xem [quy trình Git và mẫu PR](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#10-quy-trinh-git) trong bản chi tiết. Kế hoạch chỉ định tên branch; việc tạo branch thực tế diễn ra khi bắt đầu task.

## 8. Ước lượng và điều kiện bàn giao

Ước lượng cộng từ 36 thẻ task: **28,5–52 ngày công**, mỗi ngày khoảng 4–6 giờ tập trung, chưa gồm thời gian chờ review, key hay tài liệu nghiệp vụ. Đây là dự toán theo task nhỏ, thay cho ước lượng gộp 26–38 ngày của bản trước; không phải lịch bàn giao cố định. Cập nhật lại sau 4–6 task đầu bằng thời gian thực tế.

`P0` là phần lõi; `P1` là phần vận hành/bàn giao. Priority của từng task ở bản chi tiết; cả P0 và P1 trong kế hoạch phải đạt trước `AIC-036`. Các ngưỡng chất lượng RAG, quyền, latency và chi phí tiếp tục theo [mục 9 của bản chi tiết](AI_CHATBOT_IMPLEMENTATION_DETAILS.md#9-thiet-ke-kiem-thu).

## 9. Phần để sau MVP AI

Hội thoại nhiều lượt có lưu trữ, streaming/SSE, chatbot public, upload PDF/DOCX nếu corpus chưa cần, OCR, crawler tự động, tool ghi booking/payment và fine-tuning. Khi phát sinh phạm vi mới, cấp mã task mới; không thêm ngầm vào một branch đang Review.

Tài liệu nền: [bản đồ kiến trúc](ARCHITECTURE_KNOWLEDGE_MAP.md), [README backend](../../backend/README.md), [ADR giá](../adr/0001-pricing-quote-policy.md), [ADR giới hạn lưu trú và availability](../adr/0002-stay-limits-and-sale-availability.md).
