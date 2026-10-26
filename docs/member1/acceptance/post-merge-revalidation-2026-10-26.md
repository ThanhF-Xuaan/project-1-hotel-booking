# Đối soát develop sau PR #33

- **CONFIRMED baseline:** `develop/origin/develop@96d7815b4e5d13f5052ca0fa6a6c414c8297ea1e`, đã fast-forward repo chính; nhánh thực hiện `codex/m1-post-merge-revalidation`.
- **CONFIRMED thay đổi upstream:** ngoài runner `368f6e4`, develop có phòng đoàn/block inventory, master folio, BI dashboard và hai migration thêm bảng group inquiries/master folio adjustments. Bằng chứng PASS tại e027e90 không đại diện các thay đổi mới này.
- **Ủy quyền:** người dùng đã merge PR và yêu cầu tiếp tục công việc hợp lý; sandbox local riêng, dữ liệu giả lập và migration vào sandbox này đã được cho phép trong chuỗi nghiệm thu. Không truy cập DB dev/shared/production.
- **Bảo vệ:** 9 file untracked hiện có, gồm đặc tả U14, được hash trước đồng bộ và giữ nguyên. Không reset/clean/stash.

## Trình tự và allowlist

1. Khởi động Docker Desktop nếu engine đang tắt. Chỉ dựng Compose project `hotel-m1-acceptance` bằng manifest runtime đã xác minh, tạo snapshot riêng **trước migration mới**, rồi dừng hạ tầng, giữ volume.
2. Đối soát hai source file được audit T12 nhưng đã thay đổi qua các PR upstream: ErrorCode.java chỉ thêm lỗi Group/folio; RoomAvailabilityServiceImpl.java trừ group-block trong available count. Review source/provider/DTO và giữ lịch sử hash.
3. Allowlist hồ sơ: `docs/t12/r1-00/v2.0.0-draft.1/baseline-source-evidence.json`; `docs/t12/re-audits/2026-10-26-core-integration-v1.json` và `.md`; verifier mới `tools/t12/verify_core_reaudit.py`; báo cáo này, README nghiệm thu và report timestamp do runner tạo trong `docs/member1/acceptance/runs/`.
   Tooling đi kèm: `tools/t12/test_core_reaudit.py` kiểm tra chống đổi source/metadata bằng dữ liệu in-memory; `tools/acceptance/Verify.ps1` gọi thêm verifier Core trong cùng quy trình. Đây là kiểm tra provenance bổ sung, không thay validator/gold/threshold.
   Regression xác nhận portability test cũ dùng ErrorCode làm ví dụ "không có canonical LF"; sau đối soát file này có LF/CRLF được ghi rõ nên giả định đó không còn đúng. Cho phép cập nhật `tools/t12/test_portability_validation.py` chọn ApiResponse (vẫn chưa có canonical LF) cho phép thử strictness cũ, thêm kiểm tra hai Core file đã đối soát; không bỏ assertion strictness.
4. Chạy source re-audit, validator/portability rồi `Verify.ps1` thật trên baseline mới; ghi kết quả/skip đúng nguồn. Snapshot của Verify là sau migration/seed và được restore sau test; snapshot trước migration giữ riêng để phục hồi vào DB sandbox rỗng nếu cần schema cũ.
5. Nếu tái hiện bug làm hỏng tích hợp, khóa thêm file allowlist và regression theo lỗi đó trước khi sửa. Không thay gold/validator/threshold hoặc sửa nghiệp vụ ngoài lỗi đã xác nhận để lấy kết quả xanh.

**Slice kiểm chứng bổ sung:** sau lượt baseline đầu, thêm `frontend/e2e/api/t12-core-integration.spec.ts` dùng ứng dụng/DB thật: seed group-block giả lập bằng harness, xác nhận Core giảm tồn, T12 refresh loại card hết phòng rồi nhận lại sau release; so digest DB quanh từng advisor request để bảo vệ no-write. Harness chỉ dọn dữ liệu có marker của riêng nó, toàn DB có snapshot. `tools/acceptance/Test.ps1` bỏ số portability hardcode 11 vì suite hiện có 12 ca; không đổi verdict/exit rule. Chạy test mục tiêu trước, sau đó full Verify để bằng chứng cuối khớp source cuối.

