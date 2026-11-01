# U22-D03 — API demo bất thường điện/nước

CONFIRMED: base develop `6d24615d587d636a6697f9ada2fd9aa5766db4e3`, branch `codex/m1-u22-utility-demo-api-20261101`, worktree sạch. User cho phép triển khai/review/fix/Git delivery và mock trước, dữ liệu thật sau. Owner Member 1 theo ủy quyền user; không giả member sign-off hoặc human peer review.

## Nguồn và contract

CONFIRMED engine `UtilityAnomalyDetectionService`/typed response hiện có, local contract `u22-utility-anomaly-local-v1.md` và frozen CSV SHA256 `48078f6afa5d34ac6c3bf0292813486d33bf10cdc15397085ddd8223b5bb9638`. Engine chỉ dùng normalized SYNTHETIC cumulative daily evidence, tối đa 42 ngày + anchor, >=28 prior valid points, causal median/MAD/strict threshold; không thay constants, labels, fixture hoặc gold. T04 mapped usage chưa chứng minh unit/day/epoch nên không nối live business provider.

PROPOSED theo ủy quyền: `GET /api/v1/ai/demo/utility-anomalies?scenario=ELECTRIC_SPIKES`, default ELECTRIC_SPIKES. Chỉ nhận tối đa một scenario, không nhận hotelId/meterId/regionId/unit/role/clock hoặc URL từ client. Unknown, empty, lowercase, duplicate hoặc extra query trả400 với message cố định, không echo input. Danh sách12scenario cố định từ seriesId uppercase của corpus, không là IDs đồng hồ nghiệp vụ.

Response ApiResponse hiện có/code200/result gồm dataMode=SYNTHETIC, businessData=false, scenario, notice tiếng Việt, referenceInstant, danh sách scenario(id/label/unit), analysis DTO hiện có và counts(valid/evaluated/needsReview/excluded/insufficient). Counts tính từ kết quả engine, không từ oracle. ANALYZED chỉ nghĩa có ít nhất một ngày được score; INSUFFICIENT_COVERAGE không thành NORMAL. Mốc `2026-10-30T17:00:00Z`; analysis19/09–30/10/2026, không phải hôm nay. Chỉ cảnh báo cần xem xét, không kết luận gian lận hoặc ghi nghiệp vụ.

Phải bật đồng thời profile `m1-demo` và `hotel.ai.demo.utility.enabled=true`; prod/production luôn loại controller/provider. Default-off và độc lập với occupancy flag. Auth dùng SecurityConfig/Keycloak hiện có; CHAIN_ADMIN, anonymous401/nonadmin403 khi bật, authenticated404 khi tắt. Check trusted SecurityContext trước/sau replay và authorization port trước/sau read; không auth từ selector. Dependency/domain failure503 sanitized, invalid scenario400; không tạo fallback dữ liệu.

Resource runtime mới `ai/demo/utility-readings-v1.csv` là projection8cột của frozen CSV, bỏ cột expected; giữ nguyên516rows gồm missing raw, unit, epoch, marker, negative/reset cases. Pin raw SHA/bound128000bytes; header/series/date/metadata/count validation khi load. Resource không đọc từ test path; .gitattributes -text giữ hash Windows/Linux. Fixture hỏng không khởi tạo demo. Không DB/network/Core repository hoặc business writes.

## Acceptance / allowlist / delivery

- Mới3Java files `modules/ai/demo/utility/{UtilityDemoConfiguration,UtilityDemoController,UtilityDemoService}.java`, resourceCSV và attributes; mới `UtilityDemoApiTest` cùng package, thêm API E2E `frontend/e2e/api/u22-utility-demo.spec.ts`.
- Chỉ thêm utility opt-in/default-false/export env trong `tools/acceptance/{Run-Application,Test}.ps1`; profile không bị lặp nếu cùng bật occupancy. CI minimum chỉ tăng đúng test mới đã chạy, không giảm old count. Docs contract/report/README/run report. Không sửa UI, App/provenance helpers, engine/T04/schema/IAM/gold/dependencies.
- Kiểm hai gate, prod exclusion, missing flag, CHAIN_ADMIN/filter+method/direct-service auth, exact selectors, tất cả12cases/unit/status/42points/determinism/summary, reset/gap/low coverage/decimal boundary và oracle-free resource parity/hash/bounds/no-write architecture. Existing45engine tests giữ nguyên.
- Targeted → review/fix → full Verify bật utility cùng occupancy trên sandbox synthetic mới có snapshot/restore; real Keycloak GET all12/data-sequence digest. Mode tắt và independent occupancy-on/utility-off kiểm riêng; restore original config nguyên byte, owned services stop. Commands FAIL/PASS giữ riêng, không cộng subset/skip thành PASS.
- Final review → commit/push/PR → exact-head CI PASS → merge develop → post-merge CI/handoff append-only theo user đã ủy quyền. Rollback bằng tắt flag/profile hoặc scoped revert toànPR, không DB rollback. UI utility là subtask tiếp theo; real data/business IAM/read adapter/BCA vẫn PENDING_INPUT, không kết luận toàn feature hoặc production.

CONFIRMED targeted: 33 test API mới +45 engine tests =78 PASS; CI minimum1410→1443 và require-suite UtilityDemoApiTest. Resource projection SHA256 `f851a52a1fa293c5dfb4860edb0becf55b0b1479f5eec9a9d83c1ff6e11e8dca`,24965bytes/516rows, parity test xác nhận đúng8cột và không có oracle. Review đã sửa assertion test mới: LOW_COVERAGE thiếu raw ngày đầu nên EXCLUDED, không phải warmup; fixture/engine giữ nguyên. Runtime sẽ kiểm both-on bằng full Verify, sau đó occupancy-only và utility-only riêng để chứng minh gate độc lập.
