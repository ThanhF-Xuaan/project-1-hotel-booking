# T12 chatbot — triển khai trên develop mới

CONFIRMED: base `f1e26b80268c166dae0c6d7ee91d7b7718190e54`, worktree riêng `C:/Users/admin/.codex/worktrees/m1-t12-chatbot-20261106`, detached HEAD. Người dùng cho phép triển khai kế hoạch và fetch/rebase. Worktree D: và các thay đổi dở được giữ nguyên. Không stage/commit/push.

## Tích hợp source

Đã áp 43 file ngoài POM từ patch tích hợp đã kiểm trước đó. POM được ghép riêng: giữ Lombok 1.18.36 và thứ tự annotation processor của upstream; thêm Spring AI BOM 2.0.2 và hai model modules của feature. Không đưa thay đổi seed/Liquibase từ worktree cũ sang. Xung đột patch POM đã giải quyết; không còn là blocker triển khai.

## Kiểm chứng thực chạy

- PASS: Maven compile toàn bộ main/test source trên JDK 21, regression chọn lọc Advisor/RAG: 306 tổng, 305 đạt, 1 skipped calibration; 0 failure/error. Không gọi là full backend suite. Log cục bộ `backend/target-integration-check.log`.
- PASS: frontend TypeScript/Vite build; tái sử dụng node_modules đã cài qua junction. Log `frontend/build-integration-check.log`.
- PASS: git diff --check.
- NOT RUN: frontend browser trên build mới, database integration, Gemini live, pilot ingest mới hoặc nghiệm thu ngữ nghĩa.

Lệnh Maven: `mvnw.cmd -o -Dacceptance.integration=false -Dtest=RagKnowledgeReviewRegressionTest,ScopedRagRetrievalAndGroundedAnswerTest,GeminiRag*Test,RagProvider*Test,RagRequestBudgetTest,RagOutboundLedgerTest,T12RagZeroWriteArchitectureTest,HybridRoomAdvisorLlmServiceTest test`. Live/calibration/eval flags false; Liquibase/DDL tắt; datasource trỏ loopback port 1 để tránh dùng DB ngoài ý muốn.

## Phần kế hoạch tiếp theo

Chưa hoàn thành kế hoạch chatbot live. Còn chuẩn bị launcher sandbox đúng pilot, tự sinh cấu hình/bí mật sandbox, chốt model/quota và policy chi phí phủ cả R1/RAG, kho embedding Gemini riêng và bộ case pilot. R1 hiện có Gemini adapter riêng; cần đưa lời gọi R1 vào kiểm soát chi phí chung trước khi bật cùng RAG. Không coi test offline hoặc fallback là thành công Gemini live. Không tạo phê duyệt owner hoặc chữ ký peer review.
