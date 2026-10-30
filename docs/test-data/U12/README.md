# Hướng Dẫn Sử Dụng File Mẫu Excel Rooming List (Sprint 3 — U12)

Tài liệu này mô tả chi tiết quy chuẩn cấu trúc bảng tính Excel danh sách phòng (Rooming List) và mục đích sử dụng của 3 tệp dữ liệu mẫu phục vụ kiểm thử tính năng **Excel Rooming List Import (U12)**.

---

## 1. Quy Chuẩn Cấu Trúc File Excel (Excel Schema Specification)

- **Định dạng file:** Tệp bảng tính Microsoft Excel (`.xlsx`).
- **Tên Sheet dữ liệu bắt buộc:** `RoomingList` (không phân biệt hoa thường).
- **Hàng tiêu đề (Header):** Hàng 1 cố định.
- **Dữ liệu (Data):** Bắt đầu từ Hàng 2 trở đi.
- **Giới hạn số dòng tối đa:** 500 dòng dữ liệu khách / lần tải lên (đảm bảo hiệu năng và ngăn chặn OOM).
- **Dung lượng tệp tối đa:** 10MB.

### Bảng Định Nghĩa Các Cột (Column Definitions)

| Cột (Header) | Tên tiếng Việt | Bắt buộc | Định dạng & Quy tắc kiểm tra (Validation Rules) | Ý nghĩa nghiệp vụ |
|:---|:---|:---:|:---|:---|
| `Full Name` | Họ và tên | **Có** | Chuỗi ký tự, tối đa 250 ký tự. Không được để trống. | Họ tên đầy đủ của khách lưu trú thực tế tại phòng. |
| `Gender` | Giới tính | Không | Chấp nhận: `Nam`, `Nữ`, `Khác` hoặc `MALE`, `FEMALE`, `OTHER`. | Giới tính để khai báo thông tin lưu trú. |
| `Date of Birth` | Ngày sinh | Không | `dd/MM/yyyy` hoặc `yyyy-MM-dd`. Phải là ngày trong quá khứ, năm sinh >= 1900. | Ngày tháng năm sinh của khách. |
| `Nationality` | Quốc tịch | Không | Tối đa 100 ký tự. Mặc định là `Việt Nam` nếu để trống. | Quốc tịch phục vụ xuất báo cáo lưu trú công an. |
| `ID Number` | Số giấy tờ | Không | Số CCCD (9-12 chữ số) hoặc Hộ chiếu (chữ + số, 6-50 ký tự). Phải duy nhất trong tệp. | Căn cước công dân / Hộ chiếu định danh khách. |
| `Phone` | Số điện thoại | Không | 10 số, bắt đầu bằng `0` hoặc `+84` (VD: `0912345678`). | Số điện thoại liên hệ của khách. |
| `Email` | Email | Không | Định dạng chuẩn RFC 5322 (VD: `guest@example.com`). | Email nhận thông tin xác nhận hoặc hóa đơn. |
| `Room Number` | Số phòng | Không | Mã số phòng vật lý (VD: `101`, `201`), tối đa 20 ký tự. | Gán khách vào phòng vật lý tương ứng trong đơn. |
| `Note` | Ghi chú | Không | Chuỗi ký tự, tối đa 250 ký tự. | Ghi chú yêu cầu đặc biệt hoặc lý do lưu trú. |

---

## 2. Danh Sách 3 File Excel Mẫu Trong Thư Mục

### 📁 1. `rooming-list-valid-20.xlsx`
- **Mục đích:** Kiểm tra luồng Happy Path — Import thành công 100%.
- **Số dòng dữ liệu:** Đúng 20 dòng khách hợp lệ (từ dòng 2 đến dòng 21), không tính hàng tiêu đề.
- **Đặc điểm:** Dữ liệu chuẩn hóa hoàn chỉnh, kết hợp khách Việt Nam và khách quốc tế, có số phòng phân bổ, không trùng lặp giấy tờ tùy thân.
- **Kết quả kỳ vọng:**
  - Preview nhận diện `totalRows = 20`, `validCount = 20`, `errorCount = 0`.
  - Khi xác nhận import: Tạo chính xác 20 bản ghi mới trong bảng `stay_guests` (nếu database chưa tồn tại dữ liệu trùng).

