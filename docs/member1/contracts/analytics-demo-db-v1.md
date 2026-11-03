# Occupancy/Group — nguồn PostgreSQL tổng hợp v1

User duyệt schema demo riêng và tích hợp DB→API→UI. Data-source server là fixture (default) hoặc db; không nhận selector nguồn/URL/schema từ HTTP client. Giữ các GET /api/v1/ai/demo/occupancy và /api/v1/ai/demo/group-room-assignment, mỗi request nhận tối đa một query scenario, envelope và fixed clocks cũ. CHAIN_ADMIN, m1-demo cùng flag forecast/group riêng và !prod/!production. Giá trị cấu hình khác fixture/db làm khởi tạo demo thất bại; DB thiếu/lỗi/drift làm request503fixed, không fixture fallback. Invalid scenario400, anonymous401, nonadmin403; khi route tắt authenticated404.

## Schema và provenance

Schema m1_demo chứa datasets, occupancy_samples, group_scenarios, group_candidates, group_quotas, group_night_eligibility. Khóa/FK chỉ nằm trong schema demo theo dataset/scenario; ID không mặc định tham chiếu public.hotels hoặc23phòng hiện hữu. Hai bảng Liquibase registry được đặt cùng schema. Changelog db/demo/m1-analytics.xml nằm ngoài root includeAll, chỉ chạy explicit qua provisioner.

occupancy-v1:840rows/2scenario HOTEL_A,HOTEL_B, hotel1/region10 vàhotel2/region20;420ngày từ2025-09-05 đến2026-10-29, reference2026-10-29T17:00:00Z. Missing giữnullcounts; zero capacity khác missing. group-v1:7cases theo main resource hiện có,15candidates/23quotas/36night eligibility, reference2026-10-31T02:00:00Z; quota0/bottleneck/no-transfer giữ nguyên. Không expected/gold trong DB; sourcehash pin theo mainassets đã review, body digest PostgreSQL lưu và kiểm trong dataset manifest.

Tất cả vẫnSYNTHETIC/businessData=false; Group previewOnly=true. Đây là replay với dữ liệu tổng hợp trong DB, không live inventory/demand forecast hoặc phân phòng nghiệp vụ. Mỗi request đọc READY/version1/sourceSHA/reference/digest đúng, parse scope/shape/bounds và giữ engine validation. Reader không traCore/PII hoặc gọi bất kỳ write service nào. Không đổi JSON fixture/frozen tests/algorithms/thresholds; kết quả fixture vàDB phảiparity.

## Seed, credentials và rollback

Runner Seed-M1AnalyticsDemo.ps1 gọi Python stdlib/docker/psql và Liquibase4.31 hiện hữu. Target chỉ hotel_postgres với volumeproject-1-hotel-booking_pg_data và hotel_booking_db; hoặc container mang label hotel-m1-acceptance với DBhotel_acceptance_db. DBm1_analytics_test dành riêng IT. Kiểm running/namespace/source hashes, backup schema trước sửa; migration vàseedDML riêng. DML atomic/advisory lock/lock timeout, khôngupsert. Cùng version/payload no-op; drift dừng, không tựrepair. Receipt ghi provenance/digest và tênrole, khôngpassword.

Một seed có insert tạo role m1_demo_reader_<10hex> mới, NOINHERIT/NOSUPERUSER/NOCREATEDB/NOCREATEROLE/NOREPLICATION; chỉ GRANT CONNECT, USAGE/SELECT schema demo. Password ngẫu nhiên lưu runtime.private.json trong evidenceignored, khônglog/Git. Rerun no-op không thêmrole; NewReader opt-in phục hồi credential mới khi cần, không đổi passwordrole đã có. Role cũ có thể xóa riêng sau khi app dừng và xác minh ownership.

App dùng pool Hikari riêng, khôngđăngký DataSource thay datasourceCore: lazy/minIdle0/max2/connect5s/readOnly, repeatableRead vàqueryTimeout5s. Env M1_DEMO_DB_URL chỉ loopback với tênDB local allowlisted; USER/PASSWORD riêng, từ chối postgres/admin role. Kiểm runtime role không có quyềnpublic/Core hoặc quyềnwrite demo, auth trước/sau replay; JSON lỗi chỉmessagecốđịnh. Metadata ngày giờ/dataset tổng hợp không phải bằng chứng IAM business thật.

Rollback dataset theo key/version cần snapshotpayload/digest đúng; cascade chỉ các childrowsdemo. Không DROP public/resetsequences hoặcrestore dump để xóa dữ liệu mới của user. Chuyển data-source=fixture để rollback ứng dụng. Cleanup acceptance chỉ được dùng schema mới tạo đã xác minh trống trước test, rollback dataset và xóarole/schema thuộc run trước snapshotrestore. Schema đang có dataset của user được giữ nguyên.
