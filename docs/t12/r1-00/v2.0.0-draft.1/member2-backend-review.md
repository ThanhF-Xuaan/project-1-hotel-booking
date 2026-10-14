# Member 2 — Backend Contract Review

**Version:** `2.0.0-draft.1`. **Human approval:** PENDING; reviewer/chữ ký/ngày: chưa có. Approval plan và R1-00 không phải approval public APIs, Security hoặc Pricing semantics. Tài liệu để Member 1 trao đổi; chưa gửi thông báo hoặc sửa module Member 2.

Đối chiếu [tracker của người dùng](../../../T12-contract-requests.md): M2-01 → CR-01/04; M2-02..05 → CR-03; M2-06 → CR-06/10; M2-07 → CR-02; M2-08 → CR-03/10. Đây là bản chi tiết cho cùng các request, không phải approval tracker thay thế. CR-07 chưa thực hiện ở R1-00; CR-08/09 là R2, không chặn foundation R1.

## Contract thực tế và đề xuất

| Contract | Baseline | Đề nghị Member 2 duyệt |
|---|---|---|
| Hotel discovery | Public Hotel filter có regionId/keyword/status và pagination | Xác nhận publication/active/not-deleted và mapping locationKeyword; không dùng phone keyword làm tìm thành phố ngầm. |
| Public HotelRoomType discovery | Chỉ admin/authenticated `/inventory/hotel-room-types/filter` | Proposed POST `/api/v1/public/hotel-room-types/filter`: hotelId hoặc locationKeyword; page/pageSize, sortBy=id, sortDirection=ASC; safe projection theo `core.schema.json#/$defs/PublicRoom`. Endpoint chưa tồn tại. Nếu dùng endpoint khác, cập nhật draft trước integration. |
| Public detail | Chưa có | Proposed GET `/api/v1/public/hotel-room-types/{id}`; validate published hotel + room active/not-deleted, ID liên kết hotel trong detail UI, không internal inventory/audit/guest fields. |
| Availability | POST `/api/v1/public/availability/search`, inclusive date endpoints | Xác nhận `availableCount` sau booked/locked/ooo, fallback ngày chưa có row, scope/active checks, consistency và lỗi. Adapter gọi `[checkIn, checkOut-1]`, từng đêm ≥1. Không được gọi hold/release. |
| Pricing | POST `/api/v1/public/pricing/calculate`, checkout exclusive | Xác nhận currency authoritative, VAT, service fee và charge coverage; không coi Swagger description là đủ. Không đổi công thức Pricing Engine trong PR Member 1. |

Request discovery proposed:

```json
{"locationKeyword":"Synthetic City","page":0,"pageSize":20,"sortBy":"id","sortDirection":"ASC"}
```

Response sử dụng wrapper `code/message/result`, PageResponse `content/pageNumber/pageSize/totalElements/totalPages/first/last/empty`. Public projection proposed giữ HotelRoomType ID, hotelId/name, roomTypeId/code/name, maxAdults/maxChildren/maxTotalGuests, extraBedsAllowed, features và status ACTIVE. Không yêu cầu basePrice để rank hoặc totalQuantity để hiển thị.

## Các quyết định bắt buộc

| ID | Quyết định cần xác nhận | Nếu chưa duyệt |
|---|---|---|
| M2-01 | Public room endpoints/projection, stable pagination, status/soft-delete/publication semantics, visibility scope từ server | Development dùng fixtures; chặn live integration. |
| M2-02 | Nguồn currency: field response hoặc metadata/config đã xác nhận; có phải mỗi hotel một currency? | Currency null; không chứng nhận hard budget currency. |
| M2-03 | finalTotalAmount bao gồm VAT nào, service fee/mandatory fees nào, rounding; giá thời điểm nào và quote validity | `serviceFeeStatus=UNKNOWN`, fee amount null; không dùng từ all-in hoặc tự thêm phí. |
| M2-04 | Daily netAmount trước VAT; có authoritative all-in nightly breakdown cho EACH_NIGHT không? | Không PASS hard EACH_NIGHT khi chỉ có pre-tax breakdown. |
| M2-05 | Capacity/children/infants/extra bed: ngoài maxTotal còn maxAdults/maxChildren; trẻ em chưa phân loại tuổi; extra bed không tự tăng capacity | MVP adults/children hiện có; hỏi lại khi không đủ dữ liệu; không tự sửa business rules. |
| M2-06 | Hotel/room active scope và ownership cơ chế guest/authenticated, cookie/CSRF, TTL, rate limits, session 404/409 | Security proposal pending; không sửa Security source ở R1-00. |
| M2-07 | Availability inclusive, stale reads/sparse rows, booking concurrency có thể thay đổi sau observation | UI ghi cần revalidate; assistant không đảm bảo inventory hoặc giữ phòng. |
| M2-08 | Core error HTTP/code semantics, timeout/retry policy chỉ cho reads, field null/omitted; campaignCode có được hỗ trợ thực tế? | Draft error envelope riêng cần align; không quảng cáo campaignCode đã có hiệu lực. |

`BACKEND_QUOTED_TOTAL`, AVERAGE_PER_NIGHT và EACH_NIGHT là semantics đề xuất để review; không tự reinterpret lời nói “mỗi đêm” nếu chưa rõ coverage. Với “900k mỗi đêm”, CHAT phải hỏi coverage/basis khi mơ hồ; không ngầm coi toàn bộ là all-in.

CR-06 cần chốt cả bootstrap/cấp owner credential trước CHAT/SEARCH mới, anonymous cookie versus Keycloak principal, và lỗi 401/AUTH_REQUIRED khi credential thiếu/sai. Chưa có endpoint cấp credential hoặc guest advisor access trên baseline. SessionId thiếu có thể tạo conversation mới chỉ sau khi owner đã được xác thực theo contract được duyệt.

## Member 1 có thể thực hiện độc lập

Schema/DTO validation, fixture adapters, deterministic filtering/ranking, multi-turn state, negative security tests và evaluation runner dùng synthetic inputs. Adapter interfaces không expose broad Core services hoặc repositories. Fixture nằm trong test resources; không làm production mock fallback. Các task sau chỉ bắt đầu khi người dùng duyệt riêng.

## Gate trước integration

- [ ] M2-01..08 có Approved/Rejected/Changes Requested, người duyệt, ngày và contract version.
- [ ] Runtime OpenAPI capture cùng environment/build SHA; đối chiếu source-shaped fixtures với response thật.
- [ ] Consumer contract tests chứng minh checkout semantics, ID/date/currency binding, safe public projection và pagination.
- [ ] Hard budget không PASS nếu currency/fee scope thiếu; finalTotal giá Backend được giữ nguyên.
- [ ] Ownership/scope checks phủ CHAT/SEARCH/REFRESH; blocked paths không xuất hiện trong adapter allowlist.
- [ ] 0 hold/release/booking/cancel/payment/refund/guest-write calls; không mở public route bằng chỉnh Security trong PR Member 1.

## Form phản hồi

Ghi từng ID: `decision`, `approvedVersion`, `approvedBy`, `approvedAt`, `API evidence`, `required changes`. Nếu có thay đổi wire hoặc semantics, tạo draft version mới; không sửa fixtures/đáp án đã human-approved để làm test pass. Các contract TBD là blocker integration, không blocker toàn bộ development Member 1.
