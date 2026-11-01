# U22-D03 — API demo bất thường điện/nước

**PASS local/synthetic, cả hai gate độc lập.** Scope là API demo local/synthetic, UI là subtask kế tiếp. User ủy quyền tự quyết implementation/review/fix/commit/push/PR/merge, mock trước/dữ liệu thật sau; không giả member sign-off hoặc human peer approval. [Contract/allowlist](../contracts/u22-utility-demo-api-v1.md).

Base develop `6d24615d587d636a6697f9ada2fd9aa5766db4e3`, branch `codex/m1-u22-utility-demo-api-20261101`, worktree ban đầu sạch. Không sửa T04, engine/ngưỡng median-MAD, frozen fixture/labels/gold, schema, IAM chung, frontend UI hoặc dependencies. Không reset/stash/overwrite repo D hoặc worktree khác.

## Hành vi

`GET /api/v1/ai/demo/utility-anomalies?scenario=ELECTRIC_SPIKES`, mặc định ELECTRIC_SPIKES. Có 12 kịch bản: ELECTRIC_FLAT, WATER_FLAT, ELECTRIC_SPIKES, WATER_SPIKES, ZERO_BOUNDARY, MAD_NOISE, GAP, RESET, REPLACEMENT, NEGATIVE_DROP, LOW_COVERAGE, SLOW_VARIATION. Đây là aliases của dữ liệu giả lập, không phải đồng hồ/khách sạn nghiệp vụ. API chỉ nhận một scenario, không nhận business scope/role/unit/clock/URL; unknown/empty/lowercase/duplicate/extra selectors400, message cố định không echo input.

Dual gate profile `m1-demo` và `hotel.ai.demo.utility.enabled=true`, default-off; prod/production loại controller/provider. Gate độc lập với occupancy. Launcher chủ động đặt cờ false nếu cấu hình không bật, không kế thừa cờ utility từ host, không lặp profile nếu cùng bật occupancy. Dùng SecurityConfig/Keycloak hiện có; CHAIN_ADMIN, anonymous401 và nonadmin403 khi bật; authenticated404 khi tắt. Service đọc trusted SecurityContext trước/sau replay và qua authorization port của engine; không cache quyền admin hoặc tin selector client. Đây là kiểm quyền theo IAM hiện có, không tuyên bố đã thử remote Keycloak role revocation giữa request.

Response ApiResponse/code200/result có SYNTHETIC/businessData=false, notice tiếng Việt, scenario/catalog label+unit, referenceInstant, analysis DTO và counts. Mốc cố định `2026-10-30T17:00:00Z`, phân tích 19/09–30/10/2026, không phải số liệu hôm nay. KWH/M3 và epoch/marker chỉ có nghĩa cho synthetic corpus; không suy unit thật từ meter type. Counts phân biệt valid/evaluated/needsReview/excluded/insufficient; ANALYZED không nghĩa mọi ngày đã được score.

Resource main riêng `ai/demo/utility-readings-v1.csv` là projection8cột/516rows của frozen source, loại expected; SHA256 `f851a52a1fa293c5dfb4860edb0becf55b0b1479f5eec9a9d83c1ff6e11e8dca`,24965bytes. Pin hash, bound128000bytes, header/date/scenario/unit/count validation; attributes -text giữ bytes Windows/Linux. Chỉ đọc fixed classpath resource, không runtime test path hoặc arbitrary file/URL. Asset lỗi dừng khởi tạo demo; analysis dependency lỗi503/message an toàn, không fallback.

Engine cũ tính daily delta với predecessor/epoch hợp lệ; >=28 prior mới score. Missing/reset/replacement/drop giữ EXCLUDED/INSUFFICIENT, không thành zero hoặc normal. Chỉ NEEDS_REVIEW có nhãn “Cần kiểm tra”; không kết luận gian lận hoặc thay đổi giá/vận hành. Adapter không có SQL/network/Core repositories hoặc business write path.

## Review và bằng chứng

