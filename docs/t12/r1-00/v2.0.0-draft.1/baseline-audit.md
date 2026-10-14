# Baseline audit — R1-00

## Git và bảo toàn công việc

- Repository: `D:\Project\University\project-1-hotel-booking`.
- Remote: `https://github.com/ThanhF-Xuaan/project-1-hotel-booking.git`.
- Branch ban đầu: `feature/T12-rag-chatbot`. Working tree có chỉnh sửa governance, hai file docs đang staged delete và root `AGENTS.md` chưa tracked; không clean/reset/copy các thay đổi này.
- HEAD lần kiểm tra đầu: `905550bd527673016bddca8bb8599681ae74bf09`; ngay trước fetch đã chuyển thành `e609b0d46fbaf45f3c6212444ef6b79a63c8bbfa` do hoạt động khác. R1-00 không tạo commit ở repository gốc.
- Đã `git fetch origin`, đối chiếu `git ls-remote origin refs/heads/develop` và `origin/develop`: cùng `afbac57ae463ee2dc8705a2b3e9a7a8b5fca85bb` lúc tạo worktree ngày 08/10/2026.
- Worktree riêng: `D:\Project\University\worktrees\hotel-t12-room-advisor`, branch `feature/T12-ai-room-advisor-v2`, HEAD bắt đầu clean tại đúng SHA trên. Không cherry-pick hoặc copy implementation T12 cũ. Các worktree governance/skills hiện có không bị sử dụng hoặc sửa.

## API tại commit đã audit

Paths dưới đây tương đối với root. Chỉ xác minh source; `GET http://localhost:8080/v3/api-docs` không khả dụng. Chưa có runtime contract capture hoặc integration test.

| Nguồn | Kết luận |
|---|---|
| `backend/src/main/java/vn/edu/utc/hotel_booking/common/dto/ApiResponse.java` | Success `code=200`, payload `result`; trường null được bỏ. |
| `common/dto/PageResponse.java`, `BaseSearchDto.java` cùng package | PageResponse dùng `content/pageNumber/pageSize/totalElements/totalPages/first/last/empty`. Search dùng `page/pageSize/sortBy/sortDirection`, page bắt đầu 0, pageSize tối đa 100. |
| `modules/organization/controller/PublicHotelController.java` | Có `/api/v1/public/hotels`, `/filter`, `/{id}`. HotelSearchDto có regionId, keyword, status; không có contract tìm thành phố/toạ độ. |
| `modules/inventory/controller/HotelRoomTypeController.java` | `/api/v1/inventory/hotel-room-types/{id}` và `/filter` yêu cầu authentication. Chưa có public discovery/detail. Không sử dụng admin endpoint làm API public. |
| `modules/inventory/dto/response/HotelRoomTypeResponse.java` | Có hotelId, HotelRoomType ID, sức chứa, extraBeds, features, basePrice, status. Không có room image hoặc bed description. basePrice không phải giá báo cho chuyến đi. |
| `modules/inventory/controller/PublicAvailabilityController.java` | Có public `/availability/search` đọc; cùng controller còn `/hold` và `/release`, đều cấm với AI. Không cấp broad service/tool cho AI. |
| `modules/inventory/service/impl/RoomAvailabilityServiceImpl.java:54` | Khoảng startDate/endDate bao gồm hai đầu. Stay `[checkIn,checkOut)` phải gọi endDate = checkOut trừ một ngày. availableCount phải đủ trên mọi đêm. |
| `modules/pricing/controller/PublicPricingController.java` | Public POST `/api/v1/public/pricing/calculate`. Request có HotelRoomType ID, ngày, adults, children, extraBeds, campaignCode; không có roomsCount hoặc currency. |
| `modules/pricing/dto/response/PriceBreakdownDto.java` | Có dailyPrices, preTaxAmount, vatPercent, vatAmount, finalTotalAmount. Không có currency/serviceFeeAmount/chargeCoverage. Description “đã gồm thuế & phí” chưa đủ để chứng nhận all-in. |
| `modules/pricing/service/impl/PriceEngineImpl.java:81` | Checkout exclusive. Lines 193–194 tính VAT và cộng vào preTax. Không thấy áp dụng hotel.serviceFeePercent. Không suy diễn fee bằng 0. |
| `modules/pricing/service/impl/PriceEngineImpl.java:52` | Kiểm tra tổng khách và extraBeds; không thay thế kiểm tra riêng maxAdults/maxChildren tại discovery. Chưa thấy xử lý campaignCode hoặc tính phụ thu trẻ em riêng. |
| `common/config/SecurityConfig.java:42` | `/api/v1/public/**` permitAll; các endpoint advisor và cơ chế session mới chưa được cấu hình. Không suy ra tenant authorization từ permitAll. |
| `frontend/src/App.tsx:35`, `frontend/src/modules/booking-portal/pages/SearchPage.tsx:29` | Có search/checkout/my-bookings; chưa có room detail route. SearchPage dùng mockOffers. Route detail trong contract là đề xuất, không phải link đang chạy. |

Các path bắt đầu `modules/` và `common/` trong bảng đều nằm dưới `backend/src/main/java/vn/edu/utc/hotel_booking/`.

## Quy tắc đã đọc và điểm xung đột

Đã đọc root `AGENTS.md` tại checkout gốc, `AGENTS.member1.md`, `.agents/rules/07-member-1-architecture-pipeline.md`, core AI rules và bốn skill m1-spec-designer, m1-execution-planner, m1-ai-rag-developer, m1-quality-and-git-delivery. Đã đọc ancestor `D:\AGENTS.md` và skill viết tài liệu kỹ thuật tiếng Việt.

Lúc tạo worktree, root AGENTS.md chưa có và governance ở commit origin/develop còn quy tắc cũ vector-first, LLM tool calling và business writes có xác nhận. Yêu cầu trực tiếp cùng plan v2 đã duyệt có ưu tiên; R1-00 không sửa hoặc copy governance.

Sau đó người dùng đã thêm/sửa governance, skills và tài liệu T12, đồng thời xác nhận các thay đổi và yêu cầu tiếp tục task. Đã đọc lại AGENTS.md, AGENTS.member1.md, rules 00/01/07, bốn skill m1 cũ đã cập nhật, ba skill room-recommendation/evaluation/lifecycle mới, skill data-processing để xác định ngoài scope, ba agent review specs, ADR/contract tracker, roadmap/prompts và draft evaluation v2. Các file mới hiện có trong worktree nhưng **không thuộc change set R1-00**. Git HEAD của worktree vẫn tại SHA đã xác minh; file contents của governance chưa merged không được coi là baseline Git hoặc owner approval. Snapshot SHA256 trước/ sau continuation dùng để bảo vệ các file người dùng.

Các quy tắc mới đồng nhất với T12 v2. ADR/tracker vẫn có approval gates trước integration; quyền phát triển độc lập bằng fixtures của R1-00 đến từ yêu cầu trực tiếp của người dùng. CR-07 (provider/dependency), CR-08/09 (R2 pgvector/ingestion) không thành prerequisites cho task foundation này. Xem [governance alignment](governance-alignment.md).

`docs/ai/ARCHITECTURE_KNOWLEDGE_MAP.md` được ancestor đề cập nhưng không có tại baseline. Task không sửa shared source hoặc gọi repository của module khác; kiến trúc thực tế được audit trực tiếp. Không áp dụng tên package hoặc migration framework giả định trong tài liệu ancestor lên source hiện tại.
