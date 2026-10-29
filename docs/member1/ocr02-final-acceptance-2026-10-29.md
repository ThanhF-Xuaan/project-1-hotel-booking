# OCR-02 — nghiệm thu sandbox local 29/10/2026

**Kết quả: PASS cho phạm vi OCR-02 local/synthetic.** User là người quyết định cuối cùng, đã cho phép sửa mọi module cần thiết và không yêu cầu sign-off từ thành viên khác. Quyết định này gỡ approval gate; kết luận kỹ thuật bên dưới dựa vào test thực, không giả Member 3/Member 2 đã ký duyệt.

## Môi trường và baseline

- Source worktree: C:/Users/admin/.codex/worktrees/m1-completion-a00/project-1-hotel-booking, detached HEAD/base5509af2e26daca6dd67a32acb987a86648808df2. Không import về D: hoặc commit/push.
- Docker project mới **hotel-ocr02-final**, volumes riêng; Postgres15432/hotel_acceptance_db, Redis16379, Keycloak18081/hotel-acceptance; backend18080, frontend13113. Tất cả bind loopback. Không dùng database/Redis/Keycloak project khác.
- Full Spring Boot application thực, profile ocr-local và image OCR bật, Tesseract local vie+eng. Scheduling tắt, external AI key gỡ khỏi launcher, object storage local. Không gọi OCR cloud.
- Migrations repository master-data và fixtures synthetic chạy trong DB sandbox mới. Các cổng/project được kiểm trước khi dùng. Migrations chưa áp vào deployment nào khác.

## Đã sửa thêm sau H020

Test không mock đã tái hiện save HTTP400 do schema legacy: first_name/last_name bắt buộc, sau đó check constraint chỉ đọc identity_type/identity_number trong khi API/entity dùng full_name/document_type/document_number. Đây là lệch schema của consumer thật, test mock trước không bắt được.

Đã thêm 2 incremental XML/SQL changesets theo convention, không sửa changeset cũ:

- `backend/src/main/resources/db/changelogs/incremental/2026/20261029-193000-stay-guest-legacy-name-compatibility.xml` và SQL cùng basename trong `db/sqlFile/incremental/2026/`: bỏ NOT NULL trên first_name,last_name,guest_type legacy; full_name vẫn bắt buộc, giữ cột/dữ liệu cũ.
- `backend/src/main/resources/db/changelogs/incremental/2026/20261029-193100-stay-guest-identity-compatibility.xml` và SQL cùng basename: người lớn vẫn phải có bộ identity hoàn chỉnh; chấp nhận cặp document_type/document_number hiện hành hoặc cặp identity_type/identity_number legacy. Không tự điền thông tin giấy tờ.
- `frontend/e2e/browser/ocr-full-runtime.spec.ts`: opt-in OCR_FULL_APP=true, bắt buộc đúng Docker project riêng. Browser mount chính StayGuestsModal với booking-room fixture thật, không intercept API. Kiểm 7 trường từ response OCR thật, chỉnh tên, checkbox, thời gian, không tăng guest count trước submit; submit tạo guest và lodging queue thật, xác nhận DB, dọn đúng hai record vừa tạo trong finally. Test RBAC/logout xác minh Keycloak + production blacklist service/filter + Redis thật.

Các source có trước giữ nguyên SHA256. Không thay parser/frontend contract để né regression.

## Kết quả cuối

| Kiểm tra | Kết quả |
|---|---|
| Playwright 5 API +5 component +2 full-runtime | **12/12 PASS**,0skip. Log Temp/ocr02-accepted-browser-api.log. Full-runtime dùng app/HTTP/DB/Redis/Keycloak/Tesseract thật. Component tests vẫn mock API theo mục đích hồi quy lỗi UI. |
| Maven OCR8classes +StayGuestServiceTest | **90 tests:89 PASS,1 SKIPPED,0fail/error**. Case Keycloak trong servlet harness skip vì không truyền env; Keycloak thực đã được kiểm bằng full-runtime phía trên. Temp/ocr02-final-regression.log. |
| npm run build | PASS, Temp/ocr02-accepted-build.log |
| npm run lint | PASS, Temp/ocr02-accepted-lint.log |
| Liquibase migration / full app startup | PASS, Temp/ocr02-fullapp-accepted.log |
| DB identity constraint negative probe | PASS: adult không có identity bị check_violation, không lưu row |
| Backup/restore | pg_dump custom trước migration; restore vào DB ocr02_restore_verify trong cùng container riêng thành công, query stay_guests count=1. File Temp/ocr02-before-compat.dump. |
| git diff --check / preservation | PASS; mọi file repository có trước giữ nguyên, chỉ thêm migration/test/report; handoff/plan append riêng. |

