# ADR 0001 — Chính sách báo giá cho luồng đọc đầu tiên

**Trạng thái:** áp dụng tạm cho code base, cần nghiệp vụ duyệt trước khi mở lệnh booking.  
**Ngày:** 24/09/2026.

## Bối cảnh

Schema có giá cơ sở, pricing rule, discount rule, surcharge rule, phí dịch vụ và VAT, nhưng chưa có tài liệu chốt thứ tự áp dụng khi nhiều rule cùng hiệu lực. Quote phải cho kết quả xác định và không bỏ qua rule chưa hiểu.

## Quyết định

Với mỗi đêm trong khoảng `[checkIn, checkOut)`: cộng các pricing adjustment hợp lệ vào giá cơ sở; chọn **một** discount đang áp dụng có priority cao nhất; cộng phụ thu thêm người được yêu cầu; tính phí dịch vụ trên số sau giảm/phụ thu; tính VAT trên số đó cộng phí dịch vụ. Mỗi thành phần làm tròn 2 chữ số thập phân theo `HALF_UP`, rồi cộng tổng các đêm.

`HOLIDAY` chỉ áp dụng đúng ngày liên kết; `WEEKEND` là thứ Sáu và thứ Bảy theo seed hiện tại; `PEAK_SEASON` theo ngày hiệu lực. `LONG_STAY`, `EARLY_BIRD`, `LAST_MINUTE` và campaign không có mã được hỗ trợ ở quote đầu tiên. Campaign yêu cầu promo code không áp dụng khi API chưa nhận mã. Giường phụ/nhận phòng sớm/trả phòng trễ chỉ tính khi use case tương ứng được triển khai. Rule/điều kiện đang hiệu lực nhưng không hiểu, VAT thiếu hoặc trùng, hoặc giá trị ngoài phạm vi hợp lệ trả `422`.

Giá quote chỉ để tham khảo, chưa giữ tồn và chưa là snapshot giao dịch. Giá và thuế suất đều lấy từ database, không hardcode một mức VAT.

## Hệ quả

FE và AI dùng cùng `PricingQuoteService`. Một số cấu hình rule mới sẽ làm báo giá tạm ngừng cho hạng phòng liên quan cho đến khi calculator được mở rộng. Trước booking/payment cần chốt chính sách stack discount, hoàn tiền, phí theo đợt lưu trú và kiểm thử thêm ca cạnh tranh giá.
