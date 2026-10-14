# Baseline audit — R1-00

## Git và bảo toàn công việc

- Repository: `D:\Project\University\project-1-hotel-booking`.
- Remote: `https://github.com/ThanhF-Xuaan/project-1-hotel-booking.git`.
- Branch ban đầu: `feature/T12-rag-chatbot`. Working tree có chỉnh sửa governance, hai file docs đang staged delete và root `AGENTS.md` chưa tracked; không clean/reset/copy các thay đổi này.
- HEAD lần kiểm tra đầu: `905550bd527673016bddca8bb8599681ae74bf09`; ngay trước fetch đã chuyển thành `e609b0d46fbaf45f3c6212444ef6b79a63c8bbfa` do hoạt động khác. R1-00 không tạo commit ở repository gốc.
- Đã `git fetch origin`, đối chiếu `origin/develop`: commit `0e6d379eba8ab6ac7cf3573728d92d97f3e659e6` (implement booking session management, room inventory holding, and public search features).
- Worktree riêng: `D:\Project\University\worktrees\hotel-t12-room-advisor`, branch `feature/T12-ai-room-advisor-v2`, đồng bộ trên nền `0e6d379`. Không mang commit `2dbecf2` và không sửa code Member 2/3/4.

## API tại commit đã audit (0e6d379)

Paths dưới đây tương đối với root. Chỉ xác minh source; `GET http://localhost:8080/v3/api-docs` không khả dụng. Chưa có runtime contract capture hoặc integration test.

| Nguồn | Kết luận |
|---|---|
| `backend/src/main/java/vn/edu/utc/hotel_booking/common/dto/ApiResponse.java` | Success `code=200`, payload `result`; trường null được bỏ. |
| `common/dto/PageResponse.java`, `BaseSearchDto.java` cùng package | PageResponse dùng `content/pageNumber/pageSize/totalElements/totalPages/first/last/empty`. Search dùng `page/pageSize/sortBy/sortDirection`, page bắt đầu 0, pageSize tối đa 100. |
| `modules/organization/controller/PublicHotelController.java` | Có `/api/v1/public/hotels`, `/filter`, `/{id}`. HotelSearchDto có regionId, keyword, status. |
| `modules/booking/controller/PublicBookingSearchController.java` | `POST /api/v1/public/booking/search` và `POST /api/v1/public/booking/price-quote` cung cấp tìm kiếm phòng và tính giá công khai. |
| `modules/booking/service/impl/PublicBookingSearchServiceImpl.java` | Kiểm tra phòng trống liên tục từ `checkInDate` đến `checkOutDate.minusDays(1)` (checkout-exclusive nights); tích hợp sẵn 6-Stage Pricing Pipeline. |
| `modules/pricing/pipeline/handler/TaxAndFeeHandler.java` | Tính 5% phí dịch vụ (`serviceFee = net * 0.05`) và 10% VAT (`vat = (net + serviceFee) * 0.10`). |
| `modules/pricing/dto/response/PriceBreakdownDto.java` | Trả về bóc tách đầy đủ: `totalRoomNetAmount`, `totalRoomServiceFee`, `totalRoomVat`, `totalRoomGrossAmount`, `preTaxAmount`, `vatAmount`, `finalTotalAmount`. |
| `modules/booking/dto/response/AvailableRoomTypeResponse.java` | Cung cấp sức chứa chi tiết: `standardAdults`, `standardChildren`, `maxAdults`, `maxChildren`, `maxInfants`, `extraBeds`, `amenities`, `inclusions`, `priceBreakdown`. |
| `modules/booking/controller/PublicBookingSessionController.java` | Quản lý draft booking session (`/api/v1/public/booking/sessions/*`), là thao tác ghi nghiệp vụ (CẤM T12 GỌI). |
| `modules/inventory/service/RoomInventoryHoldService.java` | Quản lý giữ/nhả phòng (`holdRooms`, `releaseRooms`, `batchHoldInventory`), là thao tác ghi nghiệp vụ (CẤM T12 GỌI). |
| `common/config/SecurityConfig.java:42-43` | `.requestMatchers("/api/public/**", "/api/v1/public/**").permitAll()`, tự động cho phép các endpoint `/api/v1/public/ai/room-advisor/*` công khai không cần sửa security config. |
| `frontend/src/App.tsx:35`, `frontend/src/modules/booking-portal/pages/SearchPage.tsx:29` | Có `/portal/search` và `/portal/checkout`. Action path T12 hỗ trợ `/portal/search?hotelId=...&hotelRoomTypeId=...` và `/portal/hotels/{id}/room-types/{id}`. |

Các path bắt đầu `modules/` và `common/` trong bảng đều nằm dưới `backend/src/main/java/vn/edu/utc/hotel_booking/`.

## Quy tắc đã đọc và điểm xung đột

Đã đọc root `AGENTS.md` tại checkout gốc, `AGENTS.member1.md`, `.agents/rules/07-member-1-architecture-pipeline.md`, core AI rules và bốn skill m1-spec-designer, m1-execution-planner, m1-ai-rag-developer, m1-quality-and-git-delivery. Đã đọc ancestor `D:\AGENTS.md` và skill viết tài liệu kỹ thuật tiếng Việt.

Lúc tạo worktree, root AGENTS.md chưa có và governance ở commit origin/develop còn quy tắc cũ vector-first, LLM tool calling và business writes có xác nhận. Yêu cầu trực tiếp cùng plan v2 đã duyệt có ưu tiên; R1-00 không sửa hoặc copy governance.

Sau đó người dùng đã thêm/sửa governance, skills và tài liệu T12, đồng thời xác nhận các thay đổi và yêu cầu tiếp tục task. Đã đọc lại AGENTS.md, AGENTS.member1.md, rules 00/01/07, bốn skill m1 cũ đã cập nhật, ba skill room-recommendation/evaluation/lifecycle mới, skill data-processing để xác định ngoài scope, ba agent review specs, ADR/contract tracker, roadmap/prompts và draft evaluation v2. Các file mới hiện có trong worktree nhưng **không thuộc change set R1-00**. Git HEAD của worktree vẫn tại SHA đã xác minh; file contents của governance chưa merged không được coi là baseline Git hoặc owner approval. Snapshot SHA256 trước/ sau continuation dùng để bảo vệ các file người dùng.

Các quy tắc mới đồng nhất với T12 v2. ADR/tracker vẫn có approval gates trước integration; quyền phát triển độc lập bằng fixtures của R1-00 đến từ yêu cầu trực tiếp của người dùng. CR-07 (provider/dependency), CR-08/09 (R2 pgvector/ingestion) không thành prerequisites cho task foundation này. Xem [governance alignment](governance-alignment.md).

`docs/ai/ARCHITECTURE_KNOWLEDGE_MAP.md` được ancestor đề cập nhưng không có tại baseline. Task không sửa shared source hoặc gọi repository của module khác; kiến trúc thực tế được audit trực tiếp. Không áp dụng tên package hoặc migration framework giả định trong tài liệu ancestor lên source hiện tại.
