# OCR-02 — Báo cáo triển khai và bằng chứng local

Ngày 28/10/2026. Trạng thái: **TASK_READY_FOR_REVIEW**, chưa có quyết định TASK_PASSED độc lập.

## Baseline và quyền triển khai

- Source: `C:/Users/admin/.codex/worktrees/m1-completion-a00/project-1-hotel-booking`.
- Detached HEAD/base `5509af2e26daca6dd67a32acb987a86648808df2`.
- User đã cho phép tự động hoàn thiện OCR-02, tự chọn phương án và mở rộng phạm vi sửa. Quyền này thay chỉ đạo review-only trước đó; không ghi thành xác nhận của Member 3 hoặc Member 2.
- Mọi OCR-01/OCR-02 dirty/untracked trước lượt này được bảo vệ; chỉ sửa tiếp đúng năm file hiện hữu được liệt kê dưới đây. Không chuyển source về repo D:, không stage/commit/push/merge.

## Các thay đổi

1. `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/ocr/service/IdentityDocumentOcrService.java`: capture họ tên/quốc tịch dùng khoảng trắng ngang, dừng trước newline; không lấy nhãn dòng kế tiếp. Giữ schema và threshold.
2. `backend/src/test/java/vn/edu/utc/hotel_booking/modules/ai/ocr/IdentityDocumentOcrServiceTest.java`: siết hai assertion thành exact-value, thêm regression LF/CRLF. Test mapping bảy trường trong controller tiếp tục giữ nguyên expected.
3. `backend/src/main/resources/application-ocr-local.yaml` (mới): profile opt-in; max file 5 MiB, max request 6 MiB, threshold 6 MiB. Threshold bằng toàn request để các part được chấp nhận đều nằm trong memory. Flag mặc định false, executable lấy từ TESSERACT_EXE.
4. `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/ocr/config/OcrMultipartPrivacyGuard.java` và test mới `OcrMultipartPrivacyGuardTest.java`: khi bật OCR ảnh, startup thất bại nếu multipart tắt, giới hạn không hữu hạn/không phù hợp, hoặc threshold thấp hơn giới hạn request. Cấu hình mặc định có disk spool bị từ chối khi engine bật.
5. `backend/src/test/java/vn/edu/utc/hotel_booking/modules/ai/ocr/controller/IdentityDocumentOcrControllerTest.java`: runtime harness đọc chính YAML bằng Binder, gọi guard, giữ positive control default spool; thêm kiểm tra token/JWKS từ Keycloak thật và tùy chọn chạy bộ API Playwright khi servlet đang mở.
6. `frontend/src/modules/operation/services/identityOcr.service.ts` (mới): multipart qua API client/JWT interceptor hiện có, để browser tự gắn boundary; không gửi rawOcrText. Kiểm file không rỗng/type/5 MiB và Zod kiểm shape/date/enum/human flag của response. Không lưu file hoặc draft vào localStorage.
7. `frontend/src/modules/booking/components/StayGuestsModal.tsx`: tải ảnh, điền đúng bảy field, hiển thị confidence/valid/warnings, cho sửa và yêu cầu checkbox xác minh. Chặn submit khi đang OCR/chưa xác minh; chỉ gọi StayGuest.create từ submit. Hai datetime-local bắt buộc đáp ứng request backend; kiểm giờ trả sau giờ nhận, chuyển thành ISO có timezone. Abort khi hủy/đóng/đổi phòng, xóa draft và thông tin form; không ghi lỗi Axios chứa payload giấy tờ vào console khi lưu thất bại.
8. `frontend/e2e/api/ocr-synthetic.spec.ts`: hỗ trợ token cấp từ harness trong environment, siết exact-value họ tên của text test; mặc định giữ cách login acceptance cũ.
9. `frontend/e2e/browser/ocr-review.spec.ts` và `frontend/e2e/fixtures/ocr-review.html`, `ocr-review.tsx` (mới): mount chính StayGuestsModal để kiểm bảy field, sửa rồi submit, checkbox, timezone, không ghi trước submit, lỗi503/unreadable/shape và hủy request. Fixture nằm ngoài production entry/build; API của browser test được intercept.

## Kết quả kiểm chứng

| Kiểm tra | Kết quả | Phạm vi bằng chứng |
|---|---|---|
| Maven targeted 8 classes | PASS: 63 test, 0 fail/error/skip | Controller15, servlet4, parser11, upload11, adapter9, TSV3, no-write7, guard3 |
| Playwright API | PASS: 5/5 | HTTP Tomcat thật, Tesseract thật, JWT/JWKS Keycloak thật, profile YAML thật; JSON, multipart, lỗi ảnh/path traversal |
| Playwright Chromium | PASS: 5/5 | Component thật; response OCR và endpoint lưu được intercept; không ghi StayGuest thật |
| npm run build | PASS | TypeScript + Vite production build |
| npm run lint | PASS | ESLint toàn frontend |
| git diff --check | PASS | Không lỗi whitespace |
| Hash preservation | PASS | Năm file hiện hữu đổi đúng phạm vi; mọi file có trước khác giữ nguyên |

