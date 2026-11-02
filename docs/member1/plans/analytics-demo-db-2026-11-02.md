# Occupancy/Group DB demo — kế hoạch thực thi đã được user duyệt

CONFIRMED: base develop321ad5a4c69ff7312c78cc634cfdf9088c9f3a3d, nhánh codex/m1-analytics-db-20261102, worktree sạch trước sửa. Đã checkout/pull develop, merge vào nhánh trước và tạo nhánh mới. User chọn schema demo riêng và DB→API→UI, ủy quyền schema local/cross-member/Git delivery; không giả peer approval. Skills áp dụng: grounded executor, data processing, DB schema design, quality/Git delivery.

## Contract và acceptance

- Giữ GET demo occupancy/Group, scenario selectors, envelopes, CHAIN_ADMIN, fixed replay dates, dual profile/flags và prod exclusion. Dữ liệu SYNTHETIC/businessData=false/previewOnly, không map synthetic ID thành phòng thật. Occupancy840rows/2hotel, Group7cases từ main resources frozen/hash-pinned, không đưa expected oracle vào DB. Giữ engines/gold/thresholds.
- Schema m1_demo có6domain tables và Liquibase registry riêng, changelog ngoài root includeAll. Constraints/keys theo dataset/scenario; không FK/Core writes. Seed explicit local/transactional; same version no-op, drift abort, rollback scoped/digest guarded. Backup trước sửa schema hiện hữu. Hai clocks khác nhau giữ đúng fixture.
- Provider fixture/db, default fixture, DB pool riêng lazy/read-only/repeatable-read/5s và role mới không quyền public/Core. Dùng M1_DEMO_DB_URL/USER/PASSWORD riêng; credentials trong ignored runtime file, không Git/log. DB absent/invalid/staged/digest drift→503fixed/no fallback; auth kiểm trước/sau compute.

## Slices và allowlist

1. DB-01: db/demo XML+SQL mới; Seed-M1AnalyticsDemo.ps1, analytics_demo_db.py và test mới; local provisioning/backup/receipt, drift/no-op/rollback/atomicity trên DB test riêng.
2. OCC-DB vàGROUP-DB: mới ai/demo/data provider/config; sửa5service/config hiện hữu và occupancy controller thêm503; tests mới trong ai/demo/data. Không sửa nguồn Core/security hoặc frozenassets/oldtests.
3. E2E-DB: acceptance launcher và verifier truyền opt-in mode/credential riêng, CI PG integration job; API/browser test hiện hữu trên sandbox DB mode, lint/build/source audits. Frontend business/UI chỉ sửa nếu có reproducer; hiện chưa cần.
4. DELIVERY: self-review/fix/retest, scoped commit/push/PR và exact-headCI; merge khi GitHub policy cho phép, postmerge evidence. Không sửa protection/fakeapproval/reset/stash/overwrite worktrees khác.

## Verification và rollback

48existing demo tests giữ nguyên;6new provider unit và5PostgreSQL IT ban đầu. IT dùng DB m1_analytics_test riêng, không skip khi được chọn. Parity cả9scenarios, auth revoke, role cannot write/readCore, staged/tampered/unavailable sanitized. Provisioner kiểm hashes/counts/constraints/no-op/rollback; runtime Keycloak/API/UI trên acceptance sandbox, main public/Utility digest giữ nguyên. Bằng chứng FAIL/PASS lưu riêng; không cộng các lượt thành coverage mới.

Rollback app bằng data-source=fixture; dataset delete chỉ qua runner sau digest/payload checks. Không DROP schema chứa dữ liệu khác, không reset sequences/Core. DB test và sandbox có snapshot/ownership guards. Kết quả là DB-backed synthetic demo; V03 real-data và official BCA vẫn chưa được nghiệm thu. Report ghi mọi file shared/member khác và lý do, lệnh thật/NOT RUN, Git trạng thái.
