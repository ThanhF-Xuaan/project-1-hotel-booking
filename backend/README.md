# Backend và chatbot RAG

Code base Java 21/Spring Boot 4 theo modular monolith. Hai luồng đã có: tra cứu cơ sở/hạng phòng/khả dụng/báo giá; quản lý tài liệu tri thức và chat một lượt có citation. Booking, hold, check-in/out và VNPay chưa nằm trong code base này.

## Chạy ở môi trường mới

1. Sao chép `.env-example` thành `.env`, đặt `DB_PASSWORD` và `KC_ADMIN_PASSWORD` cho máy local. Không commit `.env` hoặc API key.
2. Chạy `docker compose up -d --build`. Compose dùng volume mới `pg_vector_data`, không gắn volume PostgreSQL `pg_data` của cấu hình cũ. Flyway tạo schema từ `V001` và các migration tiếp theo; mặc định không nạp dữ liệu demo. Keycloak import realm `hotel-realm` với browser client `hotel-web` (Authorization Code + PKCE) và audience `hotel-backend`.
3. Mở `http://localhost:8080/v3/api-docs`. Các API `/api/v1/public/**` không cần JWT; `/api/v1/**` còn lại cần JWT nhân viên Keycloak và permission/scope trong PostgreSQL.
4. Khi cần dữ liệu demo, nạp lần lượt `database/04-...sql` đến `database/09-seed-ai-permissions.sql` **một lần** lên database local mới. Tạo user trong Keycloak, rồi tạo dòng `staffs` với `keycloak_id` bằng `sub` của user, `role_id` và `scope_type/scope_entity_id` đúng phạm vi. Realm export không chứa tài khoản hay mật khẩu nhân viên.

**Database hiện có:** `V001` là snapshot từ các script `01`, `02`, `03`, không được áp thẳng lên database đã có bảng. Trước khi nâng cấp phải backup, so schema thực tế với snapshot trên bản sao và baseline Flyway có kiểm chứng. Compose đổi từ `postgres:16-alpine` sang `pgvector/pgvector:0.8.6-pg16-trixie`; không gắn image mới vào volume cũ khi chưa thử dump/restore và kiểm tra collation. Không dùng `docker compose down -v` để xử lý lỗi migration.

Chạy backend trực tiếp trên Windows: dùng JDK 21, PostgreSQL 16 có pgvector, đặt `DB_URL`, `DB_USER`, `DB_PASSWORD`, `KEYCLOAK_ISSUER_URI` và `KEYCLOAK_JWK_SET_URI`. JVM cần timezone PostgreSQL nhận được, ví dụ `-Duser.timezone=Asia/Ho_Chi_Minh` (alias `Asia/Saigon` trên một số máy không được PostgreSQL chấp nhận). `GET /actuator/health` và `/actuator/health/readiness` dùng cho kiểm tra khởi động, không gọi provider AI.

## AI

Mặc định `AI_ENABLED=false`: backend vẫn khởi động, endpoint AI trả `503`. Để dùng provider thật, đặt `AI_ENABLED=true`, `AI_CHAT_MODEL_PROVIDER=openai`, `AI_EMBEDDING_MODEL_PROVIDER=openai` và `OPENAI_API_KEY` ở môi trường chạy. Model embedding mặc định `text-embedding-3-small`, 1536 chiều, phải khớp migration `V002`. Đổi model/dimension cần migration vector mới; không chỉ đổi biến môi trường.

Tạo tài liệu qua `POST /api/v1/ai/knowledge/documents`; worker lấy job lưu bền trong DB, chia đoạn và tạo embedding. Kiểm tra `GET /api/v1/ai/knowledge/jobs/{id}`; sau khi `SUCCEEDED`, gửi `PATCH /api/v1/ai/knowledge/documents/{id}` với `{"action":"PUBLISH"}`. `REVOKE` khiến nguồn ngừng được truy xuất ngay. Chat ở `POST /api/v1/ai/chat`; `roomQuery` tùy chọn trả thẻ báo giá từ cùng pricing service với REST, không để model tự tính tiền. Chỉ có chat một lượt; không có hội thoại lưu trữ.

Permission mới `USE:AI_ASSISTANT`, `VIEW/CREATE/PUBLISH/REVOKE:KNOWLEDGE` được gán cho các role mặc định qua migration nếu role đã có, hoặc qua seed `09` nếu nạp role demo sau Flyway. Global document chỉ quản trị cấp `CHAIN`; tài liệu theo cơ sở tuân theo scope của nhân viên.

## Kiểm tra

Tra cứu/quote và `roomQuery` của AI nhận **1–30 đêm** theo `[checkIn, checkOut)`; vượt giới hạn trả HTTP `400`, mã `1002`. Mỗi quote đọc 4 nhóm rule theo cả khoảng ngày, rồi chọn rule theo từng đêm. Availability không phụ thuộc vào trạng thái `OCCUPIED`/`CLEANING` hiện tại; slot, lịch bảo trì, counter và cờ đóng phòng `MAINTENANCE` vẫn được kiểm tra. Chi tiết tại [ADR 0002](../docs/adr/0002-stay-limits-and-sale-availability.md).

Lỗi giao thức HTTP giữ status/header của Spring và envelope `code/message/result`: `405` dùng mã `1010` kèm `Allow`, `415` dùng `1011`, `406` dùng `1012`, route không tồn tại trả `404`/`1004`. Lỗi xử lý ngoài dự kiến vẫn trả `500`/`9999`. Với `Accept` không hỗ trợ định dạng JSON, client không nên phụ thuộc vào body của lỗi `406`.

```powershell
cd backend
.\mvnw.cmd test     # Unit và context test; không gọi provider trả phí
.\mvnw.cmd verify   # Thêm Testcontainers PostgreSQL 16 + pgvector; cần Docker
```

Integration test kiểm tra migration mới, quote tính tay, availability theo slot, ingest → publish → chat bằng model giả, lọc scope và revoke. Test không chứng minh API provider thật vì cần key bên ngoài; khi cấu hình key, nên chạy smoke riêng trên tài liệu thử và kiểm soát chi phí. Contract và chính sách giá trong [ADR 0001](../docs/adr/0001-pricing-quote-policy.md).

## Mở rộng

Thêm module nghiệp vụ dưới `vn.edu.utc.hotel_booking/<module>`, giữ controller, service và truy cập dữ liệu cùng module. Chỉ gọi module khác qua service công khai; không truy cập repository của module khác. Với bảng hoặc cột mới, thêm migration `V004__...sql` (kiểm tra version mới nhất trước khi đặt số), không sửa migration đã chạy. Thêm endpoint vào `/api/v1`, trả envelope `code/message/result`, kiểm tra permission và scope trên tài nguyên server đọc từ DB, rồi thêm test trên PostgreSQL thật nếu query phụ thuộc SQL.

Thêm AI tool chỉ đọc trong `aiassistant/tool`: truyền actor từ JWT đã đối chiếu với `staffs`, kiểm tra quyền trong service sở hữu nghiệp vụ và dùng lại DTO kết quả của REST. Không cho model tự chọn actor hoặc gọi repository/SQL. Cập nhật tri thức bằng cách tạo document, chờ job ingest thành công rồi publish; tài liệu đã publish muốn thay nội dung cần một version/job mới qua migration/API tiếp theo, hiện chưa hỗ trợ sửa tại chỗ. Không dùng tài liệu chứa dữ liệu cá nhân làm fixture AI.
