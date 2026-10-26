# chore: tự động hóa nghiệm thu local đầy đủ

## Vấn đề và kết quả

Quy trình cũ chạy Maven chọn lọc sau khi ứng dụng đã mở, còn kiểm tra PostgreSQL/Redis thật phải điều phối riêng. `Verify.ps1` hiện chạy toàn bộ quy trình trên sandbox local cố định: hạ tầng/fixture → snapshot riêng → Maven đầy đủ → source/dependency audit → ứng dụng thật → validator, lint/build, API/browser → dừng app → restore → dừng container.

Maven chạy trong process riêng, bỏ generic DB/Redis/app env và xác minh các integration test thực chạy qua Surefire. Runner chặn sandbox đang hoạt động, cổng bị chiếm, cấu hình sai đích và lượt Verify đồng thời. Cleanup dừng ứng dụng theo manifest; lỗi ownership/restore giữ hạ tầng để kiểm tra và trả FAIL.

Snapshot và báo cáo Verify có tên riêng; report cũ được giữ. Test.ps1 riêng vẫn trả PARTIAL/exit 1 khi skip Maven. Supervisor chỉ chấp nhận sau khi đối chiếu đủ evidence Maven và từng suite còn lại.

## Kiểm chứng

Lệnh: `pwsh -NoProfile -File tools/acceptance/Verify.ps1`.

- PASS Maven 943 test, 0 failure/error, 1 skip contextLoads có sẵn; PostgreSQL 3/3, Redis 4/4 thực chạy.
- PASS T12 source audit, validator 184 instance, portability 11/11.
- PASS lint/build; npm audit 0 vulnerability; không còn warning bundle >500 kB.
- PASS Playwright API 16/16 và browser 6/6 với ứng dụng thật/dữ liệu giả lập.
- PASS guard cấu hình/cổng/khóa, truyền exit code, chờ launcher và bảo vệ report cũ.
- PASS restore và teardown; volume giữ lại, không còn app/container/listener acceptance.

Kiểm chứng tại base `e027e90bb56a9320e1271614761b8b97f285853e` cộng diff tooling; [report cuối](runs/acceptance-20261025-213907-808-024fdc71.md) có SHA và hash scripts. [Hồ sơ](repeatable-runner-2026-10-25.md) giữ cả các lượt runner FAIL trước khi sửa.

## Giới hạn và rollback

Chỉ dùng sandbox acceptance có thể khởi tạo lại. Snapshot được tạo sau migration/seed, không phải backup dữ liệu trước khi khởi tạo sandbox. T05 mẫu chính thức, U14 consent/history thật và gold/model approval tiếp tục chưa được nghiệm thu. Log console Windows có một số thông báo tiếng Việt phụ thuộc code page; JSON/Markdown dùng UTF-8.

Rollback bằng revert commit tooling; phục hồi bằng snapshot riêng của lần Verify khi cần. Các file untracked có sẵn của người dùng được giữ ngoài commit.

Base PR: `develop`; head: `codex/m1-repeatable-acceptance`. Người dùng tự tạo và accept merge PR.
