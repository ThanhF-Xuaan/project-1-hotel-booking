# Nghiệm thu tái lập sau PR #28

- **CONFIRMED baseline:** `develop@e027e90bb56a9320e1271614761b8b97f285853e`, cây file trùng commit đã nghiệm thu `5b8eca4`.
- **Owner/ủy quyền:** người dùng phụ trách toàn project, cho phép tiếp tục công việc hợp lý sau khi tự merge PR #28. Slice này chỉ sửa tooling nghiệm thu; không sửa API/schema, gold, thuật toán hoặc frontend nghiệp vụ.
- **CONFIRMED gap:** Test.ps1 hiện chạy Maven chọn lọc sau khi app đã mở; PostgreSQL IT cần app dừng và biến chuyên biệt. Initialize-AcceptanceRuntime đặt generic DB/Redis env nên Maven default-config test có thể bị nhiễm. Lần chạy trước phải điều phối thủ công.

## Allowlist và trình tự

1. `tools/acceptance/Start.ps1`: tách tùy chọn dựng hạ tầng/khởi chạy app, giữ mặc định hiện hữu.
2. `Snapshot.ps1`, `Restore.ps1`: nhận snapshot riêng theo lần Verify; không ghi đè bản đó. Giữ cơ chế snapshot mặc định của quy trình từng bước/Reset hiện hữu.
3. `Test.ps1`: xuất JSON cho supervisor, báo cáo có tên riêng; skip Maven vẫn PARTIAL/exit 1.
4. `Run-BackendTests.ps1` (mới): process riêng, làm sạch env liên quan, chạy `clean test`, kiểm chứng Surefire thật và bắt buộc PostgreSQL 3/3 + Redis 4/4.
5. `Verify.ps1` (mới): preflight/lock, chỉ chấp nhận sandbox cố định đã dừng, dựng infra → snapshot → Maven → app → suites còn lại + source audit/npm audit → dừng app → restore → dừng container. Lỗi restore giữ hạ tầng để khôi phục và verdict FAIL.
6. README, báo cáo slice này và báo cáo kết quả timestamp mới trong `docs/member1/acceptance/runs/`.

## Tiêu chí, kiểm tra và rollback

- Một lệnh chạy hết kiểm tra local, ghi SHA, hash scripts, lệnh, exit code, trạng thái từng suite, skip thật; không gọi Gemini.
- Không chạy khi app/container đã hoạt động hoặc cổng bị chiếm. Không kill theo cổng; dừng theo manifest đã xác thực.
- Snapshot trước test và restore khi xong; không xóa Docker volume. Dừng nếu không tạo snapshot hoặc không xác thực đích DB/Redis.
- Chạy parser PowerShell trước; chạy supervisor thật với generic DB/Redis env cố ý sai để xác minh cách ly. Kiểm tra report, listeners, container và Git sau cleanup.
- Các kiểm tra thành phần đã PASS trước merge không cần lặp riêng; lần chạy supervisor mới xác minh việc điều phối vừa sửa.
- Rollback: hoàn tác riêng diff tooling trên branch `codex/m1-repeatable-acceptance`; không reset/clean/stash. Giữ 8 file untracked cũ. Chưa commit/push/PR.
- T05 mẫu chính thức, U14 consent/history thật và gold/provider approval giữ trạng thái cũ. Harness T12 synthetic 74 case đã có, không được đồng nhất với gold đã duyệt hoặc model accuracy.

## Kết quả

**CONFIRMED — PASS**, run `20261025-213907-808-024fdc71`, exit 0. [Báo cáo thực thi](runs/acceptance-20261025-213907-808-024fdc71.md). HEAD/base vẫn `e027e90`; script hashes trong báo cáo khớp chính xác các file hiện tại. Mã nghiệp vụ/backend/frontend, API, dependency, schema, fixtures và gold không đổi.

Lệnh tại repo chính:

```powershell
pwsh -NoProfile -File tools/acceptance/Verify.ps1
```

Lần kiểm chứng cố ý đặt DB_PORT=1, REDIS_HOST=unreachable.invalid và REDIS_PORT=1 ở process gọi. Compose vẫn dùng manifest acceptance; Maven process bỏ generic env trước khi gắn biến IT riêng. Không sửa DockerNetworkConfigTest hay cấu hình dev để đạt kết quả.

