# U14 privacy gate và T12 source re-audit

- **Baseline xác nhận:** `origin/develop@52dc842c4e361ce729f8dd742fca5236eebe535a`.
- **Nhánh làm việc:** `codex/m1-privacy-audit-followup`.
- **Ủy quyền:** người dùng nhận quyền quyết định toàn project và yêu cầu thực hiện phương án hợp lý; phạm vi hiện tại là giảm rủi ro U14, đối soát nguồn T12 và làm rõ giới hạn T05.
- **CONFIRMED:** `BookingGuest` chưa có mapping tài khoản Keycloak; chưa tìm thấy consent store trong repo. DTO U14 nhận guestId, consentGranted và stayHistory từ caller. Chúng chưa phải dữ liệu được server xác minh.
- **Quyết định triển khai:** U14 HTTP hiện tại chỉ dành cho đánh giá bằng dữ liệu giả lập. Controller mặc định không được đăng ký; chỉ bật khi `hotel.ai.personalization.synthetic-evaluation-enabled=true` và chỉ `CHAIN_ADMIN` được gọi cả hai endpoint. Launcher acceptance bật rõ ràng chế độ này. Bỏ identifier/preference khỏi log U14.
- **Tương thích:** route, DTO và response thành công giữ nguyên khi mode được bật. Truy cập mặc định bị đóng; các vai trò khác bị từ chối kể cả gửi consent=true. Đây là thay đổi phạm vi truy cập có chủ đích.
- **T12:** re-audit thay đổi import lazy/Suspense tại commit đã merge `8d5a347`; xác minh cây Route, guard/role, redirect và mapping module. Chỉ cập nhật record nguồn App.tsx sau đối soát, giữ entry cũ trong audit version mới, không sửa validator/schema/gold/threshold.

## Danh sách file được sửa

- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/personalization/controller/GuestPersonalizationController.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/personalization/service/GuestPersonalizationService.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/personalization/service/PersonalizationBaselineEvaluator.java`
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/ai/personalization/controller/GuestPersonalizationControllerTest.java`
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/ai/personalization/controller/GuestPersonalizationExposureTest.java`
- `tools/acceptance/Prepare.ps1`, `tools/acceptance/Run-Application.ps1`
- `tools/acceptance/Test.ps1` (chỉ bỏ số test Maven/API hardcode đã cũ; giữ nguyên lệnh và exit-code gate).
- `frontend/e2e/api/personalization-u14.spec.ts`
- `docs/t12/r1-00/v2.0.0-draft.1/baseline-source-evidence.json` (chỉ entry App.tsx)
- `docs/t12/re-audits/2026-10-25-lazy-routes-v1.json`, `docs/t12/re-audits/2026-10-25-lazy-routes-v1.md`
- `tools/t12/verify_lazy_route_reaudit.cjs` (runner chỉ đọc để tái hiện đối soát AST và hash từ Git).
- `docs/member1/acceptance/README.md`, báo cáo này.

Báo cáo đối soát develop từ lượt trước giữ nguyên; README cập nhật trạng thái/hành vi mới và giữ các mốc lịch sử. Các file untracked của người dùng không được đưa vào change set.

## Kiểm chứng dự kiến và rollback

Chạy test exposure bằng Spring context/method security thật: mặc định tắt, false tắt, true cho admin, non-admin/anonymous bị từ chối trước service. Chạy toàn bộ personalization regression; T12 validator/portability; API/browser với backend thật và dữ liệu giả lập. Giữ quy trình snapshot/restore và dừng sandbox sau test. Rollback code qua revert change set sau khi commit được cho phép; không reset/clean/stash hoặc thao tác dữ liệu chung.

## Hợp đồng dữ liệu thật còn thiếu

**U14 — PROPOSED:** luồng thật phải resolve guest từ principal hoặc staff scope đã xác thực, kiểm tra consent còn hiệu lực server-side trước khi đọc history, chỉ đọc lịch sử thuộc guest/scope được phép và chỉ dùng field cần thiết. Client không được quyết định consent/ownership/history. Cần xác định identity binding, nơi ghi/rút consent, nguồn history read-only và quy tắc giữ/xóa trước khi triển khai. Chưa tạo schema hoặc thời hạn lưu trữ giả định.

Cờ synthetic là cổng bật/tắt do người vận hành quản lý; không tự nhận biết dữ liệu thật và giả. Chỉ dùng fixture giả lập trong mode này. Không ghép tài khoản với guest bằng email/phone do client khai báo để thay thế identity binding đáng tin cậy.

**T05 — BLOCKED nghiệp vụ:** cần file mẫu/schema từ đích nhận export, phiên bản template, danh mục mã, cột bắt buộc, kiểu ngày/giới tính/giấy tờ/quốc tịch, quy tắc tên sheet/thứ tự, encoding và cách xác nhận xử lý thành công. Có mẫu này mới xây fixture synthetic theo schema và test nội dung workbook. Định dạng XLSX hiện tại chỉ được xác nhận kỹ thuật.

## Kết quả thực thi — 2026-10-25 18:59 +07:00

Các lệnh dưới đây chạy trên baseline `52dc842` cộng working diff được liệt kê ở trên; thay đổi chưa commit. Dữ liệu, guest ID và tài khoản dùng trong kiểm tra đều giả lập.

| Kiểm tra | Lệnh/phương pháp | Kết quả |
|---|---|---|
| U14 targeted | Trong backend: `.\mvnw.cmd '-Dtest=*Personalization*' test` | PASS, exit 0; 31 test, 0 lỗi/skip. Có 10 test exposure với Spring context và method-security proxy thật. |
| Backend toàn repo | `.\mvnw.cmd clean test`, `T12_REDIS_IT=true`, host `127.0.0.1:16379`, exclusive verified cho Redis chỉ thuộc sandbox acceptance; app chưa chạy | PASS, exit 0; 909 test, 0 lỗi, 1 skip. Redis integration 4/4 thực chạy; `contextLoads` có `@Disabled` yêu cầu hạ tầng. |
| U14 mặc định tắt | Khởi động backend thật với runtime không có cờ; admin token hợp lệ POST cả recommend và evaluate-baseline, consent=true | PASS; cả hai trả 404. |
| U14 bật trong sandbox | Runtime đặt cờ true; API E2E thật dùng admin, manager_h1, reception_h1, test_customer | PASS; admin chạy thuật toán; ba tài khoản non-admin nhận 403 ở cả hai endpoint trước mọi phân tích. |
| API regression | `npm run test:api` | PASS, exit 0; 12/12. |
| Browser regression | `npm run test:e2e` | PASS, exit 0; 6/6, gồm login, T12 và request no-write so với digest DB. |
| T12 source re-audit | `node tools/t12/verify_lazy_route_reaudit.cjs` | PASS, exit 0; 39 route node, 22 lazy component, 4 default alias xác minh cùng component. |
| T12 artifact contract | `python tools/t12/validate_r1_contracts.py` | PASS, exit 0; 184 instance, 74 case, 23 protected hash, 34 source hash. Engine gold evaluation vẫn NOT_RUN. |
| Portability | `python tools/t12/test_portability_validation.py` | PASS, exit 0; 11/11. Thử hash App LF/CRLF thành công; sửa nội dung trong bộ nhớ bị từ chối. |
| Frontend | `npm run lint -- --max-warnings=0`; `npm run build`; `npm audit --audit-level=high` | PASS, exit 0; lint 0 lỗi/cảnh báo, Vite 8.2.4 chunk lớn nhất 246.80 kB, audit 0 lỗ hổng. |
| PowerShell | Parse AST các script acceptance được sửa | PASS; không có lỗi cú pháp. `Test.ps1` chỉ đổi mô tả bỏ số test hardcode; lần này chạy các suite riêng để giữ báo cáo lịch sử. |

Một lệnh kiểm tra thử dùng `--json-report` trong `.runtime` bị validator từ chối vì report path phải nằm ngoài repository. Đã chạy lại lệnh chuẩn và lưu stdout vào log, đạt PASS; quy tắc validator được giữ nguyên.

Bằng chứng local nằm trong `tools/acceptance/.runtime/`: `u14-privacy-targeted.log`, `privacy-followup-maven-full.log`, `u14-disabled-http.json`, `privacy-followup-api.log`, `privacy-followup-browser.log`, `privacy-followup-contract.log`, `privacy-followup-build.log`, hai log start và `privacy-followup-restore.log`. Log runtime không đưa vào Git.

## Trạng thái bàn giao

- **CONFIRMED:** scope HTTP U14 đã được đóng mặc định và giới hạn synthetic/admin. Chưa có luồng đọc consent/history thật hay mapping guest từ principal; cờ không tự nhận biết PII.
- **CONFIRMED:** T12 source hash được reconcile sau audit, có bản snapshot entry cũ và runner chỉ đọc. Không sửa validator, gold, schema hoặc ngưỡng.
- **CONFIRMED:** Liquibase không có changeset mới; snapshot đã tạo trước khi seed/test, sau đó restore thành công (2 hotels, 5 room types, 305 availability, 5 staff). App và container đã dừng; volume giữ lại. Không gọi Gemini hoặc dịch vụ dữ liệu thật.
- **BLOCKED nghiệp vụ:** T05 cần mẫu/schema chính thức; U14 thật cần identity binding và nguồn consent/history được chốt. Các contract yêu cầu được liệt kê ở trên.
- **Git:** branch `codex/m1-privacy-audit-followup`, HEAD/base `52dc842`; chưa commit/push/PR. Thay đổi/untracked có từ trước được giữ nguyên.
