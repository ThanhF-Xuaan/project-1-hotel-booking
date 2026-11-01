# V03-D01 — Kiểm chứng gói mock sau khi chia sẻ

**CONFIRMED — PASS local cho verifier exact synthetic pack.** Base develop672fc742e3741e24a0679eb2849aed268a024e59, branch codex/m1-v03-pack-verifier-20261101, worktree ban đầu sạch. User ủy quyền tự quyết implementation/review/fix/commit/push/PR/merge và cross-member changes; không giả human peer approval. [Đặc tả/allowlist](../plans/v03-pack-verifier-2026-11-01.md), [cách dùng](m1-v03-mock-data-guide-2026-10-31.md).

## Hành vi

CLI `python tools/acceptance/verify_m1_mock_pack.py --input-directory <directory>` kiểm gói có sẵn, read-only và offline. Gói chuẩn có24files/3subdirectories,248104bytes:840occupancy rows,516utility readings/12scenarios,7Group scenarios và1lodging draft. Rebuild canonical expected bytes bằng generator hiện có và frozen source hashes; đối chiếu toàn bộ file/manifest. Không parse/trust manifest nhận vào để chọn file, không chỉ so hash tự khai. Một lỗi làm cả pack FAIL; không sửa/gộp/chuẩn hóa/relabel input hoặc coi PASS là dữ liệu production.

Layout/path/type/link/size được kiểm trước khi đọc payload. Reject extra/missing/nested paths, symlink/junction/reparse point, hardlinks và special files; root/ancestors cũng được kiểm. Read flags readonly/no-follow khi platform hỗ trợ, nonblocking để không mắc kẹt special-file race, fd identity trước đọc và metadata/path/parent checks trước/sau. Scan tối đa27entries hợp lệ, file reads tối đa expectedLength+1, trusted total budget1MB. Không đọc path từ input manifest; source lỗi fail closed trước input access.

Output là JSON với fixed reason, không echo input path/content/exception; argparse errors cũng được thay để không lộ giá trị argument. Exit0PASS/1FAIL/2usage; help in hướng dẫn. PASS luônSYNTHETIC/productionReady=false, counts và exact verification basis. Không network/DB/ZIP/import hoặc ghi input/output file. Việc shell lưu stdout vào receipt ngoài pack là hành động gọi riêng, không phải side effect của verifier.

## Review/fix và test

- Lượt đầu27tests có62subtest failures/2errors vì Windows DirEntry.stat trả inode/device/link count0; positive pack bị chặn sai. Dùng Path.lstat lấy metadata thật và thêm regression names-only scandir. Không bỏ guard inode/link.
- Lượt tiếp28tests có32subtest failures/3errors vì Windows stat/fstat ctime khác semantics dù cùng file. Tách cross-API identity khỏi ctime, giữ ctime version checks trong cùng API trước/sau; thêm regression mô phỏng variation. Không nới content/hash/path/link hoặc mutation assertions. Logs FAIL giữ riêng.
- Final **30/30PASS,0skip**:24test verifier mới+6generator cũ. Bao gồm mọi24file tamper cùng độ dài và mọi24file missing, payload+manifest rehash, relabel/count/oracle/duplicate/invalid JSON, layout/oversize/truncation, actual root/ancestor/child junctions trên Windows, hardlink, special-mode guards và actual nonregular entry, replacement trước đọc, mutation file/dir khi đọc, partial reads, source failure/budget, CLI success/fail/usage và không leak nội dung.
- Test directory aliases dùng junction thật trên Windows, symlink thật trên POSIX; actual nonregular entry là directory trên Windows/FIFO trên POSIX. Các platform-independent guards đều chạy; không dùng skip hoặc bỏ assertion để lấy PASS. Test stat/fstat metadata variation chạy trên cả hai platform.

