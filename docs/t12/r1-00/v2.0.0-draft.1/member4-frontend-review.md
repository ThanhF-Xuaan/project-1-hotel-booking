# Member 4 — Frontend Contract Review

**Version:** `2.0.0-draft.1`. **Human approval:** PENDING; chưa có người duyệt/ngày ký. Tài liệu review, không triển khai Frontend hoặc gửi thông báo cho Member 4.

M4-01..07 chi tiết hoá CR-05/10 trong [tracker của người dùng](../../../T12-contract-requests.md); không tạo approval mới hoặc đổi owner. Card bổ sung stay, response có criteriaProvenance/ambiguities theo governance mới. Các field này không cho client quyền xác nhận scope hoặc nguồn giá.

## Card và route đề xuất

[RoomRecommendation Schema](schemas/advisor.schema.json) và [response example](examples/results-response.json) là chuẩn wire draft. Một card gồm recommendationId, rank, stay, RoomSummary, PriceQuote, reasons có evidenceFields và một action VIEW_ROOM_DETAILS.

Proposed path: `/portal/hotels/{hotelId}/room-types/{hotelRoomTypeId}`. Baseline chỉ có `/portal/search`, checkout và my-bookings; route detail này **chưa tồn tại**. Không thay bằng checkout hoặc deep link tạo booking. Không render CTA “giữ phòng”, “đặt ngay”, “thanh toán” trong AI cards. HotelRoomType ID khác RoomType catalog ID và RoomInstance ID.

| ID | Quyết định cần Member 4 duyệt | Hành vi yêu cầu |
|---|---|---|
| M4-01 | Detail route template và xử lý room không published/không thuộc hotel | Internal path server-built; không nhận href do LLM sinh. Card click chỉ navigate xem detail. |
| M4-02 | Card schema, responsive layout, label giá, currency null | Giữ decimal string đến formatter; không tự tính VAT/fee/tổng nhiều phòng. Currency null phải ghi chưa xác nhận, không tự gắn VND. |
| M4-03 | Revalidation trên detail/refresh và stale card UX | Refresh session version mới qua REFRESH; detail tải public detail + availability + Pricing theo flow M2/4 duyệt. Không âm thầm hold/booking. |
| M4-04 | NEED_INFO, NO_MATCH, PARTIAL/INCOMPLETE_SEARCH, TEMP_UNAVAILABLE và OUT_OF_SCOPE | Phân biệt thiếu thông tin, không có match sau full coverage, chưa kiểm tra đủ và lỗi API. Không đổi lỗi thành “hết phòng”. |
| M4-05 | Session credential, CSRF, version conflict/ownership | Không gửi owner/role/tenant từ client. 409 reload state; 404 không lộ owner. Phối hợp M2, không tự chốt authentication. |
| M4-06 | Multi-room limitation và điều hướng search thường | roomsCount≥2: giải thích giới hạn, hỏi tìm 1 phòng hoặc nút đi `/portal/search`; không thêm action này vào recommendation card. |
| M4-07 | Budget basis/coverage clarification và incomplete coverage copy | Phân biệt total/average/night-by-night; không nói all-in khi UNKNOWN. PARTIAL hiển thị cảnh báo và có thể refresh để kiểm tra thêm. |

## Rendering và tương tác

- Top 3–5 distinct HotelRoomType khi đủ; 1–2 card vẫn hợp lệ nếu chỉ có 1–2 match. Không tạo card giả hoặc lặp room để đủ số lượng.
- `rank` là thứ tự Java trả về; không frontend sort lại theo basePrice. `recommendationId` chỉ định danh card/snapshot, không quyền giữ phòng.
- `price.observedAt` là thời điểm quan sát, không thời hạn guarantee; không hứa còn phòng đến lúc đặt. Service fee UNKNOWN hiển thị rõ chưa xác nhận.
- Reasons chỉ là nội dung text đã grounded; không render HTML/Markdown links từ model/catalog. Không lộ internal inventory counters hoặc raw upstream errors.
- SessionVersion cập nhật nguyên tử với response mới; response về chậm của version cũ không thay cards phiên mới. Disable/reload khi conflict, không gửi nhiều refresh để tăng quota.
- Không lưu raw chat/PII hoặc credential trong analytics/localStorage. TTL và transcript retention chờ M2 duyệt.

## Gate trước integration

- [ ] M4-01..07 có quyết định cùng version/người duyệt/ngày.
- [ ] Consumer tests từ examples/schema; detail route navigation đúng hotelId/HotelRoomType ID.
- [ ] E2E click card không tạo hold/booking/payment request; network assertion businessWriteCalls=0.
- [ ] Price/availability đổi hoặc room ngừng public tại detail được revalidate; stale snapshot không bị hứa giá/tồn.
- [ ] Partial coverage, currency null, unknown fee, multi-room, 409 và lỗi Core có UX riêng.
- [ ] Render text an toàn; ownership và scope response không thể bị mở rộng từ payload.

Member 4 có thể mock schema examples trong test riêng sau approval; R1-00 không sửa Frontend. Link route trong fixture là proposal, chưa được coi là link chạy được.
