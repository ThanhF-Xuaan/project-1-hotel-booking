# Tiến độ và kịch bản demo chatbot RAG Gemini

Ngày 05/11/2026 — Member 1. **CONFIRMED: triển khai và kiểm thử offline. UNKNOWN: chất lượng Gemini live. Chưa đủ điều kiện release.**

Base/HEAD `63a26ba4c81c92e9892383de29bf76bc96b617b9`; branch `codex/m1-t12-rag-gemini-contract-20261104`. Code còn dirty/untracked; SHA một mình chưa mô tả bản chạy. Hash thực tế được ghi tại `backend/target/task10-preflight.json` và receipt TASK-10 sau kiểm chứng.

## Phần đã làm

| Phần | Evidence quan sát được |
|---|---|
| Corpus/eval | 12 nguồn synthetic, 13 version; 20 development và 40 acceptance tách riêng. Giữ DRAFT / NOT APPROVED, không sửa gold cũ |
| Provider/embedding | Model lazy/conditional, key riêng, kiểm dimension/count/NaN/zero, document/query task type, ingestion nguyên tử; có giới hạn chunk và không fallback hashing vào DB Gemini |
| Retrieval | Giữ scope/date/version precedence, top5, threshold riêng; calibration thật chưa chạy, 0.30 chưa là ngưỡng được đo/duyệt |
| Generation/citation | Prompt redacted, tối đa 5 chunk/10.000 ký tự; kiểm JSON/ID/quote/finish reason, Java dựng metadata; lỗi dùng extractive fallback có warning |
| Outbound/privacy/no-write | 2 ask đồng thời, 20/phút/JVM, hủy HTTP thật khi deadline, không retry; log metadata; architecture/spy và SDK loopback kiểm không ghi nghiệp vụ |
| Sandbox tooling | Launcher/profile riêng, kiểm target/hash/ownership/role, snapshot/restore; durable ledger trước outbound ≤5 USD/200 input/120 generation. Chưa provision DB hoặc gọi Gemini live |
| Widget | Microcopy fallback/429, giữ scope và abort/stale guard; render text, demo label opt-in, không khóa Tìm phòng |
| TASK-10 | Auditor capture có khóa hash, protocol chấm human, demo và runbook; không tạo kết quả live giả |

Mốc kiểm thử trước TASK-10: backend 1.761 PASS/19 SKIP; 22 test ledger và 42 check PowerShell; frontend lint/build PASS, 10 browser fixture test và 1 kiểm tra demo label tắt PASS, 4 runtime SKIP. Đây là bằng chứng offline, không phải 40 acceptance Gemini đã chạy. Kết quả kiểm tra mới nhất phải đọc receipt TASK-10, không cộng dồn số test giữa các lần chạy.

TASK-10: regression backend cộng test runner **1.770 PASS / 20 SKIP**, 0 failure/error; riêng runner **9 PASS / 1 SKIP**. Replay capture có human review là test được skip. Review đã bổ sung kiểm thiếu trace/write/citation sai và kiểm số liệu theo 40 case gốc; lượt lặp, p95 từng lượt và fallback được báo riêng. SHA artifact và dependency tree được lưu trong `backend/target/task10-verification.json` và `task10-dependency-tree.txt`.

Tóm tắt khi báo cáo: “Pipeline RAG và widget đã có kiểm soát nguồn, lỗi và giới hạn outbound; các kiểm tra offline đã chạy thành công. Phần còn lại để chứng nhận Gemini là môi trường persistent thực, calibration, 40 acceptance cùng các lượt lặp và human semantic review. Chưa có số liệu live để khẳng định chất lượng hoặc SLA.”

## File thuộc owner khác đã sửa

Người dùng xác nhận Member 4 đã cho phép UI và cho phép sửa cross-owner, yêu cầu báo file/lý do. TASK-09 sửa:

- `frontend/src/modules/booking-portal/components/RoomAdvisor/HotelKnowledgePanel.tsx`: microcopy fallback/429, demo label, abort khi gửi câu mới và chống kết quả cũ.
- `frontend/src/modules/booking-portal/services/hotelKnowledgeService.ts`: lỗi 429 typed và Retry-After đã validate, giữ kiểm scope/citation; không retry.
- `frontend/e2e/browser/t12-knowledge.spec.ts`: kiểm fallback, XSS, stale/unmount, 429/503 và R1 độc lập.
- `frontend/e2e/browser/t12-knowledge-runtime.spec.ts`: bổ sung assertion runtime/failure và source ID synthetic uppercase tương thích store.

TASK-10 chỉ thêm ba file được giao, không sửa code owner khác. Không sửa `Run-Application.ps1`, migrations, corpus hoặc gold. Xác nhận người dùng không bị ghi thành chữ ký peer reviewer độc lập.

## Demo 6 tình huống

Chỉ demo live sau các gate trong [runbook](../t12-rag-gemini-runbook.md). Nếu dùng fixture/browser mock, ghi rõ OFFLINE FIXTURE, không giới thiệu là Gemini live. Bật `VITE_RAG_SYNTHETIC_DEMO=true`, chọn khách sạn explicit; không có key frontend.

| Tình huống | Thao tác và điều cần kiểm |
|---|---|
| Câu hỏi chuẩn | Sen, asOfDate 20/11/2026: giờ nhận/trả phòng; nguồn H1-CHECKIN-v2, 15:00/11:00. Nếu UI không có date selector, cấu hình ngày sandbox được duyệt hoặc dùng request API có asOfDate; không giả định ngày hiện tại đã qua 15/11 |
| Paraphrase | Hỏi khác câu nhưng cùng facts; đúng nguồn/giờ, không bắt model lặp câu chữ |
| Câu hỏi mới | Khi đang chờ, gửi câu mới; response trước bị abort, không trộn lịch sử |
| Khác khách sạn | Chuyển Trúc: xóa kết quả cũ, nhận 16:00/trả 10:00 theo nguồn Trúc; không dùng policy Sen |
| Thiếu nguồn | Câu hỏi không có trong corpus: không bịa thông tin/citation, hướng dẫn liên hệ lễ tân |
| Generation fallback | Fixture timeout hoặc bad reference đã khóa: Java trích evidence, có nguồn và thông báo “Đang hiển thị trích đoạn tài liệu do dịch vụ AI tạm gián đoạn.” Không tính là Gemini generation thành công |

Không tắt quota hoặc gây outage DB nghiệp vụ để demo. Nhánh fault chỉ chạy trong fixture cô lập đã mô tả trong manifest. Mục Tìm phòng phải tiếp tục hoạt động khi knowledge 503/429.

## Nghiệm thu và giới hạn còn mở

| Hạng mục | Trạng thái hiện tại |
|---|---|
| 40 acceptance + 20 lượt lặp live | NOT RUN, 60/60 quan sát chưa thu; không có quality %, p95 hoặc chi phí live để báo |
| Human semantic review | NOT RUN; không tự duyệt facts/quality hoặc giả chữ ký |
| Persistent sandbox/restart/outage/restore thực | NOT RUN; cần target/DDL/grants/config được duyệt |
| Model/dimension/giá/usage thực | UNKNOWN; thiếu dữ liệu thì ledger khóa, không suy diễn token/cost |
| Dev threshold lock | NOT RUN; phải hoàn tất trước acceptance |
| Collector trace toàn bộ scenario | Chưa tự động hóa đầy đủ; runbook quy định protocol thu độc lập. Auditor không tự gọi provider hoặc tạo trace thay collector |
| Corpus/eval/threshold approvals | DRAFT/PROPOSED đến khi có quyết định cho đúng hashes |
| Production/multi-chain | Không chứng nhận từ corpus hai hotel của một chain synthetic |

Quyền sửa code của người dùng đã ghi nhận. Chưa có bằng chứng riêng về phê duyệt semantic, corpus/eval, provider/privacy/giá và môi trường chạy trong artifact hiện có; không suy từ quyền sửa code hoặc các báo cáo lịch sử. Không commit/push/merge trong TASK-10. Dùng [runbook](../t12-rag-gemini-runbook.md) để tiếp tục; mọi FAIL/NOT RUN phải giữ nguyên trong báo cáo tiến độ.