| Lệnh thực chạy | Kết quả |
|---|---|
| `python -B -m unittest discover -s tools/acceptance -p 'test*.py' -v` | PASS30/30 |
| `python -B -m unittest discover -s tools/t12 -p 'test*.py' -v` | PASS58/58 |
| `python -B -m unittest discover -s tools/member1 -p 'test*.py' -v` | PASS9/9 |
| `python -B -m unittest discover -s tools/ci -p 'test*.py' -v` | PASS6/6 |
| CLI generate→verify canonical pack; tampered payload+rehash→verify | PASS: canonical24files/248104bytes exit0, tampered exit1/CONTENT_MISMATCH; sanitized JSON/no stderr |
| AST parse hai file Python mới; diff/privacy review | PASS |
| Local Maven/frontend/DB/HTTP/browser runtime suites | NOT RUN — chỉ thêm offline Python tooling/docs; không đổi ứng dụng hoặc runtime contracts |

Tổng103tooling tests được thực hiện bằng bốn lệnh riêng. CI hiện hữu tự discover file test mới dưới tools/acceptance; backend minimum1472/required suites và67mockbrowser giữ nguyên. Kết quả Linux CI/exact-head/postmerge và delivery cuối theo PR/receipt/canonical handoff; không gọi Windows local là proof Linux hoặc production.

## File sửa và ownership

| File | Lý do |
|---|---|
| `tools/acceptance/verify_m1_mock_pack.py` — Member1 mới | Offline/read-only verifier, bounded/safe filesystem reads và sanitized CLI |
| `tools/acceptance/test_mock_pack_verifier.py` — Member1 mới |24tests mới và portability/tamper/race/CLI regression |
| `docs/member1/acceptance/m1-v03-mock-data-guide-2026-10-31.md` — shared guide | Thêm cách kiểm gói đã sao chép, giới hạn exact bytes/links/semantics |
| `docs/member1/acceptance/README.md` — shared index | Dẫn tới guide/report mới |
| `docs/member1/plans/v03-pack-verifier-2026-11-01.md`, report này | Lưu đặc tả, phạm vi và bằng chứng |

**Không có file nghiệp vụ hoặc UI của member khác được sửa trong subtask này.** Shared docs là hai file cần cập nhật để team chạy đúng tool; không thay generator, oldtests, frozen fixture/gold, backend/frontend/schema/IAM/dependencies/CI/CD. Không reset/stash/overwrite repo D hoặc protected worktrees. Canonical handoff/plan append-only sau delivery, giữ prefix bytes.

## Giới hạn, dữ liệu và rollback

Cần trusted checkout/source để xây canonical export; đây không là real-data validator/importer hoặc chứng minh ownership/IAM. Verifier kiểm byte/default namespace/metadata thường, không quét mọi filesystem metadata và không bảo đảm atomic khi actor có quyền thay ancestor đang chạy. Không nhận ZIP, không vượt qua links/hardlinks để đọc; cần local filesystem hỗ trợ identity và thư mục không bị sửa đồng thời. Đường dẫn có `..` bị từ chối; JSON normalization/LF changes cũng FAIL theo exact-byte contract.

PASS mock không đóng các gate occupancy/utility/Group provider/data thật/IAM hoặc mẫu BCA chính thức. Gói lodging vẫn humanVerified=false/nonofficial draft; Group vẫn fixed asOf/TTL, utility giữ unit/epoch/marker và decimal, occupancy không điền số0 vào coverage thiếu. Các dấu hiệu này được giữ đúng byte, không phải kiểm nghiệp vụ trên nguồn thật mới.

Output/logs/source hashes/CLI fixtures và receipt nằm ngoài Git tại `C:/Users/admin/.codex/outputs/v03-pack-verifier-2026-11-01/`; không chứa credential/PII thật. Rollback ngừng dùng tool hoặc scoped revert PR, không DB/schema rollback. Tình trạng deployment của team được theo dõi riêng; không khẳng định Group business acceptance bằng general healthchecks.
