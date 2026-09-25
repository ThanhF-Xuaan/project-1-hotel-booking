---
trigger: always_on
description: "Quy chuẩn sinh code Frontend: Component, State Management, Chống Memory Leak, Reactive Form và BaseHttpService cho 'Hệ thống Quản lý Khách sạn'"
---

---
trigger: always_on
globs: src/**/*.jsx, src/**/*.tsx, src/**/*.css
---

# 🎨 Quy Chuẩn Phát Triển Frontend & Giao Diện (Frontend Coding & UI Standards)

> **Framework áp dụng:** React 19, TypeScript 6.0, Vite 8.2, Tailwind CSS v4, React Hook Form, Zod  
> **UI Design System:** Figma Design System Tokens v4.0 (Radius 12px/16px, Control Height 48px/40px, Bố cục Bảng 3 Hàng)

Tài liệu này quy định các ràng buộc nghiêm ngặt về nhận diện hình ảnh, bố cục responsive, tiêu chuẩn trợ năng (Accessibility) và kiến trúc mã nguồn cho toàn bộ dự án frontend.

## 1. Ràng Buộc Bảng Màu & Giao Diện (Style & Palette Constraints)
Hệ thống sử dụng kiến trúc thiết kế dựa hoàn toàn vào Tailwind CSS với bảng màu doanh nghiệp có độ tương phản cao (Đỏ, Trắng, Đen):
- **Màu nhấn chính (Brand Red):** BẮT BUỘC sử dụng màu đỏ công nghệ (tương đương `bg-red-600`, `text-red-600`, `border-red-600`, hoặc `#EE0000`) dành riêng cho: logo thương hiệu, nút Call-to-Action (CTA) chính, nút submit quan trọng, trạng thái active/selected của thanh điều hướng và các icon cốt lõi.
- **Nền tảng (Canvas White):** BẮT BUỘC dùng màu trắng tuyệt đối (`bg-white`) hoặc off-white cực sạch làm nền chủ đạo cho toàn bộ panel, dashboard và layout. Tuyệt đối không sử dụng các biến thể Dark Mode hay các dải gradient xám đục.
- **Typography & Bố cục (Deep Black):** BẮT BUỘC dùng màu đen đặc (`text-black` hoặc `text-neutral-900`) cho các thẻ tiêu đề (`<h1>` đến `<h6>`), văn bản nội dung và thanh điều hướng chính để đảm bảo độ sắc nét tuyệt đối trên nền trắng.

## 2. Ràng Buộc Tailwind CSS & Responsive
- **Tiếp cận Mobile-First:** BẮT BUỘC viết các utility classes của Tailwind cho màn hình di động trước, sau đó mới nâng cấp dần cho các màn hình lớn hơn thông qua các breakpoint `sm:`, `md:`, `lg:`, và `xl:`.
- **Bố cục linh hoạt (Fluid Layouts):** Tránh hardcode giá trị pixel (`px`) cho cấu trúc layout, độ rộng, padding hay margin. BẮT BUỘC sử dụng phần trăm (`%`), viewport units (`vw`/`vh`), hoặc các đơn vị tương đối (`rem`/`em`) để giao diện tự động scale mượt mà trên mọi thiết bị.

## 3. Khả Năng Truy Cập (A11Y) & Semantic HTML
- **Cấu trúc ngữ nghĩa:** KHÔNG lạm dụng thẻ `<div>`. BẮT BUỘC sử dụng các thẻ HTML5 chuẩn ngữ nghĩa như `<main>`, `<nav>`, `<header>`, `<footer>`, `<section>`, `<article>`, và `<aside>` tại các vị trí logic.
- **Độ tương phản:** Đảm bảo mọi tổ hợp màu chữ/nền đều vượt qua bài kiểm tra tương phản chuẩn WCAG AA/AAA.
- **Phần tử tương tác:**
  - Mọi nút bấm chỉ có icon BẮT BUỘC phải có `aria-label` mô tả chức năng.
  - Mọi form control BẮT BUỘC phải liên kết hợp lệ với `<label>` thông qua `htmlFor` hoặc bọc trực tiếp.
  - Trạng thái form (lỗi/cảnh báo) BẮT BUỘC phải rõ ràng về mặt thị giác, sử dụng viền màu đỏ chính và text hỗ trợ (assistive text).
