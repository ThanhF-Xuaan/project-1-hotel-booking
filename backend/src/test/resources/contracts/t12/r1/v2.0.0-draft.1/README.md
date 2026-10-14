# Synthetic Core API fixtures — R1-00

Toàn bộ dữ liệu là synthetic, chỉ dùng test, không phải dump khách hàng hoặc runtime capture. `manifest.json` chỉ rõ schema và trạng thái từng file.

| Nhóm | Nguồn/giới hạn |
|---|---|
| pricing-request/responses-native | Field names từ PriceCalculationRequest/PriceBreakdownDto; JSON money dạng number như DTO BigDecimal thông thường. Không có currency/fee coverage. Fixtures số nguyên tránh mất precision; consumer phải parse Decimal/BigDecimal. |
| availability-request/responses-native | Search inclusive; hai đêm 20/21-11-2026 cho stay checkout 22. Inventory counters chỉ ở Core fixture, không lên card. |
| discovery/detail-proposed | API public chưa tồn tại. Projection và pagination là đề xuất, không dùng admin endpoint ngầm. 26 loại phòng, 6 hotel, page size 20. |
| quotes-normalized-proposed | AI adapter DTO dạng decimal string. VND chỉ là metadata synthetic cho case test, chưa được xác nhận từ Core. UNKNOWN service fee và BACKEND_QUOTED_TOTAL không có nghĩa all-in. |
| transport-failures | HTTP failures, malformed JSON, scope/session/PII/injection sentinel cho negative tests; không phải success response schema. |

Ứng viên 1026 ở hotel 6 có baseRate cao nhưng finalTotalAmount thấp nhất. Oracle toàn bộ: 1026, 1001, 1002, 1003, 1004 khi mọi preference bằng nhau. Không cắt 20 ứng viên rồi báo đã xét toàn bộ phạm vi.

Evaluation mutations là JSON Patch trên fixture riêng của từng case; phải deep-copy trước apply, không đổi file gốc. Một số mutations cố ý làm sai ID/thiếu đêm/null response để kiểm tra reject/error behavior. Native totals chỉ phục vụ test grounding; không phải implementation lại Pricing Engine. Không dùng fixtures làm production fallback khi API lỗi.