### 📁 2. `rooming-list-invalid-rows.xlsx`
- **Mục đích:** Kiểm tra khả năng xử lý lỗi chi tiết theo từng dòng (Row-level Error Handling).
- **Số dòng dữ liệu:** 11 dòng dữ liệu (từ dòng 2 đến dòng 12), kết hợp xen kẽ giữa dòng hợp lệ và dòng lỗi.
- **Danh sách kịch bản lỗi được thiết kế có chủ đích:**
  - **Dòng 2:** Hợp lệ (`Nguyễn Văn Hợp Lệ`) -> Preview hiển thị Hợp lệ.
  - **Dòng 3:** Bỏ trống họ tên -> Báo lỗi: *"Họ và tên khách không được để trống"*.
  - **Dòng 4:** Email sai định dạng (`email_sai_dinh_dang`) -> Báo lỗi: *"Email 'email_sai_dinh_dang' không đúng định dạng"*.
  - **Dòng 5:** Ngày sinh không tồn tại trên lịch (`31/02/1995`) -> Báo lỗi: *"Ngày sinh '31/02/1995' không đúng định dạng"*.
  - **Dòng 6:** Hợp lệ (`Phạm Văn Năm`) -> Preview hiển thị Hợp lệ.
  - **Dòng 7:** Giới tính không hỗ trợ (`KhongXacDinh`) -> Báo lỗi: *"Giới tính 'KhongXacDinh' không hợp lệ"*.
  - **Dòng 8:** Số điện thoại quá ngắn (`012345`) -> Báo lỗi: *"Số điện thoại '012345' không đúng định dạng"*.
  - **Dòng 9:** Ngày sinh ở tương lai (`15/05/2099`) -> Báo lỗi: *"Ngày sinh không được ở tương lai"*.
  - **Dòng 10:** Hợp lệ (`Bùi Văn Chín`, CCCD `001091099009`) -> Preview hiển thị Hợp lệ.
  - **Dòng 11:** Trùng số CCCD với dòng 10 (`001091099009`) -> Báo lỗi: *"Số giấy tờ '001091099009' bị trùng lặp trong file (đã xuất hiện tại dòng 10)"*.
  - **Dòng 12:** Hợp lệ (`Đinh Văn Mười Một`) -> Preview hiển thị Hợp lệ.
- **Kết quả kỳ vọng:**
  - `totalRows = 11`, `validCount = 4`, `errorCount = 7`.
  - Hiển thị đúng số dòng và nguyên nhân lỗi của từng dòng; các dòng hợp lệ vẫn hiển thị và có thể được xác nhận import.

### 📁 3. `rooming-list-invalid-structure.xlsx`
- **Mục đích:** Kiểm tra cơ chế phòng vệ khi cấu trúc file sai lệch hoàn toàn (Structural Validation).
- **Đặc điểm:** Tệp Excel hợp lệ nhưng sheet dữ liệu bị đặt tên sai là `DanhSachKhach` thay vì `RoomingList`.
- **Kết quả kỳ vọng:**
  - Backend phát hiện lỗi cấu trúc và từ chối xử lý với mã lỗi `EXCEL_INVALID_STRUCTURE` (400 Bad Request).
  - Thông báo rõ ràng: *"Cấu trúc file không hợp lệ: Không tìm thấy sheet dữ liệu có tên 'RoomingList'"*.
  - Hoàn toàn không ghi bất kỳ bản ghi nào vào database.

---

## 3. Cách Sử Dụng File Mẫu Khi Kiểm Thử

1. **Trên Giao Diện (Frontend):**
   - Truy cập màn hình **Danh sách Đặt phòng** (`/booking/list`) hoặc trang **Import Rooming List** (`/booking/rooming-list`).
   - Chọn đơn đặt phòng mong muốn (trạng thái `CONFIRMED`).
   - Nhấn nút **Import Rooming List (Excel)**.
   - Kéo thả hoặc duyệt chọn một trong ba file trên.
   - Quan sát bảng Preview phân tích dữ liệu, badge hợp lệ/lỗi và chi tiết lỗi của từng dòng.
   - Nhấn **Xác nhận Import** đối với file hợp lệ để hoàn tất lưu vào cơ sở dữ liệu.

2. **Qua API (cURL / Postman / Swagger):**
   ```bash
   # Preview file mẫu 20 dòng
   curl -X POST "http://localhost:8080/api/v1/bookings/1/rooming-list/preview" \
     -H "Authorization: Bearer <TOKEN>" \
     -F "file=@docs/test-data/U12/rooming-list-valid-20.xlsx"

   # Xác nhận import file mẫu
   curl -X POST "http://localhost:8080/api/v1/bookings/1/rooming-list/import?confirm=true" \
     -H "Authorization: Bearer <TOKEN>" \
     -F "file=@docs/test-data/U12/rooming-list-valid-20.xlsx"
   ```