- **Điều hướng bàn phím:** Các phần tử tương tác BẮT BUỘC phải hỗ trợ điều hướng tuần tự bằng bàn phím (`tabIndex`) và có hiệu ứng focus nổi bật (`focus:ring-2 focus:ring-red-600`).

## 4. Cấu Trúc Thư Mục Module Chuẩn (Module Directory Structure)
Mỗi module chức năng (Feature Module) phải được đóng gói khép kín theo cấu trúc chuẩn:

```text
src/app/pages/<feature-name>/
├── configs/                    ← Chứa DTO, Interface, Enum, Column Definitions
│   ├── <feature>.model.ts      ← Khai báo Types, DTOs, Enums
│   └── <feature>.table-cfg.ts  ← Cấu hình cột bảng hiển thị
├── services/                   ← Tầng giao tiếp API & xử lý dữ liệu
│   ├── <feature>.service.ts    ← Gọi API (kế thừa BaseHttpService)
│   └── <feature>-helper.service.ts ← mapToUI, mapToApi, STATUS_OPTIONS
├── components/                 ← Các Sub-components / Modals / Drawers
│   ├── <feature>-modal-form/   ← Form thêm/sửa bản ghi
│   └── <feature>-detail/       ← Màn hình chi tiết
├── <feature>-list.component.ts ← Màn hình danh sách chính
├── <feature>-list.component.html
└── <feature>-list.component.scss
```

## 5. Tầng HTTP Service Cơ Sở (BaseHttpService Pattern)
Mọi Service gọi API không gọi trực tiếp thư viện HTTP thô mà phải kế thừa từ `src/lib/axios.ts` (Axios Instance cấu hình Keycloak JWT Interceptor).
- Tầng này đã tích hợp sẵn: Xử lý JWT Token tự động, Interceptor bắt lỗi mạng, Loading spinner toàn cục, và chuẩn hóa cấu trúc `ApiResponse<T>`.

## 6. Tầng Chuyển Đổi Dữ Liệu (Helper / Mapping Layer)
Tách riêng logic chuyển đổi dữ liệu giữa API DTO và UI Model vào file `helper.service.ts`:
- `mapToUI(apiDto: ApiDto): UIModel`: Chuyển đổi trạng thái số sang nhãn hiển thị, định dạng ngày tháng, số tiền.
- `mapToApi(uiFormValue: any): ApiRequestDto`: Chuẩn hóa dữ liệu form thành payload đúng chuẩn backend yêu cầu.
- `STATUS_OPTIONS`: Danh sách hằng số dùng cho dropdown filter.

## 7. Xử Lý Form Phản Ứng (Reactive Forms & Validation)
- **Nguyên tắc:** Sử dụng Reactive Form, không dùng Two-Way Binding thô cho các form phức tạp.
- **Quy chuẩn Validation:**
  - Hiển thị thông báo lỗi rõ ràng dưới từng trường khi user `dirty` / `touched`.
  - Disable nút Submit hoặc hiển thị Loading khi đang gửi request lên server.
  - Tự động trim khoảng trắng thừa ở 2 đầu input trước khi submit payload.

## 8. Quản Lý Trạng Thái & Chống Rò Rỉ Bộ Nhớ (Memory Leak Prevention)
- **Nguyên tắc vàng:** Bất kỳ Subscription hoặc Stream nào được mở trong Component **BẮT BUỘC** phải tự động hủy khi Component bị Destroy.
  - **Với Angular Modern:** Sử dụng `takeUntilDestroyed(this.destroyRef)` hoặc pipe `async` trên template HTML.
  - **Với React:** Viết cleanup function trong `useEffect()` hoặc sử dụng React Query (`useQuery`).
  - **Với Vue:** Sử dụng composables và `onUnmounted()`.
- **Cấm kỵ:** Tuyệt đối không gọi `.subscribe()` trần mà không có cơ chế unsubscribe.