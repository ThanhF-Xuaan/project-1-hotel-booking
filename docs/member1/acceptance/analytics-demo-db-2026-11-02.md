# Occupancy/Group — dữ liệu tổng hợp trong PostgreSQL

CONFIRMED base321ad5a, branch codex/m1-analytics-db-20261102. User duyệt implementation/schema local/Git/cross-member từ kế hoạch trong chat. Mục tiêu DB→API→UI với schema riêng; đây là synthetic integration, không nghiệm thu dữ liệu vận hành thật. [Contract](../contracts/analytics-demo-db-v1.md), [kế hoạch](../plans/analytics-demo-db-2026-11-02.md).

## Cách dùng DB local

Trong DBeaver trên hotel_booking_db, refresh Schemas và mở m1_demo. Sáu bảng domain lưu840occupancyrows và7Groupscenarios cùngcandidates/quota/nighteligibility. Các ID nằm trong demo namespace, không đại diện23phòng thật. Query `SELECT * FROM m1_demo.datasets;` để xem SYNTHETIC/version/READY; có thể xem occupancy_samples/group_scenarios trực tiếp.

Provision từ project root, chọn evidence directory mới ngoài Git:

```powershell
./tools/acceptance/Seed-M1AnalyticsDemo.ps1 -Action seed -Container hotel_postgres -Database hotel_booking_db -EvidenceDirectory $env:TEMP/m1-analytics-seed-new
```

Không dùng lại evidence directory; runner không ghi đè backup/receipt. Cùng dataset/version không đổi là no-op, drift FAIL và giữ dữ liệu. `-Action inspect` kiểm toàn bộ payload/hash/reference; `-Action rollback -Dataset occupancy-v1` hoặc group-v1 chỉ xóa dataset đã xác minh. Không chạy rollback khi ứng dụng đang sử dụng dữ liệu. Rollback không tự xóa các runtime role còn được dùng.

Khi seed có dữ liệu mới, file runtime.private.json chứa account read-only được tạo; giữ file ngoài Git/log. Để khởi chạy backend local: profile m1-demo, hai flag forecast/group true, HOTEL_AI_DEMO_DATA_SOURCE=db, M1_DEMO_DB_URL=jdbc:postgresql://localhost:5433/hotel_booking_db và USER/PASSWORD từ file đó. Không đổi credentials datasourceCore. Giữ frontend/API routes hiện có; notice vàfixed dates vẫn như demo cũ. Sau một no-op có thể dùng file credential của lần seed trước; NewReader chỉ dùng khi chủ động cần tài khoản read-only mới.

Kiểm toàn bộ sandbox:

```powershell
./tools/acceptance/Verify.ps1 -IncludeAnalyticsDbDemo
```

Runner yêu cầu sandbox idle/fixedloopback vànamespace m1_demo chưa tồn tại; snapshot trước mutation, seed saumigrationCore, bậtDBmode cho backend, dùng Keycloak/API/browser hiện hữu. Cuối lượt dừng apps, cleanup dataset/role/schema thuộcrun, restoresnapshot vàruntimeconfig nguyênbyte. Không seed DBuser từ runner này. Utility mainDB giữ nguyên.

## Bằng chứng cuối và review

