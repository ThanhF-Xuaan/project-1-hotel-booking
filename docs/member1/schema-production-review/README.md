# Module 9–10 — bản sửa schema để review

Trạng thái: **PROPOSED**. Phần SQL đã được sửa theo source; triển khai production vẫn **BLOCKED**.

## Phạm vi và nguồn đối chiếu

- Repo: `https://github.com/ThanhF-Xuaan/project-1-hotel-booking.git`.
- Branch chứa PR này: `codex/member1-schema-master-data-review-20261106`.
- Source ban đầu được đối chiếu tại HEAD `63a26ba4c81c92e9892383de29bf76bc96b617b9`; PR được tạo từ `origin/develop` HEAD `7d83d2620ec8387b17e638c431a418ac35a52115`.
- Chưa xác minh SHA đang chạy trên production.
- Member 1 chuẩn bị bản sửa T04/T05/U14/T12; Member 2 duyệt shared schema, IAM, Liquibase và môi trường DB.
- Chỉ sửa 10 bảng Module 9–10 trong file bên ngoài repo. SQL của Module 1–8 được giữ nguyên; không xác nhận tính tương thích của các module đó.

## File bàn giao

| File | Nội dung |
|---|---|
| `hotel_booking_schema_tables.sql` | 77 định nghĩa bảng; Module 9–10 đã sửa. Chỉ là schema tham khảo cho DB mới. |
| `hotel_booking_member1_supporting_objects.sql` | 15 index và 9 trigger cho Module 9–10, gồm 6 partial unique index. |
| `hotel_booking_member1_schema_checks.json` | Kết quả kiểm tra tĩnh và SHA-256 của SQL, bản gốc. |
| `hotel_booking_schema_tables.before-member1-006b4b398141.sql.bak` | Bản gốc nguyên byte trước khi sửa. |

**CONFIRMED:** Đây là DDL, không phải master-data. Không ghép CREATE TABLE vào `04-master-data.sql`. Các bản ghi đồng hồ, chỉ số, lương, binding, consent và nội dung RAG là dữ liệu nghiệp vụ; không tạo mẫu để nạp production. Không tự đặt embedding model/dimension hoặc cấp quyền mới.

## Nội dung đã sửa

- T04: dùng identity, `location_label`, `recorded_by`, `updated_by`, `NUMERIC(12,3)` và enum ELECTRICITY/WATER theo source. Không cascade xóa chỉ số theo đồng hồ. Unique của mã đồng hồ và chỉ số theo ngày chỉ áp dụng bản ghi chưa xóa mềm.
- T05: dùng `bonus` theo entity; bỏ các cột hotel/payment chưa có contract. Bổ sung partial unique của staff/kỳ lương từ migration đã có. Không tự thêm luật tính lương hoặc CHECK chưa có trong source.
- U14: dùng issuer/subject, binding version, FK `booking_guests`, consent theo hotel/purpose/revision và event theo version. Giữ nguyên giới hạn `SYNTHETIC_FIXTURE`; không mở quyền dùng danh tính khách hàng thật.
- T12: dùng UUID cho document/chunk, scope HOTEL_SCOPED/CHAIN_PUBLIC, phiên bản và ngày hiệu lực, vector embedding, ràng buộc model/dimension, một document active trên mỗi source. Không lấy giá hoặc tồn phòng từ RAG.
- Tách index/trigger ra file phụ để giữ file chính chỉ có định nghĩa bảng. Trigger dùng hàm `update_updated_at_column()` hiện có; không sao chép hoặc thay đổi hàm dùng chung.
- Các bảng Member 1 dùng CREATE TABLE không có IF NOT EXISTS: DB đã có bảng phải báo lỗi thay vì che giấu sai lệch schema. Đây không phải script sửa DB hiện hữu.

Nguồn: các SQL incremental `20261003-150000-create-utility-tables`, `20261004-120000-create-payroll-summary`, `20261017-200000-fix-utility-reading-soft-delete-unique`, `20261018-120000-add-payroll-summary-unique-index`, `20261026-160000-create-u14-local-binding-consent`, `20261030-230000-create-rag-pgvector-store`; đối chiếu thêm entity T04/T05 và RAG store/configuration.

## Các điều kiện chặn production