Lịch sử thất bại giữ trong Temp/ocr02-full-e2e.log (schema), ocr02-full-verified-tests.log (identity constraint). Một lượt API chạy trước backend ready bị connection refused, log ocr02-full-final-tests.log; đã đợi app Started rồi chạy cuối12/12. Không giảm assertion hoặc threshold để đạt.

Full-runtime command từ frontend sau khi full backend/profile/fixtures sẵn sàng:

```powershell
$env:VITE_BASE_URL='http://127.0.0.1:13113'
$env:VITE_API_URL='http://localhost:18080'
$env:OCR_FULL_APP='true'
$env:OCR_IMAGE_ENABLED='true'
npx playwright test e2e/browser/ocr-full-runtime.spec.ts e2e/browser/ocr-review.spec.ts e2e/api/ocr-synthetic.spec.ts
```

Maven từ backend: `.\mvnw.cmd '-Dtest=OcrUploadValidatorTest,IdentityDocumentOcrServiceTest,TesseractOcrVisionAdapterTest,TesseractTsvParserTest,IdentityDocumentOcrControllerTest,U13ZeroWriteAndPiiSafetyTest,OcrMultipartServletRuntimeTest,OcrMultipartPrivacyGuardTest,StayGuestServiceTest' test`, TESSERACT_EXE trỏ binary local.

## Finding và quyết định

- REV-005-F001/OBS-OCR02-01: CLOSED theo review H020; parser regression tiếp tục PASS.
- REV-004-F001: CLOSED cho scope local: quyết định owner đã được user thay thế + browser consumer edit/human verify/save DB thật đạt. Không tự nhận Member 3 ký duyệt.
- REV-004-F002: CLOSED cho deployment target sandbox đã kiểm: full Boot nạp ocr-local, startup privacy guard hoạt động, cap/memory servlet regression đạt; production JWT/filter và Redis revocation đạt qua full app. User chấp thuận cấu hình local. Không khẳng định Redis outage hoặc production load đã được kiểm.

## Giới hạn và rollback

PASS phạm vi OCR-02 local theo G1 synthetic; không phải nghiệm thu toàn FIT hoặc production/SRS. UI test mount consumer thật, không bao gồm điều hướng toàn trang BookingList; không có ảnh định danh thật. Profile multipart cap5/request6/threshold6MiB áp toàn bộ upload trong process: deployment có upload lớn cần profile/phân tách phù hợp; không tự áp profile này vào production. Không benchmark tải đồng thời/Redis outage, không đặt các kiểm đó thành PASS.

Schema rollback có trong XML nhưng sẽ bị DB từ chối nếu record mới có NULL legacy fields hoặc chỉ modern identity. Vì vậy chỉ rollback khi dữ liệu tương thích đã được owner quyết định, hoặc restore snapshot trước migration trên target riêng. Không tự bịa/backfill tên hay identity. Đã thử restore snapshot độc lập, không làm mất dữ liệu sandbox đang kiểm.

Sandbox và local application sẽ dừng sau kiểm chứng; giữ volumes/snapshot để tái lập, không xóa volume. Repo changes giữ chưa commit để user review.

Đính chính giao tiếp: kiểm kê handoff thực tế H001–H020 cho thấy chỉ một H020. Nhận định trước đó rằng mã H020 bị trùng là sai; không renumber lịch sử. Ghi các event tiếp theo theo số thực tế.


## Delivery follow-up — 29/10/2026

Thêm changeset `20261029-194000-preserve-stay-guest-type-required` (XML/SQL) giữ guest_type NOT NULL vì entity đã map discriminator này và default ADULT. Hai changesets đã áp trước được giữ nguyên checksum; thứ tự mới khôi phục invariant thay vì sửa migration lịch sử. Nếu môi trường đã có guest_type NULL thì changeset dừng, không tự phân loại/backfill. Full-name và adult identity constraints vẫn bắt buộc. Bước bàn giao tiếp theo kiểm chứng trên repoD rồi ghi kết quả/commit vào delivery report riêng.


Bàn giao cập nhật: xem [ocr02-delivery-2026-10-29.md](ocr02-delivery-2026-10-29.md). Code đã commit và fast-forward vào repoD, đã kiểm lại full runtime từ repoD; mô tả chưa tích hợp ở các đoạn trên là lịch sử.
