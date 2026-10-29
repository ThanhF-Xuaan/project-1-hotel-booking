# OCR-02 — bàn giao đã tích hợp 29/10/2026

**Kết quả: PASS local/synthetic, đã tích hợp vào repo chính D:.** Đây là báo cáo mới nhất; các report trước giữ như lịch sử.

## Git / nơi review

- Implementation commit: `0e23d093b4279c796a614ff46d4557d404c62259`, branch `codex/m1-ocr02-complete`, base `5509af2e26daca6dd67a32acb987a86648808df2`.
- 34 files OCR được commit riêng, bao gồm OCR-01 runtime dependency, OCR-02 consumer, tests/synthetic fixtures, contract và báo cáo. Không đưa group-assignment/T12/U22 contracts vào commit.
- Repo D:/Project/University/project-1-hotel-booking, branch `codex/fix-readonly-availability`, đã fast-forward tới implementation commit; tiếp theo có commit documentation delivery. Không merge origin/develop/shared branch hoặc push remote.
- Repo D có staged/dirty work từ trước ở inventory và docs; giữ nguyên SHA256 ngoài incoming OCR paths và staged binary diff byte-for-byte. Test chạy trên working tree tích hợp này, có các thay đổi cũ được bảo vệ; không tuyên bố toàn project là clean.
- Source worktree m1-completion-a00 vẫn giữ các contract không liên quan untracked; không clean/stash/reset.

## Hành vi hoàn tất

- OCR ảnh local Tesseract có bounded size/output/timeout/concurrency; flag mặc định tắt, không fallback text khi engine lỗi lúc flag bật.
- Bóc tách7field, confidence/warnings, human verification; parser name/nationality không nuốt nhãn kế tiếp, bao gồm nhãn trống LF/CRLF.
- StayGuestsModal nhận multipart draft, sửa và checkbox trước submit; không auto-save. Hủy/đóng form abort và xóa draft.
- Startup guard yêu cầu multipart memory profile khi bật image OCR. Profile ocr-local: file5MiB/request6MiB/threshold6MiB, áp mọi upload trong process.
- Schema tương thích với full_name/document_type/document_number và dữ liệu legacy, vẫn cần full_name, guest_type và giấy tờ người lớn.

## Hiệu chỉnh migration trong lượt bàn giao

Hai changesets193000/193100 đã áp ở sandbox trước được giữ checksum nguyên vẹn. Thêm changeset194000-preserve-stay-guest-type-required (XML/SQL) để khôi phục NOT NULL guest_type, vì entity đã map và default ADULT; không cần nới field này. Changeset không đoán/backfill NULL: target có NULL phải xử lý dữ liệu có chủ đích trước khi chạy.

Trên DB sau cả3migration: first_name,last_name nullable; full_name,guest_type NOT NULL. Negative probes thật xác nhận adult thiếu identity và row guest_type NULL đều bị từ chối. Không sửa migration cũ hoặc thay schema trực tiếp ngoài Liquibase.

## Kiểm chứng lại trên repo D:

| Kiểm tra | Kết quả |
|---|---|
| Maven8OCRclasses +StayGuestServiceTest | **90 tests:89PASS,1SKIP,0fail/error**, BUILD SUCCESS |
| Playwright5API+5component+2full-runtime | **12/12PASS**,0skip |
| TypeScript/Vite build | PASS |
| ESLint toàn frontend | PASS |
| Full Boot startup + migration194000 | PASS trên sandbox riêng |
| Negative constraint probes | PASS: giữ adult identity và guest type |
| Snapshot/restore trước194000 | PASS: pg_dump, restore vàoocr02_delivery_restore_verify, query count guest=1 |
| git diff --check + preservation | PASS |

Log ngoài repo tại C:/Users/admin/AppData/Local/Temp/:
- ocr02-delivery-d-maven.log
- ocr02-delivery-d-e2e.log
- ocr02-delivery-d-build.log
- ocr02-delivery-d-lint.log
- ocr02-delivery-d-fullapp.log
- ocr02-delivery-before-third.dump
- ocr02-delivery-d-before.json vàocr02-delivery-d-index-before.patch

Maven case realKeycloakTokenAndJwksAuthorizeOnlyReceptionRole trong servlet harness bị skip vì không cấp env tokens cho harness. Full-runtime test riêng chạy Keycloak/production JWT filter/Redis thật:401missing,403wrong role,200reception, logout rồi401revoked. Không gọi90/90PASS.

Full-runtime browser mount component StayGuestsModal thật với booking-room fixture thật; API không intercept, lưu guest/lodgingqueue thật và dọn đúng record test. Kiểm7field, edited name, thời gian, checkbox và guest count không tăng trước submit. Component regression riêng vẫn mock lỗi để kiểm503/unreadable/shape/cancel. Không bao gồm toàn điều hướng BookingList/FIT.

Sandbox giữ Docker project hotel-ocr02-final, PostgreSQL15432/Redis16379/Keycloak18081; backend18080/frontend13113 chạy từ repoD. Chỉ fixture synthetic, không identity thật hoặc OCR cloud. Đã dừng process/container sau verification; giữ volumes và snapshot để tái lập. Không mutation database khác.

## Chạy lại và rollback

Dùng profile ocr-local và HOTEL_AI_OCR_ENABLED=true, TESSERACT_EXE là binary vie+eng. Thiết lập datasource/Redis/Keycloak tới sandbox được xác minh; tắt scheduler, cloudAI và dùng storage local. Khi cần migrations phải snapshot target trước; không chạy startup với Liquibase enabled vào DB tùy ý.

Từ frontend với VITE_API_URL=http://localhost:18080, VITE_BASE_URL=http://127.0.0.1:13113, OCR_FULL_APP=true, OCR_IMAGE_ENABLED=true: `npx playwright test e2e/browser/ocr-full-runtime.spec.ts e2e/browser/ocr-review.spec.ts e2e/api/ocr-synthetic.spec.ts`.

Rollback ứng dụng bằng flag ảnh false; giữ memory profile nếu tiếp tục upload identity bằng legacy text flow. Rollback schema chỉ khi data tương thích; XML rollback sẽ từ chối nếu records hiện hành không còn phù hợp NOT NULL hoặc legacy identity. Không tự bịa tên/identity để rollback; snapshot restore đã kiểm là phương án cho target riêng.

Giới hạn: PASS local/synthetic, không production rollout, không stress200CCU/Redis outage hoặc full SRS. Những mục này không còn là blocker của scope OCR-02 đã được user phê duyệt. Approval/sign-off member khác được thay bằng quyết định user, không giả họ đã review.