- Targeted đầu78test có1failure: test API mới giả định point đầu mọi scenario là warmup; LOW_COVERAGE thực tế raw trống nên phải EXCLUDED. Sửa assertion đúng riêng scenario, không sửa engine/fixture/threshold hoặc broad-match bỏ kiểm tra. Review cũng đưa occupancy flag vào test context trước refresh để thực sự kiểm utility flag độc lập.
- Targeted cuối `UtilityDemoApiTest,UtilityAnomaly*Test`: **78/78 PASS**, gồm33test mới +45engine cũ,0failure/error/skip. 78 là subset của full backend, không cộng lặp. Bao gồm filters+method/direct-service auth,5profile exclusions, false/missing gate,12scenario/deterministic output, fixed clock, oracle-free resource parity/hash/bound, closed imports, units/reset/gap/low coverage/counts. Test dùng Basic chỉ là narrow servlet evidence, không thay Keycloak proof.
- [Full Verify](runs/acceptance-20261101-151045-388-0955c553.md) **PASS:1451 backend /34 API /47 browser**,0failure/error/skip trong executions. Backend clean test và context smoke là hai lệnh riêng, không gọi là một lượt Maven. Năm utility API tests mới nằm trong34,33unit mới/78targeted nằm trong1451, không cộng lặp. Real Keycloak admin đọc tất cả12kịch bản, repeat deterministic, customer/reception403, anonymous401, invalid400; digest data/sequence main DB trước/sau bằng nhau.

| Kiểm tra | Kết quả |
|---|---|
| Full Verify both-on | PASS run20261101-151045-388-0955c553; cả occupancy API/UI và utility hoạt động |
| Occupancy-only / utility-only, Keycloak thật | PASS hai mode riêng, mỗi mode4occupancy API+5utility API+2browser=11checks,22 executions không cộng vào full run. API tắt404, API bật200, quyền và digest vẫn được kiểm |
| Read-only / cleanup | PASS actual HTTP data/sequence digest, cả hai run snapshot restore; original config khôi phục nguyên byte, owned services/apps/listeners/manifest đã dừng |
| Source/contract/lint/build/audit | PASS full Verify và targeted ESLint/actionlint/PowerShell parse.0ESLint error/warning; còn12moderate tooling advisories baseline,0high/critical |
| CI | Minimum1410→1443 theo33test mới; require-suite UtilityDemoApiTest. Kết quả exact-head/post-merge ghi ở PR/receipt/handoff, không gọi1451local là CI |

Logs FAIL/PASS, source hashes, config/canonical backups, independent-flag harness và receipt tại `C:/Users/admin/.codex/outputs/u22-utility-demo-api-2026-11-01/`. Runtime credentials/snapshots không vào Git. Git status/delivery cuối ở PR/receipt/canonical handoff; không suy từ local PASS.

Child application-suites giữ PARTIAL/exit1 vì Maven chạy riêng; supervisor yêu cầu sáu suite thực thi PASS cùng backend.json/snapshot restore trước verdict PASS. Lượt targeted FAIL đầu được giữ nguyên; không nới test/gold/threshold. Final self-review không còn finding chặn trong scope demo API. Các file:3demo Java classes, CSV/attributes, UtilityDemoApiTest, API E2E mới,2launcher scripts, CI minimum/require-suite, contract/report/README và run report. Code/fixture/source hashes giữ nguyên từ full Verify tới delivery; không giả unit/mock servlet là chứng cứ Keycloak/runtime thực tế.

Xem demo: thêm `HOTEL_AI_DEMO_UTILITY_ENABLED="true"` vào config synthetic acceptance rồi dùng launcher hiện có; truy cập GET bằng bearer CHAIN_ADMIN. Default-off giữ nguyên nếu không opt-in. Tắt flag/profile hoặc revert scoped PR để rollback, không DB/schema rollback. Không production hoặc paid provider call. UI utility là bước sau; nguồn điện/nước thật/unit/day/reset/epoch/read adapter/IAM nghiệp vụ và official BCA vẫn PENDING_INPUT. Không công bố toàn U22/SRS/production hoàn tất.
