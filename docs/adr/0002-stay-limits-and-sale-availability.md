# ADR 0002 — Giới hạn kỳ lưu trú và cách tính tồn phòng bán

**Trạng thái:** áp dụng cho luồng tra cứu và báo giá hiện tại.  
**Ngày:** 24/09/2026.

## Bối cảnh

Khoảng lưu trú không có giới hạn và truy vấn rule cho từng đêm khiến một request có thể tạo hàng trăm nghìn truy vấn. Đồng thời, điều kiện `current_status = 'READY'` loại phòng khỏi mọi kỳ lưu trú khi phòng đang có khách hoặc đang dọn ở thời điểm tra cứu.

## Quyết định

- `StayCriteria` nhận từ 1 đến **30 đêm**, tính theo `[checkIn, checkOut)`. Đây là giới hạn kỹ thuật ban đầu cho API tra cứu/báo giá và AI tool; yêu cầu dài hơn trả `400`, mã `1002`, trước khi controller/tool gọi service truy cập DB. Giới hạn dùng chung tại `StayCriteria.MAX_NIGHTS`, được ghi trong OpenAPI. Khi cần hỗ trợ lưu trú dài hạn, phải đánh giá tải và cập nhật contract/test cùng giới hạn này.
- Mỗi quote đọc VAT, pricing rule, discount rule và surcharge rule bằng **4 truy vấn theo khoảng lưu trú**. Việc chọn rule theo từng đêm thực hiện trong bộ nhớ. Ngày bắt đầu/kết thúc của cấu hình vẫn bao gồm cả hai đầu; ngày trả phòng không tính tiền. VAT không có ngày kết thúc được coi là chưa hết hiệu lực. Discount gắn campaign chỉ hiệu lực trong phần giao của hai khoảng ngày, giữ nguyên thứ tự ưu tiên. VAT thiếu/trùng và cấu hình đang hiệu lực không hiểu vẫn trả `422`.
- Pricing rule chỉ áp dụng khi loại rule và ngày lễ được tham chiếu còn `ACTIVE`, chưa soft-delete. Rule `HOLIDAY` đang hoạt động nhưng thiếu liên kết ngày lễ vẫn là cấu hình lỗi, trả `422`.
- Tồn phòng bán được tính từ phòng chưa soft-delete, các slot trong kỳ lưu trú, lịch bảo trì đang hoạt động và counter `room_availability`. `OCCUPIED`/`CLEANING` hiện tại không tự loại phòng khỏi tồn bán. Slot có trạng thái khác `READY` trong kỳ vẫn chặn phòng; slot đúng ngày trả phòng nằm ngoài kỳ.
- `current_status = 'MAINTENANCE'` tiếp tục đóng bán phòng cho đến khi trạng thái được cập nhật. Cờ này không có ngày hoàn tất nên không tự suy đoán thời điểm mở lại. Lịch bảo trì có ngày kết thúc nằm trong `room_maintenance_blocks`; ngày kết thúc block được tính là ngày còn bảo trì theo schema hiện tại. Block `ACTIVE` giao với kỳ lưu trú sẽ chặn phòng. Cách xử lý cả OOO/OOS hiện có được giữ nguyên.
- Availability cho biết tồn bán, không cam kết phòng vật lý đã sẵn sàng bàn giao. Luồng gán phòng/check-in sau này phải kiểm tra lại trạng thái vệ sinh và vận hành tại thời điểm nhận phòng. Luồng ghi phải cập nhật slot/counter nhất quán trong transaction.

## Tác động và kiểm chứng

FE và AI tiếp tục dùng cùng service báo giá và cấu trúc response. Request trên 30 đêm trước đây được chấp nhận sẽ bị từ chối. Không đổi schema, migration, quyền hay ranh giới module.

Regression test kiểm tra giới hạn kỳ lưu trú tại các điểm vào REST/tool; 4 truy vấn rule cho cả 1 và 30 đêm; VAT, campaign và phụ thu đổi giữa kỳ; ngày lễ ngừng hoạt động/soft-delete; trạng thái phòng hiện tại, slot, biên lịch bảo trì và counter trên PostgreSQL thật.

Quote vẫn là dữ liệu tham khảo tại thời điểm đọc, chưa giữ tồn hoặc chốt giá giao dịch. Giới hạn số đêm không thay thế rate limit cho API public; rate limit nằm trong backlog hạ tầng.