1. **CONFIRMED — IAM ngoài phạm vi chưa tương thích.** File tổng hợp có `permissions(name,module,...)`; master-data trong repo dùng `permissions(action,resource,status)`. File tổng hợp có `roles(name,description,...)`; master-data dùng `roles(code,name,status)`. Nạp master-data hiện có vào schema này sẽ thiếu cột. Member 2 cần đối chiếu toàn bộ Module 1–8 với SHA ứng dụng đích; không chỉ hai ví dụ IAM này.
2. **CONFIRMED — U14 hiện là local synthetic-only.** Cần contract riêng cho binding khách hàng thật, consent, quyền truy cập và retention trước khi bật tính năng cho dữ liệu thật. Bản sửa không gỡ CHECK bảo vệ synthetic.
3. **CONFIRMED — RAG store hiện giới hạn local.** `RagVectorStoreConfiguration` chỉ nhận URL loopback với DB `t12_pgvector_*` và dùng datasource riêng; profile local tắt Liquibase. Gộp các bảng RAG vào DB nghiệp vụ không tự làm ứng dụng dùng được chúng. Cần duyệt topology, datasource, scope catalog, pgvector, quyền DB, model/dimension và lifecycle trước production. Giữ context `@rag-pgvector` là opt-in theo changelog gốc.
4. **UNKNOWN — môi trường và lịch sử DB.** Chưa có target DB, bản backup được xác nhận, DATABASECHANGELOG, dữ liệu hiện hữu hoặc SHA ứng dụng production. Không chạy bộ tổng hợp lên DB đang có dữ liệu; không dùng changelogSync để bỏ qua migration.
5. **CONFIRMED — seed mặc định có rủi ro nạp dữ liệu demo.** `application.yaml` mặc định context `master-data,dev-seed`. Môi trường production cần context được owner kiểm tra, loại `dev-seed`; không tự bật `rag-pgvector`. Cấu hình này chưa được thay đổi trong task.

## Tích hợp sau khi duyệt

- Với DB đã được Liquibase quản lý: dùng lịch sử migration hiện có; chỉ tạo migration incremental cho delta thực tế đã được Member 2 duyệt. Không thay baseline hay tạo lại các bảng đã tồn tại.
- Với DB mới: ưu tiên chạy changelog gốc đã review, không chạy đồng thời snapshot này rồi toàn bộ migration tạo bảng. File tổng hợp hiện là đầu vào review; cần xử lý các blocker trước khi chọn làm baseline mới.
- Chỉ tách DML master-data khi đã có danh mục/ma trận quyền được duyệt và schema IAM tương thích. Task này không thêm DML hoặc tự cấp quyền.
- Kiểm tra PostgreSQL/pgvector trên staging được duyệt; kiểm tra schema ứng dụng, import lương, trùng bản ghi active, xóa mềm, consent theo tenant và version, RAG active/effective/model/dimension. Parser không thay thế các kiểm tra runtime này.
- Trước deploy, xác nhận backup và thử restore; owner duyệt thứ tự chạy và phương án rollback. Không chạy rollback DROP TABLE của RAG trên DB chứa dữ liệu thật khi chưa duyệt mất dữ liệu.

## Kiểm chứng thực tế

| Kiểm tra | Kết quả |
|---|---|
| Python + pglast parse toàn file: 77 CREATE TABLE, không có DML | PASS |
| AST của 10 bảng khớp migration local, áp dụng bản sửa unique reading và bỏ IF NOT EXISTS có chủ đích | PASS |
| Parse file phụ: 15 index, 9 trigger; cột index tồn tại, 6 partial unique đúng tên | PASS |
| 120 FK: bảng/cột tham chiếu tồn tại, không tham chiếu tới bảng tạo sau | PASS |
| SQL Module 1–8 không bị sửa; SHA-256 bản gốc được lưu | PASS |
| Validator tiếng Việt (`validate_copy.py --register eng-impersonal`) | PASS — 0 lỗi, 0 cảnh báo |
| Chạy trên PostgreSQL, extension, grants, ràng buộc thực tế và dữ liệu | NOT RUN — chưa có môi trường được duyệt |
| Maven/frontend tests | NOT RUN — không sửa source ứng dụng; các test này không xác minh schema tổng hợp trên DB |
| Liquibase update, Docker, deploy production | NOT RUN — chưa được duyệt môi trường và schema |

Parser pglast được cài vào thư mục TEMP riêng để kiểm tra; không đổi dependency repo. Không đọc credential hoặc gọi model/provider. Không thêm dữ liệu thật, log PII hoặc seed khách hàng/lương.

## Rollback và Git

Rollback file local: sau khi kiểm tra file đích không có thay đổi mới, khôi phục đúng nội dung từ bản `.sql.bak`. Các file phụ là artifact review mới. Không reset hoặc clean repo. Không có thay đổi DB cần rollback vì chưa chạy SQL.

Không tạo branch, commit, push, PR hoặc merge. File SQL nằm ngoài repo nên chưa được đưa lên Git. Cần Member 2 review schema và xác nhận môi trường trước khi thực hiện bước deploy.
