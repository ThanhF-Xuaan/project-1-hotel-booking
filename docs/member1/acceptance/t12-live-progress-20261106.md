# T12 — shared budget và lần kiểm chứng live đầu tiên

CONFIRMED: base f1e26b80268c166dae0c6d7ee91d7b7718190e54, worktree riêng m1-t12-chatbot-20261106. Người dùng cho phép agent review kỹ thuật dưới tất cả vai trò để phục vụ Member 1. Đây là quyết định của user, không tạo chữ ký peer review của thành viên khác. Gate review trước đó không còn là lý do ngừng công việc trong sandbox đã cho phép. Toàn bộ chatbot chưa hoàn thành.

## Thay đổi và review

- R1 extraction dùng cùng RagOutboundLedger với RAG: reserve trước HTTP, không đưa key vào đối tượng ledger, settle usage đúng cả thinking thông qua total token count. Timeout/lỗi/usage không xác định giữ reservation và chặn chiến dịch. Cap không áp lại từ đầu theo tab hoặc sau restart. Bật chế độ ledger phải dùng cùng policy/campaign trong một backend process.
- Thêm 5 regression R1 và 1 regression shared cap qua restart. Review xác nhận offline không chạm ledger/transport, cap failure không gửi HTTP, timeout hủy request. Không thay giá/tồn phòng hoặc public DTO.
- Live compatibility IT bắt buộc configured ledger trước bất kỳ model call. Không được chạy PoC trực tiếp để né campaign.
- Initialize-ChatbotSandbox.ps1 chuẩn bị thư mục ACL owner/SYSTEM, sinh 5 bí mật 64 ký tự hex, import khóa từ environment bằng flag explicit, từ chối ghi đè và từ chối lưu vào repo. Chỉ tạo cấu hình DRAFT; các mật khẩu dành cho dịch vụ mới, chưa xác thực dịch vụ hiện có. Chưa chạy provisioning.

## Bằng chứng

| Kiểm tra | Kết quả |
|---|---|
| Shared accounting unit/ledger | PASS 56/56, 0 skipped |
| Advisor/RAG regression sau thay đổi | 312 tổng: 311 PASS, 1 calibration skipped, 0 failure/error |
| Private configuration preparation | PASS: ACL protected owner/SYSTEM, từ chối ghi đè và hash không đổi, từ chối đường dẫn repo |
| Metadata model thật | HTTP200 cho gemini-2.5-flash và gemini-embedding-001; không chứng minh quota generation |
| Live compatibility IT | FAIL 1/1, 0 skipped. Không có PASS receipt mới |
| Billing guard | PASS quan sát: RESERVE 1 embedding input, HALT UNKNOWN_OR_OUT_OF_BOUND_USAGE; không có generation |

Log local: backend/target/shared-ledger-tests.log và shared-ledger-regression.log. Live output không phát tán raw provider body hoặc key; test failure chỉ giữ lớp IllegalStateException. Receipt metadata ở t12-model-metadata-20261106.json.

## Phát hiện live và điểm dừng thực tế

Ngày 06/11/2026, PoC dùng Spring AI/SDK thật với query synthetic. Provider audit quan sát outcome=SUCCESS nhưng input/output/total đều -1; chưa chứng minh vector shape hoặc chat vì settlement đã từ chối phản hồi trước khi SDK trả về.

Ledger có đúng RESERVE cho 1 embedding input, maxCost=0.000307200000 USD, rồi HALT UNKNOWN_OR_OUT_OF_BOUND_USAGE. Đây là mức dự trù còn giữ, KHÔNG phải chi phí thực đã biết. Không reset journal/campaign, không retry và không gán token giả để làm PASS. Chưa ingest năm tài liệu vào kho Gemini.

Blocker hiện tại là contract usage của embedding API và cơ chế đối soát khoản reservation này. Không quy lỗi quota vì HTTP metadata và provider success không chỉ ra quota lỗi. Cần xác minh schema usage thực và cách đo billing đúng trước khi resume; sửa theo chứng cứ rồi kiểm transport fixture, ledger và live trở lại với campaign giữ nguyên. Không tạo campaign mới để né HALT. Khi chưa đủ thông tin để đối soát, không tuyên bố đã fix hoặc chatbot live đã xong.

## Cấu hình đã chuẩn bị

Private runtime: C:/Users/admin/.codex/t12-chatbot-runtime-20261106. Đã sinh các mật khẩu sandbox mới và import key có sẵn trực tiếp từ environment theo quyền user; không đọc/in file secrets. File secrets.json user đã tạo ở C:/private được giữ nguyên.

Policy có cap tổng5USD, input bound chat1048576, output768; embedding2048 token/input. Candidate model giữ gemini-2.5-flash để tương thích R1 và gemini-embedding-001 chiều768 cho PoC. Đọc metadata thành công không thay bằng chứng vector dimension thực tế. Cấu hình sandbox vẫn DRAFT, chưa provision Core/RAG/Redis/Keycloak mới.

Nguồn đối chiếu đã đọc: https://ai.google.dev/gemini-api/docs/pricing (text input Flash0.30USD/1M, output gồm thinking2.50USD/1M, embedding0.15USD/1M); https://ai.google.dev/gemini-api/docs/models/gemini-2.5-flash; https://ai.google.dev/gemini-api/docs/embeddings; https://ai.google.dev/api/embeddings. Snapshot pricing và policy được lưu private, khóa hash trước request. Actual usage/cost vẫn UNKNOWN cho request đã HALT.

Không commit/push/merge, không thay DB/IAM hoặc seed. Không xem một request provider SUCCESS là nghiệm thu PoC/chất lượng/chatbot PASS.