| Kiểm tra thực chạy | Kết quả |
|---|---|
| Full Maven `clean test` | PASS: 943 test, 0 failure/error, 1 skip `HotelBookingApplicationTests.contextLoads` có annotation Disabled hiện hữu. Backend thật được khởi chạy và kiểm tra API/browser riêng. |
| PostgreSQL/Redis IT | PASS: PostgreSQL 3/3 và Redis 4/4, không skip; đích và connection khác được kiểm tra trước khi đặt exclusive flag. |
| T12 synthetic harness trong Maven | PASS 74/74 với digest dataset giữ nguyên; draft/human approval không đổi, không đo model quality. |
| T12 source/artifact/portability | PASS: 39 route nodes, 22 lazy components, 4 default aliases; validator 184 instance/74 case; portability 11/11. Artifact validator vẫn chỉ kiểm tra artifact. |
| Lint/build/audit | PASS: 0 lint errors/warnings; entry 246.80 kB, không warning >500 kB; npm audit 0 vulnerability. |
| Playwright API/browser | PASS: 16/16 API, 6/6 browser với ứng dụng thật, dữ liệu giả lập. |
| Restore/cleanup | PASS: snapshot riêng được khôi phục; 2 hotel, 5 room type, 305 availability, 5 staff. Không còn manifest ứng dụng/listener trên 5 cổng; không còn container acceptance; 3 volume giữ nguyên. |
| Parser/Git/scope | PASS: mọi PowerShell script parse; diff whitespace sạch; 8 file untracked cũ giữ nguyên hash; lịch sử report không bị ghi đè. |

## Kiểm tra điều phối và lỗi trong quá trình phát triển

- Fixture test dưới runtime bị ignore, sao chép Verify/Runtime vào một thư mục project giả riêng: sai DB host bị từ chối trước bất kỳ stage nào; khi giữ file handle độc quyền, lượt Verify thứ hai bị chặn. Đây là kiểm tra guard, không phải chạy Docker hay test nghiệp vụ giả.
- Listener giả lập trên 18080 bị phát hiện; Verify trả exit 1 và listener vẫn sống cho tới khi test harness tự đóng. Không kill theo cổng.
- Harness dùng đúng function Invoke-Stage trích từ AST source: child exit 23 được ghi FAIL/exit 23; launcher có tiến trình con chạy nền trở về sau 0,5 giây trong khi tiến trình con còn sống. Harness xác minh command line của child do nó tạo rồi dừng child đó.
- Test.ps1 từ chối ReportPath đã tồn tại trước khi kiểm tra container/API, trả exit 1; hash report lịch sử không đổi. JSON và Markdown không dùng chung output path; lỗi ghi evidence chặn kết luận PASS.
- Ba lượt trước đều giữ verdict FAIL và snapshot đã restore: [212753](runs/acceptance-20261025-212753-441-3694be45.md), [213017](runs/acceptance-20261025-213017-069-3ac75ac5.md), [213247](runs/acceptance-20261025-213247-040-d561cb0e.md). Hai lượt đầu gặp nhiều bản Node trong PATH; resolver đã chọn đúng một executable. Thử kiểm tra lock ban đầu diễn ra sau lúc lượt trước đã xong nên không phải bằng chứng concurrency; kiểm tra giữ handle chủ động sau đó mới được tính PASS.
- Lượt thứ ba bị native piping giữ handle của app chạy nền dù launcher đã thoát. Đã dừng đúng app thuộc manifest để giải phóng handle, nên lượt đó không đủ điều kiện chạy API/browser và được giữ FAIL. Supervisor hiện dùng Start-Process ẩn, redirect log và WaitForExit của launcher; không dùng `Start-Process -Wait` vì nó chờ cả cây tiến trình.
- Stdout/stderr của các stage script nằm riêng trong runtime; console Windows có một số thông báo tiếng Việt phụ thuộc code page. JSON/Markdown được ghi UTF-8; exit code, class/count và assertions đã xác nhận độc lập với các dòng hiển thị này.

Các log nằm trong `tools/acceptance/.runtime/runs/<run-id>/`; metadata không chứa secret. Runtime config và snapshot không đưa vào Git. Kiểm tra guard/launcher riêng nằm trong `verify-guard-fixture/` và `stage-launch-harness/` dưới runtime.

## Trạng thái bàn giao

Trạng thái tại thời điểm ghi kết quả, trước bàn giao Git: nhánh `codex/m1-repeatable-acceptance` dựa trên PR #28 đã merge; các thay đổi tooling/docs chưa commit, push hoặc tạo PR. Trạng thái commit/push sau đó xem Git; nội dung PR có sẵn trong [bản bàn giao](repeatable-runner-pr.md). Không yêu cầu chạy lại các suite sau khi chỉ cập nhật phần diễn giải tài liệu này. T05 official format, U14 consent/history thật và gold/provider sign-off giữ nguyên giới hạn đã nêu.