- Provisionlocal PASS:2datasets SYNTHETIC/READY,840occupancy,7group,15candidate/23quota/36nightrows; source/hash/digest/payload đối chiếu từ mainassets. Seedlần2no-op khôngthêmrole.
- Targeted cuối **60/60 Java PASS**, 0 skip: 48 demo tests hiện hữu, 6 provider unit và 6 PostgreSQL IT trên DB thử nghiệm riêng. Bao gồm parity cả 9 scenario, role không đọc Core/ghi demo, revoke quyền, DB unavailable và HTTP 503. Sửa payload rồi tính lại digest vẫn bị từ chối bởi đối chiếu với dataset v1 đáng tin; response vẫn được dựng từ dữ liệu đọc DB, không fallback sang fixture.
- Lượt refactor đầu: 48 test có 4 FAIL do mapping region 10/20 và thông báo lỗi corrupt Group không khớp contract. Đã sửa code, giữ nguyên old tests/gold; lượt trung gian 59 PASS và lượt cuối 60 PASS được lưu riêng, không cộng lặp coverage.
- Provisioning6checks PASS trênDBtest riêng: no-op, seed/rollback drift rejection, selective rollbackpreservesGroup, failgiữa transactionatomic vàdatasetrestore.
- Python34PASS gồm4newprovisioner+30oldacceptance. Lượt sandbox banđầu63subtestfail/4errors do filesystem guardkhông truy cập metadata thật; rerun unsandboxedWindowsPASS, không đổioldverifier/tests/guards.
- **Full Verify cuối PASS**, run `20261102-225449-312-ac3b3133`: 1.486 backend, 39 API và 100 browser test; không failure/error/skip trong các executions. Maven clean test và context smoke là hai lệnh. Child Test.ps1 trả PARTIAL/exit1 vì skip Maven đã chạy riêng; supervisor xác minh sáu suite còn lại rồi kết luận PASS. [Run report cuối](runs/acceptance-20261102-225449-312-ac3b3133.md).
- Full run đầu `20261102-195004-437-ba5db453` cũng PASS cùng số executions, trước bổ sung kiểm tra digest tự tính lại. Giữ cả hai báo cáo; không cộng thành số test mới. Java source cuối được kiểm trong lượt cuối. Các script hashes đầu/cuối lượt cuối khớp; 21 non-doc source hashes được khóa và kiểm lại sau verification.
- **107 tooling tests PASS** qua bốn lệnh riêng: 34 acceptance (4 mới), 58 T12, 9 route và 6 CI guards. YAML, PowerShell parse, diff và privacy review PASS. Audit giữ 12 moderate tooling baseline, không còn high/critical; không nâng dependency để che kết quả.
- DB local có đúng 2 dataset READY, 840/7/15/23/36 rows tương ứng history/scenario/candidate/quota/night. Public/Utility normalized digest giữ nguyên; reader `m1_demo_reader_d202cff8de` có SELECT demo, không quyền ghi demo hoặc đọc bảng public, không superuser/inherit/create-role. Password không ghi trong báo cáo/Git/log; private file được giới hạn ACL cho user hiện tại và SYSTEM.
- Cleanup PASS: DB `m1_analytics_test` và hai role thử nghiệm đã được dọn sau khi xác minh không connection/dữ liệu khác. Sandbox dataset/role/schema được rollback, snapshot restore và apps/containers dừng. Original env.json giữ nguyên byte, compose config được tái tạo. DB người dùng `hotel_booking_db` giữ schema demo và Utility; `hotel_postgres` được giữ chạy để DBeaver sử dụng.
- Trước resume, một lệnh chưa được thực thi vì automatic approval review gặp giới hạn sử dụng; sau continue đã qua cùng cơ chế. DB local lúc resume dừng nên guard từ chối trước mutation; đã xác minh đúng volume rồi khởi động lại. Không coi hai tình huống này là test PASS hoặc lỗi ứng dụng.
- Git/CI sau commit được ghi trong PR và receipt delivery; local PASS chưa tự chứng minh CI hoặc merge. Không gọi AI self-review là peer approval.
- CI lượt đầu trên PR #53: 5 check PASS; job PostgreSQL FAIL tại provision với `DATABASE_OPERATION_FAILED`, trước khi chạy IT. Log không đủ để khẳng định nguyên nhân chính xác. Bước readiness dùng socket có thể nhận server khởi tạo tạm; đã chuyển sang `SELECT 1` có xác thực qua TCP đến đúng database. Chạy nguyên bước workflow đã sửa trên container mới: seed PASS, 6 PostgreSQL IT PASS, 6 provisioning checks PASS và container được dọn. YAML và 6 CI guards PASS. Chỉ workflow và báo cáo thay đổi sau full Verify cuối; kết quả CI của commit sửa được theo dõi riêng trong PR.

## Files và ownership

Member1: mới ai/demo/data provider/config, DBdemo XML+SQL, providertests/PGIT, runner/helperPython/PowerShell vàdocs. SửaOccupancy/Group demo service/config để chọn nguồn; occupancycontroller thêm503. Engines, frozenassets/gold/oldtests không sửa. Shared tools Run-Application.ps1 truyền datasource demo riêng cho backend vàxóabiếnkếthừa; Verify.ps1 thêmopt-in seed/cleanup/configrestore. Shared .github/workflows/member1-ci.yml thêmPostgreSQLintegrationjob vàrequireevidence, tăngminimum1472→1478 theo6unitmới; không giảmthreshold. Không sửa business/UI Member4 hoặcMember2/3 Core/security/IAM/schema public. Tất cả file/lý do cuối được khóa qua diff/deliveryreceipt.

Các file shared được sửa, cùng lý do:

| File | Lý do |
|---|---|
| `tools/acceptance/Run-Application.ps1` | Truyền nguồn DB và private reader riêng cho backend; frontend không nhận credential |
| `tools/acceptance/Verify.ps1` | Thêm opt-in seed DB, runtime switching, cleanup và khôi phục config |
| `.github/workflows/member1-ci.yml` | Thêm PostgreSQL integration job với 6 IT thực chạy, provisioning guards và giữ regression minimum tăng theo 6 unit mới |
| `docs/member1/acceptance/README.md` | Thêm link hướng dẫn và giới hạn nghiệm thu |

**Không có file business/UI của member khác được sửa.** Role kỹ thuật và schema mới chỉ thuộc demo local đã được user duyệt. Những file SQL bootstrap, Liquibase root, Booking/Inventory/security và cấu hình deployment của team giữ nguyên.

Logs, dumpsdemo, generated credentials vàreceipts nằm ở tools/acceptance/.runtime/analytics-db/ vàruns/ ngoàiGit. Rawpg_dump có nonce restrict token ngẫu nhiên; so sánh businessdigest cần bỏ đúnghai dòngguardnhưhelper hiện hữu, không bỏrows. Hashrawtrước/sau khácdo nonce không phải bằng chứngdữliệubusinessđổi; ghi riêng baseline normalized trước request vàverify cuối. Không đọc/in credentialfile của user hoặc identity/payrollPII. Rollback app→fixture/datasetdeleteguarded; V03realdata/officialBCA vẫnchưaxácnhận.