## Các điều kiện đã kiểm tra qua source

- Hai migration mới thêm bảng/index/constraint, không có drop/update dữ liệu cũ. Chỉ chạy sau snapshot ở đích đã xác minh.
- GroupCutoffScheduler mới chịu `hotel.scheduling.enabled`; acceptance launcher đặt false nên scheduler này phải tắt.
- ErrorCode/RoomAvailability source drift được phát hiện từ manifest audit, không phải lý do cho phép bỏ check hash. RAG/model, U14 consent thật, mẫu export T05 chính thức giữ các giới hạn cũ.
- Rollback tooling/hồ sơ qua revert từng commit; DB sandbox có snapshot trước migration và snapshot sau migration. Snapshot trước migration không có ba bảng mới; phải phục hồi vào DB rỗng riêng, không dùng dump cũ để giả định xóa các bảng mới trong DB hiện hữu. Việc phục hồi schema cũ chưa được chạy. Không reset repository hoặc xóa volume.

## Kết quả

**CONFIRMED — PASS local**, [lượt cuối](runs/acceptance-20261026-111047-405-831989ee.md), exit 0. Base `96d7815b4e5d13f5052ca0fa6a6c414c8297ea1e` cộng diff đối soát/tooling/regression trong nhánh. Mã nghiệp vụ upstream giữ nguyên; không sửa Java/schema/DTO/API/Frontend source để đạt kết quả.

| Kiểm tra đã thực thi | Kết quả |
|---|---|
| Preflight Git | PASS: repo chính fast-forward về develop mới, không có path collision với 9 file untracked đã bảo vệ. |
| Backup trước migration | PASS: đúng hotel_acceptance_db trên 15432, guard tên hotel giả lập; snapshot 306155 bytes, SHA-256 `64e549cfe47e79cdc80ea0eb0ed64fe9d37ff6f56e593924eb401e08d038ca3f`. |
| Liquibase mới | PASS: lượt đầu Run=2, Previously run=28, Context mismatch=1; thêm group_inquiries, group_inquiry_room_types và master_folio_item_adjustments. Lượt cuối không còn migration cần chạy. |
| Full Maven | PASS: `.\mvnw.cmd clean test`, 999 test, 0 failure/error, 1 skip HotelBookingApplicationTests.contextLoads có annotation Disabled hiện hữu. |
| PostgreSQL/Redis | PASS thực chạy: LodgingExportPostgresTest 3/3, RedisRoomAdvisorSessionStoreRealRedisTest 4/4, không skip hai class này. |
| T12 source/artifact | PASS: lazy-route audit 39 routes/22 lazy components/4 aliases; Core audit 2 source/7 dependency/14 error constants thêm mới, tuple lỗi cũ giữ nguyên. Artifact validator 184 instance/74 case, 23 protected hashes/34 source hashes. |
| Integrity tests | PASS: Core provenance 10/10, portability 12/12; mutation chỉ ở memory, không sửa source/gold. |
| Lint/build/audit | PASS: 0 lint errors/warnings, Vite entry 246.80 kB, không warning >500 kB; npm audit 0 vulnerability tại lần chạy. |
| API/browser | PASS: API 17/17, browser 6/6 với Keycloak/backend/frontend thật và dữ liệu giả lập. |
| T12 group-block targeted | PASS 1/1 trước full run. Full run cũng PASS khi yêu cầu RESULTS, vẫn có phòng khác, loại/nhận lại room type 1 theo refresh. Không chấp nhận TEMP_UNAVAILABLE làm bằng chứng hết tồn. |
| Restore/teardown | PASS: snapshot sau migration restore thành công, 2 hotel/5 room type/310 availability/5 staff. Không còn app manifest/listener acceptance hoặc container acceptance, 3 volume giữ lại. |

