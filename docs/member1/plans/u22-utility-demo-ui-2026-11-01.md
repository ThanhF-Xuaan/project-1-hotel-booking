# U22-D04 — UI demo điện/nước

CONFIRMED base develop `5af00a7ab8652a58a488dc9a1bda59f2d5e1a2ad`, branch `codex/m1-u22-utility-demo-ui-20261101`, worktree sạch. User ủy quyền implementation/frontend/review/fix/Git delivery, mock trước/dữ liệu thật sau; không giả human peer approval.

Nguồn: contract/API U22-D03 đã merge PR47, UtilityDemoService và UtilityAnomalyResponse. Frontend React19/TS6/Zod4/apiClient, ProtectedRoute CHAIN_ADMIN, occupancy demo là mẫu hiện có. API default-off và độc lập occupancy; Java engine/threshold/fixtures/gold không đổi.

PROPOSED route lazy `/ai/demo/utility-anomalies` dưới CHAIN_ADMIN, sidebar link admin, trang riêng responsive như occupancy. Chọn12aliases cố định; service chỉ GET API hiện có, validate code/provenance/scenario/units/fixture scope/clock/42dates/point states/counts trước render. Không recompute detector hoặc classification, không fallback data. Abort/request-key khi đổi/tải lại/unmount; safe401/403/404/503/network/malformed, retry. Static scenario labels chỉ để chọn khi API chưa bật, không là dữ liệu thay thế.

UI luôn ghi SYNTHETIC/không đồng hồ thật/không kết luận gian lận, fixed31/10/2026 lúc00:00 Việt Nam. Hiện42ngày 19/09–30/10, unit kWh/m³, counts evaluated/needsReview/insufficient/excluded, usage line và median/threshold từ server, anomaly marker có shape/text và bảng đầy đủ. Missing/excluded tạo gap; warmup không normal. Table giữ decimal tới8chữ số để không làm ngưỡng0.001 thành0; không giả accuracy/calibration thật. Chart scroll/focus và table alternative, mobile375/desktop1440/reduced motion.

Allowlist: mới analytics `services/utilityDemoService.ts`, `components/UtilityAnomalyChart.tsx`, `pages/UtilityDemoPage.tsx`; chỉ import/route App và link Sidebar. Mới browser `u22-utility-ui.spec.ts`, `u22-utility-runtime.spec.ts`. Mới exact-route helper `tools/member1/reconcile_utility_route.cjs` và test; compose helper hiện có `reconcile_u22_route.cjs` qua helper mới rồi giữ mọi old assertions/tests. Không sửa historical evidence/gold hoặc5T12wrappers. CI thêm mock UI spec vào command có sẵn, không giảm backend minimum1443. Plan/report/README/run report.

Implement → lint/build/targeted browser/tamper → adversarial review/fix → full Verify both-on trên volume synthetic riêng có snapshot → utility-off/occupancy-on runtime browser/API riêng → config/snapshot restore, services stop → diff/allowlist/privacy review → commit/push/PR → exact-head CI PASS → merge → post-merge CI/handoff append-only. Giữ FAIL/PASS, không cộng subset/skip thành PASS. Real Keycloak/sidebar/admin/nonadmin/12scenarios/UI values vs API và data-sequence digest là runtime proof riêng với mock tests. No paid provider/production.

Rollback scoped revert toànPR hoặc server gate off; không DB rollback. UI demo completion khác real-data integration; nguồn unit/day/epoch/read adapter/IAM nghiệp vụ/officialBCA vẫn PENDING_INPUT. Không sửa T04/schema/shared auth hoặc tự đóng toànfeature/production.
