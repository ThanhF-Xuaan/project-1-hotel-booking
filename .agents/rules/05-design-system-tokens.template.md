---
trigger: always_on
description: "Quy chuẩn Design System: Định nghĩa Tokens, Màu sắc, Typography, Radius, Layout 3 hàng cho 'Hệ thống Quản lý Khách sạn'"
---

# 📐 Quy Chuẩn Design System & UI Tokens (Design System Standards)

> **Single Source of Truth:** 'Figma Design System Tokens v4.0 (Radius 12px/16px, Control Height 48px/40px, Bố cục Bảng 3 Hàng)'

---

## 1. Hệ Thống Tokens Cốt Lõi (Design Tokens)

### Bảng màu chủ đạo (Color Palette):
| Token | Giá Trị Mẫu (HEX/HSL) | Mục Đích Sử Dụng |
| :--- | :--- | :--- |
| `primary` | `#1890FF` (Blue) / `#008080` (Teal) | Màu thương hiệu, nút chính, điểm nhấn active |
| `success` | `#52C41A` (Green) | Trạng thái thành công, duyệt, hoàn thành |
| `warning` | `#FAAD14` (Amber) | Trạng thái chờ, cảnh báo, cần lưu ý |
| `error` | `#FF4D4F` (Red) | Trạng thái lỗi, từ chối, nút xóa nguy hiểm |
| `bg-neutral` | `#F5F7FA` | Màu nền trang (Background) |
| `border-neutral` | `#E2E8F0` | Đường viền phân cách card và input |

### Quy chuẩn bo góc (Border Radius):
- **Nút bấm (Buttons):** `12px` (hoặc `rounded-xl`)
- **Ô nhập liệu & Dropdown (Inputs/Selects):** `12px`
- **Khung chứa & Hộp thoại (Cards/Modals/Drawers):** `16px` (hoặc `rounded-2xl`)

### Quy chuẩn chiều cao (Control Heights):
- **Desktop (Màn hình > 920px):** Chiều cao ô nhập và nút bấm là `48px`.
- **Tablet / Mobile (Màn hình ≤ 920px):** Chiều cao co lại thành `40px`.

---

## 2. Quy Chuẩn Bố Cục Bảng Dữ Liệu 3 Hàng (Standard 3-Row Table Layout)

Mọi màn hình danh sách CRUD đều phải tuân thủ bố cục chuẩn 3 tầng:

```text
┌────────────────────────────────────────────────────────────────────────┐
│ HÀNG 1: SEARCH & FILTER BAR                                            │
│ [ Input Tìm Kiếm ] [ Dropdown Trạng Thái ] [ Date Range ] [ Nút Lọc ]  │
├────────────────────────────────────────────────────────────────────────┤
│ HÀNG 2: ACTION TOOLBAR                                                 │
│ [ + Thêm Mới ] [ 🗑️ Xóa Hàng Loạt ]                [ 📥 Xuất Excel ]  │
├────────────────────────────────────────────────────────────────────────┤
│ HÀNG 3: DATA TABLE & PAGINATION                                        │
│ ┌──┬──────────────┬──────────────┬────────────┬─────────────────────┐  │
│ │☑️│ Tên Sản Phẩm  │ Mã           │ Trạng Thái │ Hành Động (Sửa/Xóa) │  │
│ └──┴──────────────┴──────────────┴────────────┴─────────────────────┘  │
│ [ Hiển thị 1-10 / Tổng 150 ]              [ < 1 [2] 3 4 > ] [ 10/trang ]│
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Hộp Thoại Xác Nhận Hành Động Nguy Hiểm (Confirmation Modals)

- Mọi hành động Xóa (Delete), Từ chối (Reject), Hủy bỏ (Cancel) **BẮT BUỘC** phải bật Modal xác nhận với:
  - Tiêu đề rõ ràng: `Xác nhận xóa bản ghi?`
  - Mô tả chi tiết: Liệt kê rõ tên hoặc số lượng bản ghi sẽ bị xóa vĩnh viễn / chuyển vào thùng rác.
  - Nút Hủy: Màu xám trung tính (`secondary`).
  - Nút Xác nhận: Màu đỏ cảnh báo (`danger`), có hiệu ứng loading khi đang gửi request.
