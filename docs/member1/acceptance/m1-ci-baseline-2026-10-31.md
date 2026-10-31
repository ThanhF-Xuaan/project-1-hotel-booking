# Member 1 — Báo cáo CI, 31/10/2026

**Verdict: PASS trong phạm vi CI đã xác định.** Lượt GitHub [41209464996](https://github.com/ThanhF-Xuaan/project-1-hotel-booking/actions/runs/41209464996) tại head `d4209425c41d0a00d534d4cd6d601e31552f388e` đạt cả hai job. Đây là bằng chứng chạy trên Linux, tách biệt với kiểm thử local của PR #42. PR bàn giao: [#43](https://github.com/ThanhF-Xuaan/project-1-hotel-booking/pull/43); kết quả kiểm tra head cuối và merge được ghi trong PR và handoff local.

## Phạm vi và thay đổi

- Base `62b796085618dcb8a76a072db87677660292d57a`, branch `codex/m1-ci-20261031`, worktree ban đầu sạch. User đã cho phép triển khai, tự review/sửa, commit, push, PR và merge develop. Không ghi nhận tự review là phê duyệt của một người khác.
- `.github/workflows/member1-ci.yml`: chạy khi PR/push vào develop/main hoặc chạy thủ công. Backend dùng Java 21, Tesseract eng/vie thật và Redis riêng; frontend/contract dùng Node 24, Python 3.12 và đầy đủ lịch sử Git cho kiểm tra nguồn.
- `tools/ci/verify_surefire.py` và test tương ứng: từ chối báo cáo thiếu/sai cấu trúc, số đếm không khớp, suite trùng/rỗng, thiếu suite OCR/Redis, có failure/error/skip hoặc ít hơn 1.391 test. Artifact chỉ có tổng số; không chứa XML, properties hay stdout của test.
- `tools/ci/requirements.txt`: khóa hai dependency trực tiếp của validator. Không nâng phiên bản dependency ứng dụng.
- `.gitattributes`: giữ đúng CRLF cho 42 đường dẫn đã có hash theo CRLF. Không sửa nội dung nguồn, tài liệu được bảo vệ, bộ case, manifest, hash, assertion hay ngưỡng.
- Kế hoạch và allowlist: [m1-ci-baseline-2026-10-31.md](../plans/m1-ci-baseline-2026-10-31.md).

## Review fail → fix → review pass

Lượt đầu [41209166024](https://github.com/ThanhF-Xuaan/project-1-hotel-booking/actions/runs/41209166024) FAIL ở cả hai job. Validator từ chối `.agents/agents/pii-auditor.md`; backend dừng khởi tạo bộ 74 case T12 do hash `cases.json` khác. Nguyên nhân được xác nhận: Git checkout LF trên Linux trong khi bằng chứng cũ khóa CRLF. Bộ kiểm tra báo cáo mới cũng FAIL vì chỉ có 1.318 test và test lỗi; không hạ mức tối thiểu để vượt gate.

Đã thêm quy tắc checkout cho đúng các đường dẫn có hash CRLF, bao gồm `eval/t12/r1/v0.1.0-draft/cases.json` (SHA-256 giữ nguyên `8b8f086d5da5f08679f63053e9d17dbc1cbaa510b68a8c16c68e2da680c6bf5c`). Checkout mới với `core.autocrlf=false` chạy validator PASS và Git sạch. Lượt GitHub kế tiếp PASS; nội dung Git của manifest, case, validator và production source được đối chiếu với base, không thay đổi.

| Kiểm tra thực chạy | Kết quả |
|---|---|
| Backend selected tests trên GitHub | PASS: 1.391 test / 173 suite; 0 failure, error, skip |
| Tesseract thật, fixture giả lập | PASS: 9 test adapter, gồm nhận diện CCCD/passport và ảnh khó đọc |
| Redis thật, instance riêng | PASS: 4 test; có kiểm tra PING và database trống trước khi chạy |
| Report guard trên GitHub và local | PASS: 6 test, gồm trường hợp âm và chống xuất dữ liệu nhạy cảm |
| Tooling contract/provenance trên GitHub và local | PASS: 49 test; validator kiểm tra 23 hash bảo vệ và 33 negative case |
| Current Core, lazy route và U14 source audit | PASS trên GitHub và local |
| Frontend lint / build | PASS trên GitHub và local; lint không cho phép warning |
| npm audit --audit-level=high | PASS; vẫn còn 12 cảnh báo moderate trong chuỗi dependency tooling |
| actionlint 1.7.12, diff/allowlist review | PASS local; binary actionlint tải từ release chính thức và đối chiếu checksum |

Lệnh backend trong workflow chọn `*Test` và loại đúng ba class: `HotelBookingApplicationTests`, `LodgingExportPostgresTest`, `OcrMultipartServletRuntimeTest`. Full Boot context, PostgreSQL có fixture, servlet/Keycloak runtime và các class `*IT` PostgreSQL tùy chọn **NOT RUN trong CI này**. Chúng vẫn có lệnh/bằng chứng local riêng từ PR #42; không cộng vào 1.391 test CI và không coi CI thay thế chúng. Browser/API E2E, benchmark, provider Gemini thật và triển khai production cũng không thuộc workflow này.

## Security, giới hạn và bàn giao

CONFIRMED: token workflow chỉ có quyền đọc repository, checkout không giữ credential, action được khóa bằng commit SHA và Redis bằng image digest; runner/service tạm thời, timeout hữu hạn, không nhận secret triển khai. Artifact tổng hợp giữ 7 ngày. Không dùng `pull_request_target`, không deploy, không chạy Liquibase hoặc truy cập dữ liệu thật. Không thay auth, API, schema hay logic Booking/Pricing/Inventory. Workflow tạo check; không tự cấu hình quyền admin hoặc branch protection.

OCR-02 giữ COMPLETE/MERGED từ PR #40. Tiến độ feature trước CI vẫn 15/16 task local; V03 business integration **NOT READY** do thiếu dữ liệu capacity/stays lịch sử, utility unit/day/reset/epoch, Group physical bed/floor/all-night quota/IAM và mẫu xuất BCA chính thức. Quyền quyết định của user đã có; dữ liệu và bằng chứng nghiệp vụ không thể thay bằng quyền phê duyệt hoặc fixture giả lập. Không công bố toàn SRS/production hoàn tất.

Evidence local: `C:/Users/admin/.codex/outputs/member1-ci-2026-10-31/` gồm preflight, local results, log FAIL/PASS, checkout độc lập, aggregate artifact và receipt bàn giao cuối. Canonical handoff được append riêng sau delivery, giữ nguyên lịch sử. Rollback: revert thay đổi CI qua PR; không cần rollback DB hoặc runtime. Không reset/stash/force-push hoặc sửa worktree của người khác.
