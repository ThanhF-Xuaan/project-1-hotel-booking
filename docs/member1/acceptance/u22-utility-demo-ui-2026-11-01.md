# U22-D04 — Giao diện demo điện/nước

**PASS local/synthetic, mode bật và tắt.** User ủy quyền frontend/cross-member decisions và implement→review/fix→commit/push/PR/merge, mock trước/dữ liệu thật sau. Không giả member sign-off hoặc human peer review. [Plan/allowlist](../plans/u22-utility-demo-ui-2026-11-01.md).

Base develop `5af00a7ab8652a58a488dc9a1bda59f2d5e1a2ad`, branch `codex/m1-u22-utility-demo-ui-20261101`, worktree ban đầu sạch. U22-D03 API/engine là dependency read-only đã merge; không sửa backend, T04/schema/shared IAM, model/threshold, fixture/gold, package/lockfile hoặc old tests. Không reset/stash/overwrite repo D hoặc worktrees khác.

## Hành vi

- Lazy route `/ai/demo/utility-anomalies` dưới CHAIN_ADMIN, admin sidebar **Điện/nước bất thường (Demo)**. Trang riêng responsive có link về dashboard. Anonymous về login; customer/reception bị guard chặn trước request. Backend vẫn kiểm JWT/role thật; mock JWT trong browser test không phải IAM evidence.
- Service dùng apiClient hiện có, chỉ GET `/api/v1/ai/demo/utility-anomalies?scenario=...`. Chọn12aliases cùng labels đơn vị từ contract. Metadata tĩnh giúp chọn kịch bản trước khi API bật, không fallback dữ liệu. Validate envelope/SYNTHETIC/businessData=false/scenario/catalog/unit/fixture hotel+meter/clock/version/42dates, point state/null/numeric bounds/reference counts sau barrier và summary counts. Không tính lại median/MAD/threshold hoặc classification.
- Notice luôn ghi dữ liệu mô phỏng, không đồng hồ thật hoặc kết luận gian lận. Mốc31/10/2026 lúc00:00 Việt Nam, phân tích19/09–30/10/2026. Counts tách evaluated/needsReview/insufficient/excluded; ANALYZED không nghĩa mọi ngày đã score. LOW_COVERAGE không được gọi bình thường. Reset/replacement/invalid barrier kiểm lại số điểm tham chiếu, không dùng trạng thái cũ thay thế.
- Chart dùng usage, median và threshold từ server; đường median±threshold chỉ để trình bày, lower bound giới hạn0 và có mô tả. Anomaly có marker hình thoi, danh sách ngày nổi bật và trạng thái/lý do trong bảng42ngày, không chỉ dựa màu. Excluded tạo gap, warmup vẫn có thể có usage. Bảng kWh/m³, tối đa8chữ số thập phân giữ giá trị ngưỡng một phần nghìn khi định dạng vi-VN. Không gọi dải tham chiếu là confidence interval hoặc calibration thật.
- Loading/error/retry/404disabled/403/503/network/malformed và expired session; AbortController/request key ẩn data cũ ngay khi đổi/tải lại, bỏ late response/unmount. Không render message backend hoặc arbitrary HTML/URL. Chart/table có vùng scroll/focus, labels/captions/headers, mobile375/reduced motion và desktop1440. Không khẳng định đã kiểm dark mode hoặc toàn bộ screen readers.

## Review và verification

Targeted browser **20/20 PASS**: units/gaps/markers/mobile/keyboard, cancellation, safe errors,8malformed payload cases, decimal floor, reset/low coverage, network/expired session, anonymous/customer/reception. Đây là mock UI data, không kiểm lại thuật toán hoặc Keycloak. Route/tamper **6tests PASS** gồm3old+3new; old tooling49tests PASS. Không sửa old assertions/gold. Current-route helper kiểm exact utility delta ở đúng vị trí, chiếu về App tại pre-utility SHA, rồi delegate chuỗi occupancy/T12 đã khóa.

Review ảnh mobile phát hiện select label bị cắt khi chung hàng với nút. Sửa full-width select trên mobile; thêm danh sách ngày cần kiểm tra trước chart để không phụ thuộc scroll tới marker. Marker legend đổi sang SVG. Làm rõ MAD/ngưỡng bằng ngôn ngữ người dùng trước browser stage; chạy lại lint/build trên copy cuối. Không có test assertion bị nới hoặc threshold thay đổi để PASS. Hash bản cuối được đối chiếu sau runtime và trước delivery, backend giữ nguyên.

| Kiểm tra | Kết quả |
|---|---|
| Full Verify both-on | PASS [run20261101-153800-210-818a13bd](runs/acceptance-20261101-153800-210-818a13bd.md):1451backend/34API/69browser,0failure/error/skip trong executions |
| Keycloak thật /12scenarios/UI vs API/digest | PASS admin/sidebar/all12/units/last-day values/markers match API; customer/reception denied, business data-sequence digest unchanged |
| Utility-off/occupancy-on riêng | PASS4occupancy API+5utility API+4runtime browser=13checks, không cộng vào full; admin thấy disabled, nonadmin vẫn403UI, occupancy vẫn hoạt động |
| Source/contract/lint/build/audit | PASS full Verify và final copy lint/build riêng,6route+49tooling,actionlint,copy validation.0ESLint error/warning;12moderate baseline/0high-critical |
| Cleanup | PASS full/off snapshots restore, original config khôi phục nguyên byte, regenerated compose theo original; owned services/apps/listeners/manifest stopped |
| GitHub CI | Giữ minimum backend1443; command chạy18old occupancy+20new utility mock browser,6route tests. Exact-head/post-merge proof ở PR/receipt/handoff |

Logs/source hashes/ảnh/mock/runtime/canonical-config backups/receipt ở `C:/Users/admin/.codex/outputs/u22-utility-demo-ui-2026-11-01/`. Credentials/snapshots không vào Git. Git delivery cuối đọc từ PR/receipt/handoff; không suy local PASS thành Linux runtime proof. UI utility mới3files, route/link App/Sidebar,2browser specs, utility exact-route helper+test/composition, workflow, plan/report/README/run report; file list đầy đủ ở PR/review.json cuối.

69browser gồm47old+20new mock+2new runtime tests; các subset không cộng thêm. Một real browser test lặp12scenario không bị đếm thành12tests.1451backend là clean test và context smoke riêng; child application-suites PARTIAL/exit1 vì skip Maven đã chạy riêng, supervisor kiểm6suite thực thi PASS cùng backend.json/restore mới kết luận PASS. Không test FAIL trong slice này; review NEED_FIX về mobile/copy đã sửa rồi final browser/lint/build PASS. Self-review không còn finding chặn trong scope demo UI; đây không phải human peer approval.

Để xem: đăng nhập admin sandbox, chọn sidebar trên hoặc deep link; API cần utility flag/profile U22-D03 bật, nếu chưa bật trang báo rõ. Backend giữ default-off. Rollback scoped revert toànPR; tắt backend gate để ngừng dữ liệu demo, không DB rollback. Không production/paid provider/real PII. Real source/unit/day/epoch/IAM adapter, Group business integration và official BCA vẫn PENDING_INPUT theo mock trước; không công bố toànfeature/SRS/production hoàn tất.