Tổng availability từ 305 của ngày trước thành 310 khi seed ngày mới là kết quả fixture sinh thêm ngày cho 5 room type; không dùng số 305 cũ làm expected cố định. Snapshot trước migration giữ tại runtime `pre-core-merge-20261026-f46f17f0/snapshot.sql`; snapshot trước test nhóm nằm trong runtime `targeted-core-*.sql`; snapshot cuối trong thư mục của run cuối. Không đưa SQL/runtime chứa cấu hình vào Git.

## Các sai lệch đã xử lý và lịch sử

- Validator trước đối soát FAIL vì ErrorCode hash; RoomAvailability cũng có drift khi đối chiếu manifest. `post-merge-contract-before.log` giữ lỗi đầu; chỉ hai entry được cập nhật cùng snapshot evidence cũ, canonical hashes và patch/source commit provenance. Validator và approval register không đổi.
- Portability test ban đầu FAIL tại `test_unaffected_file_lf_fails_when_not_designated` vì nó dùng ErrorCode làm ví dụ không có canonical LF. Sau re-audit, ErrorCode có canonical LF/CRLF; test chọn ApiResponse chưa bị thay đổi, giữ assertion LF phải bị từ chối và thêm check tampering cho hai Core file mới. Không đổi kết quả mong đợi của strictness hoặc gold.
- [Lượt baseline đầu](runs/acceptance-20261026-110210-569-0c86d750.md) đã PASS Maven/API 16/browser 6 trước regression nhóm mới. Field mô tả portability còn hardcode 11 dù unittest thực chạy 12; Test.ps1 đã bỏ số cứng, lượt cuối ghi thông tin dựa vào output thật.
- Regression group-block seed một contract/block/daily rows có UUID marker riêng. Digest được lấy sau từng setup change và quanh mỗi advisor HTTP request; setup/cleanup SQL không nằm trong advisor path. Cleanup xóa contract/company riêng có marker, FK cascade dọn block/daily; toàn DB được restore từ snapshot sau suite.
- Verifier Core ghim đúng accepted Git source, scope dependency và flag so sánh API cũ; thay source cùng hash, thay commit, sửa metadata cũ, bỏ dependency/API comparison hoặc sửa provider đều bị test từ chối.

## Giới hạn và bàn giao

Đây là regression tích hợp local của M1 trên baseline mới và kiểm chứng consumer T12 với group-block. Không thay thế nghiệm thu toàn bộ nghiệp vụ phòng đoàn, master folio hoặc BI. Kiểm tra digest xác nhận effect DB của các request được chạy; không phải bằng chứng cho mọi request có thể xảy ra hoặc một DB role read-only.

T05 mẫu export chính thức, U14 binding/consent/history thật, gold/model quality và staging vẫn giữ giới hạn trước. Đặc tả U14 untracked được giữ nguyên, chưa được biến thành API hoặc migration. Không phát sinh API business write trong module advisor.

Các file PowerShell cuối khớp script hashes của run cuối. Gold/fixtures/API production/shared migrations không nằm trong diff bàn giao. Commit/push chỉ chứa hồ sơ, tooling và test mới theo allowlist; 9 file untracked cũ giữ ngoài commit. Người dùng tự tạo PR vào develop như quy trình trước; rollback diff riêng bằng revert, không reset/clean/stash.

Kiểm tra provenance cuối chuyển nhãn PR 24 cũ của ErrorCode sang previousReconciledFromPr để tránh gán sai PR cho hash mới. Sau chỉnh metadata này đã chạy lại Core verifier/10 integrity tests và portability/artifact checks; không chạy lại Maven/API vì mã nghiệp vụ, test E2E và script điều phối không đổi. Full report giữ nguyên thời điểm thực thi; không viết lại hash/log của lần chạy cũ.

Lệnh tái hiện trên sandbox đã chuẩn bị và dừng:

```powershell
pwsh -NoProfile -File tools/acceptance/Verify.ps1
```

Đề xuất tiêu đề PR: `chore: reconcile Core evidence and verify post-merge acceptance`.
