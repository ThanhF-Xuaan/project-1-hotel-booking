# GROUP-D01 — API Group preview demo

CONFIRMED base develop `a70b833f8d138621e539fdbcade595eb56165a4c`, branch `codex/m1-group-demo-api-20261101`, worktree sạch. User ủy quyền implementation/review/fix/commit/push/PR/merge và mock trước/dữ liệu thật sau; không giả member sign-off hoặc human peer approval.

Nguồn hiện có:9pure Java classes dưới ai/groupassignment, contract `group-room-assignment-preview-local-v1.md`,7frozen C01 cases SHA256 `2dbd7da729831b0f21cc53c21412bb744fd8e620aaa050c6fe9a2b2a89ddc476`. Engine exact bounded matching, quota mọi occupied night theo block/type, bed waste/floor/tie objectives, previewOnly=true. Không Core provider/IAM scope/physical bed/floor data thật đã xác minh. Không sửa engine/constants/oldtest/fixture/gold hoặc gọi GroupRoomBlock write service.

PROPOSED theo user delegation: `GET /api/v1/ai/demo/group-room-assignment?scenario=FIT_AND_TIE`, default FIT_AND_TIE. Chỉ single optional scenario; reject unknown/empty/lowercase/duplicate/extra params400/message cố định. Không nhận hotelId/contractId/dates/demands/rooms/quota/scope/role/clock/workLimit/action; không POST/apply/upload/hold/assign/release/booking/guest writes.

| Alias | Frozen case | Nhãn demo |
|---|---|---|
| FIT_AND_TIE | P01 | Đủ sức chứa, ưu tiên giường và tầng |
| QUOTA_ONE | P11_QUOTA_ONE | Quota chỉ đủ một phòng |
| MIDDLE_NIGHT_LIMIT | P12_MIDDLE_BOTTLENECK | Giới hạn ở đêm giữa |
| ZERO_QUOTA | P14_ZERO | Quota bằng 0 |
| QUOTA_CONTROL | P14_CONTROL | Quota đủ cho hai yêu cầu |
| NO_TRANSFER_BLOCK | P14_NO_TRANSFER_roomBlockId | Không chuyển quota giữa block |
| NO_TRANSFER_ROOM_TYPE | P14_NO_TRANSFER_hotelRoomTypeId | Không chuyển quota giữa loại phòng |

Response ApiResponse/code200/result gồm dataMode=SYNTHETIC, businessData=false, previewOnly=true, notice tiếng Việt, scenario/catalog, referenceInstant, request/snapshot synthetic từ case và preview typed hiện có, allDemandsAssigned. Mọi IDs/demand keys trong gói chỉ là fixture IDs/opaque keys, không khách thật. COMPLETE nghĩa evaluation complete, vẫn có thể assignments rỗng hoặc unmet; allDemandsAssigned chỉ true khi unmet rỗng. Không suy quota bằng rooms count hoặc sửa all-night availability để lấp unmet. Reference `2026-10-31T02:00:00Z` và TTL chỉ đúng ở replay clock, không live availability/reservation.

Dual gate `m1-demo` + `hotel.ai.demo.group.enabled=true`, default-off, prod/production exclude cả controller/provider; độc lập occupancy/utility flags. Existing Keycloak/filter/method CHAIN_ADMIN; anonymous401/nonadmin403 khi bật, authenticated404 khi tắt. Direct service/authorization port/data read/after computation kiểm trusted role/scope, không cache role hoặc tin selectors. TEMP_UNAVAILABLE/domain mismatch503/fixed message, không fallback/partial preview hoặc raw error. Asset hỏng fail initialization.

Main resource `ai/demo/group-preview-v1.json` là projection từ frozen source: chỉ metadata/data cases, bỏ expected/source/C01-oracle metadata. Pin SHA, bound128000bytes,7case IDs/version/clock validation. Immutable cache typed request/snapshot, mapper JavaTime đã có; fixed local ports vào engine. Không runtime test resource/arbitrary file/URL/SQL/network/Core dependency.

Allowlist: mới3classes `ai/demo/group/{GroupDemoConfiguration,GroupDemoController,GroupDemoService}.java`, main JSON/.gitattributes, `GroupDemoApiTest` package tương ứng; API E2E `frontend/e2e/api/group-demo.spec.ts`; chỉ thêm group flag/default false/profile dedup trong tools/acceptance/{Run-Application,Test}.ps1; CI minimum chỉ tăng đúng new test executions và requireSuite. Docs contract/report/README/run report. Không UI, App/provenance helpers, shared security/schema/dependencies hoặc pure engine/gold changes.

Targeted gates/RBAC/default selectors/all7/determinism/parity/no-oracle/corrupt bound/readonly imports; independent known quota zero/middle/no-transfer/bed-floor oracles và old group tests giữ nguyên. Review→fix→full Verify all3demos-on trên fresh synthetic volume có snapshot, real Keycloak HTTP/all7/data-sequence digest; group-off/other-demos-on riêng; restore snapshot/config exact bytes và stop owned services. Giữ FAIL/PASS/subsets, không gọi mock servlet là Keycloak proof. Final diff/review→commit/push/PR→exact-head CI PASS→merge→postmerge CI→canonical append-only. Rollback flag off hoặc scoped revert, không DB rollback. GROUP-D02 UI là subtask sau; real providers/physical bed/floor/quota IAM và official BCA vẫn PENDING_INPUT, không fullfeature/production claim.

CONFIRMED targeted29new+56existing group tests=85/85PASS,0skip. CI minimum1443→1472/requireSuite GroupDemoApiTest. Projection25487bytes/7cases, SHA256 `8d569695a769c55b9bc94ec465d47063555a87917eb47b415a1341405d1994cf`; parity test xác nhận chỉ loại oracle metadata và giữ request/snapshot như frozen source. HTTP405 narrow servlet test có valid auth+CSRF chỉ chứng minh controller không khai báo write mappings, không thay realJWT/DB evidence.