Backend command từ backend/:

```powershell
$env:TESSERACT_EXE='C:/Program Files/Tesseract-OCR/tesseract.exe'
.\mvnw.cmd '-Dtest=OcrUploadValidatorTest,IdentityDocumentOcrServiceTest,TesseractOcrVisionAdapterTest,TesseractTsvParserTest,IdentityDocumentOcrControllerTest,U13ZeroWriteAndPiiSafetyTest,OcrMultipartServletRuntimeTest,OcrMultipartPrivacyGuardTest' test
```

Để chạy lại case Keycloak, cấp qua environment: OCR_KEYCLOAK_ISSUER, OCR_KEYCLOAK_RECEPTION_TOKEN, OCR_KEYCLOAK_DENIED_TOKEN. Hai token phải còn hạn, lần lượt có role reception và role không được phép. Không ghi token vào lệnh/log/file. Đặt OCR_RUN_API_SUITE=true để harness gọi Playwright API. Không có environment này thì case Keycloak được skip, không được báo đã kiểm Keycloak. Lượt cuối có đủ environment và 0 skip. Keycloak local đã có sẵn tại localhost:8081/realms/hotel-realm; chỉ login các tài khoản synthetic có sẵn, không sửa realm/user/security.

Browser: chạy Vite loopback cổng13113 rồi VITE_BASE_URL=http://127.0.0.1:13113 và `npx playwright test e2e/browser/ocr-review.spec.ts`.

Logs:
- `C:/Users/admin/AppData/Local/Temp/ocr02-auto-verified.log`
- `backend/target/ocr02-playwright-api.log`
- `C:/Users/admin/AppData/Local/Temp/ocr02-auto-browser-final.log`
- `C:/Users/admin/AppData/Local/Temp/ocr02-auto-build-final.log`
- `C:/Users/admin/AppData/Local/Temp/ocr02-auto-lint-final.log`
- `C:/Users/admin/AppData/Local/Temp/ocr02-auto-preservation.json`

## Finding và phần còn cần review

| ID | Trạng thái FIX | Căn cứ / giới hạn |
|---|---|---|
| OBS-OCR02-01 | FIXED | Bảy giá trị exact và LF/CRLF PASS; không nới oracle |
| REV-004-F001 | FIXED phần consumer; xác nhận M3 còn UNVERIFIED | Đã có consumer thực trong modal và browser chứng minh review/edit/explicit submit. M3 chưa xác nhận, chưa chạy luồng booking thật với DB thật |
| REV-004-F002 | FIXED phần config/guard; triển khai đích còn UNVERIFIED | YAML thật đã bind vào Tomcat; cap/memory/watch positive control và Keycloak thật PASS. Redis blacklist vẫn mock; không khẳng định full application/deployment đã nghiệm thu |

Giữ finding chưa CLOSED; REVIEW độc lập quyết định. Không tự nhận M3/M2 đã duyệt hoặc deployment production đạt.

## Cách bật, ảnh hưởng và rollback

Profile `ocr-local` là opt-in cho sandbox local đã kiểm. Bật profile và HOTEL_AI_OCR_ENABLED=true, TESSERACT_EXE trỏ executable có vie+eng. Có thể chạy app theo launcher môi trường đã được owner xác minh; không chạy launcher vào DB tùy ý để tạo bằng chứng. Chưa khởi động full app, không chạy migration hay ghi guest vào DB trong lượt này.

Cấu hình multipart áp dụng TOÀN BỘ upload endpoint trong process: giới hạn file5/request6 MiB có thể không phù hợp các upload lớn khác; cần owner cấu hình đánh giá trước rollout shared deployment. Không đưa ảnh thật vào sandbox/eval; tất cả bằng chứng dùng synthetic. Tải đồng thời nhiều request vẫn cần vận hành kiểm tải/memory; semaphore OCR chỉ giới hạn engine, không phải tổng upload đang parse.

Tắt HOTEL_AI_OCR_ENABLED để rollback engine; giữ profile memory nếu vẫn xử lý ảnh identity qua text fallback. Không gỡ profile khi flag ảnh còn bật vì guard sẽ chặn startup. UI lỗi dependency vẫn cho nhập tay. Không có thay đổi DB/schema/DTO/role hay binary dependency; không có dữ liệu cần rollback. Không thay default profile hoặc tự triển khai production.
