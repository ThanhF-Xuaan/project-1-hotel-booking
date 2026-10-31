# T12-03 — persistent RAG local v1

## Quyết định và baseline

User là người quyết định, cho phép agent tự chọn phương án và thay đổi module liên quan. Các lựa chọn dưới đây được thực hiện theo ủy quyền đó, không giả xác nhận của Member 2. Chỉ synthetic/public fixture trong sandbox mới; không production hoặc Gemini embedding live.

Base vừa fetch: `20e06e5135def269b91531b3997b784b271e17c8`. Worktree managed riêng `m1-t12-pgvector`, sạch trước task. T12-02 là patch độc lập chưa merge, giữ nguyên ở worktree provider. Hai tài liệu widget và sáu file provider được bảo vệ bằng hash/status.

CONFIRMED: RagVectorStorePort có lifecycle stage/activate/rollback/delete và đọc candidate/getDocument. Retrieval hiện xét ACTIVE/SUPERSEDED theo ngày rồi chọn version thắng theo scope/status/effectiveFrom/version; giữ nguyên semantics này để hỗ trợ lịch sử và future-effective replacement. Không gọi mọi superseded chunk là nội dung đang active.

CONFIRMED: RagEmbeddingPort hiện trả model `local-multilingual-hashing-v1`, 256 chiều, L2 normalized. Pgvector PoC chạy trên image `pgvector/pgvector:0.8.6-pg16-trixie`, PostgreSQL 16, loopback15434; extversion0.8.6 và vector roundtrip/distance đạt. Đây không phải PoC Gemini/Spring AI.

## Thiết kế và acceptance

- Mặc định in-memory. Opt-in `hotel.ai.rag.store-type=pgvector`; credential JDBC riêng chỉ dùng cho adapter RAG, không thay primary DataSource.
- Migration incremental XML/SQL với context `@rag-pgvector`: mặc định và context master-data không tạo bảng RAG. Extension giữ lại khi rollback, tránh xóa tài nguyên có thể dùng chung. Chỉ chạy trên volume mới `t12-rag-pgvector-20261030-data` đã kiểm tra DB trống và snapshot.
- Bốn bảng: cấu hình model/dimension; source theo scope/hotel/sourceId; version tài liệu; chunks với cột vector thật. B-tree theo scope/source/FK và unique ACTIVE theo source; chưa ANN index. Query scope/date trước trả candidate, similarity Java giữ nguyên; không tuyên bố SQL ANN search hoặc SLA production.
- Dimension lấy từ port, không copy dimension provider khác. Pin model/dimension trong config; mismatch bị chặn trước khi đọc corpus. Bean/kết nối được khởi tạo khi RAG được dùng, không làm R1 phụ thuộc việc mở DB RAG lúc startup. Doc/chunk metadata, SHA-256 và finite vectors phải khớp; stage cả version/chunks trong một transaction, duplicate version bị từ chối và không tạo thêm row.
- Khóa row source khi stage/activate/rollback/delete; at most one ACTIVE do partial unique index. Chuyển trạng thái document/chunks thống nhất bằng join. Không xóa ACTIVE trực tiếp. Rollback đúng source/scope/version; `clearAll` không được hỗ trợ trên persistent store.
- HOTEL_SCOPED cần hotel1..32767 tồn tại; CHAIN_PUBLIC cần hotel null. Source ID chuẩn hóa trim/uppercase, pattern `[A-Z0-9][A-Z0-9._:-]{0,127}`. Không nhận SQL/URL từ request. Scope null chỉ có nghĩa hotel hiện tại + chain-public ở candidate read; không cho mutation không rõ scope.
- Chỉ corpus đã qua governance validator; không identity/payroll/booking/live price. Bound trước embedding: content64.000 ký tự, title500, source128 và256chunks. Adapter giới hạn payload và tối đa1.000candidate, quá cap trả dependency failure thay vì cắt âm thầm.
- Reader role chỉ SELECT RAG; writer role SELECT/INSERT/UPDATE/DELETE trên bốn bảng RAG và USAGE/SELECT trên sequence của chúng. Database sandbox thu hồi PUBLIC privileges rồi cấp CONNECT cho hai role; không TEMP, CREATE trong public, hoặc quyền bảng Booking/Pricing/Inventory. Role provisioning chỉ ở sandbox; không tạo credential production trong migration.
- Demo corpus initializer không được bật cùng persistent mode; không tự seed/reset dữ liệu khi restart. Profile local mặc định demo false, không tự chạy Liquibase.
- Kiểm thực: model/vector PoC, transaction rollback, duplicate/concurrent lifecycle, scope/date isolation, failed replacement giữ active, restart JVM/container, model mismatch, least privilege, snapshot/restore và rollback DDL. Test Memory/Advisor/RAG mặc định giữ hoạt động.

## Allowlist và giao hàng

Thêm PostgresPgVectorStore, RagVectorStoreConfiguration, profile local, incremental XML/SQL, unit/IT test và report. Sửa annotation chọn InMemoryScopedVectorStore, boundary limits ở RagKnowledgeLifecycleService và lazy injection tại ba consumer RAG; giữ route/DTO/ranking/gold/API/auth/dependencies. Không sửa migration đã áp hoặc Compose chung.

Sau kiểm chứng store ban đầu, tích hợp nguyên sáu file của patch T12-02 đã kiểm chứng vào worktree này. Canonical content khớp bản gốc, không sửa hoặc xóa source tại worktree provider. Regression cuối kiểm cả hai task trên cùng cây mã. Image thiếu alias Asia/Saigon: JVM local dùng tên chuẩn Asia/Ho_Chi_Minh; không thay version database hoặc múi giờ nghiệp vụ.

Nguồn: [pgvector](https://github.com/pgvector/pgvector), [Liquibase contexts](https://docs.liquibase.com/reference-guide/changelog-attributes/what-are-contexts). Hành vi opt-in phải được kiểm trên Liquibase4.31 thực tế, không chỉ dựa vào tài liệu. Rollback ứng dụng về in-memory cần operator chọn flag; không tự fallback khi persistent DB lỗi. Report phải phân biệt code/store restart với full application runtime và live-provider evaluation.
