# Yêu cầu Member 2 duyệt môi trường chatbot live

Status: PROPOSED / CHƯA PROVISION. Base `f1e26b80268c166dae0c6d7ee91d7b7718190e54`. Người dùng yêu cầu dừng khi cần Member khác review hoặc sửa file của Member khác. Không tự ghi approval hoặc chữ ký Member 2.

## Quyết định cần duyệt

1. Môi trường mới, project `hotel-m1-t12-live`, bind loopback; dự kiến Core PostgreSQL `127.0.0.1:15437/hotel_t12_demo`, pgvector `127.0.0.1:15436/t12_pgvector_gemini_pilot_hl`, Redis `16390`, Keycloak `18091`, backend `18090`, frontend `13090`. Không có listener trên các cổng này tại preflight 06/11; cần kiểm tra lại trước provision. Pin image digest trước lệnh tạo container. Không dùng lại volume hoặc ghi stack acceptance đang chạy ở 15432/16379/18081.
2. Core demo dùng schema/migration hiện có trên DB trống riêng, fixture phòng synthetic qua Core thật; không sửa seed/master/migration hiện hữu. Khách sạn pilot không có room type. Nếu cần dòng pilot hotel 4: soạn SQL chính xác một dòng dựa trên schema thật sau bootstrap, dùng thông tin công khai đã duyệt; trình lại trước INSERT theo điều kiện cũ của người dùng.
3. Quyền app Core chỉ đọc; RAG writer chỉ trong database RAG riêng, không superuser/DDL/ghi Core. Bí mật sinh ngẫu nhiên trong thư mục riêng với NTFS ACL. Keycloak realm/client demo riêng, không đổi realm/client đang dùng. Cần Member 2 duyệt cả phần role/grants/IAM trước khi tạo.
4. Schema RAG tái dùng DDL hiện có `20261030-230000-create-rag-pgvector-store.sql`, SHA256 `3145b2e6b46917d94aae539ef2336d7c2bcf1861eba84ec0678cbee971d9f2f6`. Chưa chạy DDL hoặc Liquibase. Backup được tạo, kiểm khả năng đọc/restore trước thay đổi và trước ingest. Không xóa volume/DB cũ.
5. Model/dimension chưa chốt. Chỉ pin sau kiểm compatibility và quota; khóa policy chi phí chung R1/RAG <=5 USD trước live. Chưa bật provider hoặc tạo embedding Gemini. Chưa chỉnh launcher cũ vốn khóa acceptance hotel 1–2, chưa sửa file IAM/Core/DB hoặc cấu hình shared khác để né gate.

## Dữ liệu và bằng chứng đã có

Pack `eval/t12/pilot-halong-v1`: 5 tài liệu công khai, 20 RAG cases (10 grounded lặp 3 lần, 6 insufficient, 4 dynamic), 12 đặc tả kịch bản R1. Hash corpus đối chiếu nguồn đã ACTIVE trước đó, scope HOTEL_SCOPED/hotelId4/version1. Không kế thừa approval offline-local embedding thành approval gửi provider; cần khóa cấu hình/corpus/phạm vi live cụ thể khi mở gate đó. Review ngữ nghĩa và chạy application/live cho pack mới: NOT RUN.

PASS_DATA_INTEGRITY_ONLY: 5 documents/20 cases/12 R1 specifications; 5 tamper variants bị từ chối; 12 frozen evaluation files giữ nguyên. Backend regression nền mới 305 PASS/1 calibration skipped/0 fail; frontend build PASS. Không lấy các kết quả này thay bằng chứng IAM/DB/live.

## Điều kiện dừng và thu hồi

Theo `AGENTS.member1.md`, shared DB/security/IAM cần owner review. Skill `m1-rag-knowledge-lifecycle` yêu cầu: “Member 2-approved pgvector extension, Liquibase/schema/index/grants, backup, rollout, and rollback plan.” Đây là gate tiếp theo chưa có phê duyệt, không phải thiếu dữ liệu có thể tự bù bằng fixture.

Sau khi được duyệt đúng target, giữ state ownership cho container/process và snapshot. Dừng/restore chỉ đối tượng do run này tạo. Active RAG version không được xóa qua API delete; không tắt guard. Rollback app bằng tắt provider hoặc ngừng runtime mới, giữ kho local cũ. Commit/push/merge vẫn cần lệnh riêng.
