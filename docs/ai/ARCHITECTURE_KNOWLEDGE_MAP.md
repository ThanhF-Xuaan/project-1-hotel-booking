# Bản đồ kiến trúc backend và AI

Ứng dụng là modular monolith Java 21/Spring Boot 4. PostgreSQL là nguồn dữ liệu nghiệp vụ; pgvector lưu embedding. Keycloak xác thực, `iam` đối chiếu nhân viên, role/permission và scope. Redis đã có cấu hình kết nối nhưng chưa được dùng cho cache hay quota của luồng mẫu.

| Module | Trách nhiệm hiện có | API Java được module khác dùng |
|---|---|---|
| `iam` | Giải JWT thành nhân viên đang hoạt động, kiểm tra permission/scope | `StaffAccessService` |
| `organization` | Danh mục khách sạn | `HotelCatalogService` |
| `property` | Hạng phòng bán tại khách sạn | `RoomTypeCatalogService` |
| `inventory` | Khả dụng theo khoảng `[checkIn, checkOut)` | `RoomAvailabilityService`, `StayCriteria` |
| `pricing` | Quy tắc giá, phụ thu, phí và VAT | `PricingQuoteService` |
| `audit` | Ghi sự kiện thay đổi tri thức không chứa nội dung tài liệu | `AuditService` |
| `aiassistant` | Ingest tài liệu, vector search theo quyền, chat và tool báo giá | Chỉ REST; không module nghiệp vụ nào phụ thuộc vào AI |

Tra cứu/báo giá nhận 1–30 đêm; pricing đọc rule theo cả khoảng lưu trú. Tồn bán tách khỏi trạng thái vệ sinh hiện tại, vẫn xét slot và lịch/cờ bảo trì theo [ADR 0002](../adr/0002-stay-limits-and-sale-availability.md).

Luồng tra cứu: controller → IAM và catalog/property → inventory → pricing → envelope HTTP. Luồng chat: controller → IAM → truy xuất vector đã publish và đúng scope → kiểm tra lại quyền nguồn → chat model; `roomQuery` đi qua `RoomQuoteTool` đến cùng `PricingQuoteService` mà REST sử dụng. Model không truy cập SQL và không tự tính giá. Ingest chạy worker riêng, dùng job PostgreSQL và transaction ngắn để claim/ghi kết quả; lời gọi embedding nằm ngoài transaction.

Các thay đổi schema đi qua Flyway `backend/src/main/resources/db/migration`. `V001` là snapshot schema cũ cho database **mới**, `V002` thêm pgvector/tri thức, `V003` cấp permission AI cho role đã có. Script seed demo nằm ở `database/04`–`09` và không tự chạy. Database có dữ liệu cũ phải được backup, so schema và baseline trên bản sao trước khi nâng cấp. Không thay migration đã phát hành.

HTTP: `/api/v1/public/**` cho tra cứu công khai; các đường dẫn còn lại cần JWT có issuer/audience đúng, sau đó kiểm tra nhân viên và permission/scope bằng dữ liệu PostgreSQL. Chat hiện chỉ dành cho nhân viên. Lỗi dùng envelope `code/message/result`. Không đặt authorization chỉ ở frontend hoặc trong prompt.

Chưa có booking/hold, thanh toán, check-in/out, audit trail cho các nghiệp vụ ngoài quản lý tri thức, Redis cache/quota, hội thoại nhiều lượt và provider AI thật trong CI. Các chức năng này phải đi qua service sở hữu domain khi triển khai; tránh tạo repository access chéo hoặc hai nguồn sự thật cho tồn phòng/giá.
