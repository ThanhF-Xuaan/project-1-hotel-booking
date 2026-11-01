# V03 — Dữ liệu mock để làm việc trước khi có dữ liệu thật

CONFIRMED: user yêu cầu tạo mock trước và sẽ cung cấp dữ liệu thật sau. Pack này chỉ có dữ liệu tổng hợp; không chứa khách sạn, khách lưu trú hoặc booking thật. Nguồn fixture đã khóa hash được giữ nguyên; các nhãn đáp án `expected` không được đưa vào dữ liệu xuất.

Tạo pack mới, không ghi đè pack cũ:

```powershell
python tools/acceptance/generate_m1_mock_data.py --output-directory C:/Temp/m1-v03-mock
```

| File | Nội dung |
|---|---|
| `occupancy/hotel-1.json`, `hotel-2.json` | 840 dòng tổng cộng, 420 ngày/khách sạn; capacity theo ngày, coverage thiếu được giữ nguyên |
| `utility/*.json` | 12 kịch bản độc lập, 516 lần đọc; giữ unit KWH/M3, counterEpoch, marker reset/replacement, giá trị decimal dạng chuỗi |
| `group/*.json` | 7 kịch bản request/snapshot, physical beds/floors, quota và eligibility theo mọi đêm |
| `lodging-review.json` | 1 draft nhân thân tổng hợp, cần người xem/sửa/xác nhận; chưa có định dạng BCA chính thức |
| `manifest.json` | `dataMode=SYNTHETIC`, số lượng, SHA-256 nguồn và từng file; `productionReady=false` |

Các mốc `referenceInstant`/`asOf` là thời gian cố định của demo. Khi chạy service local cần dùng clock theo mốc đó; Group snapshot có TTL nên không được coi là tồn kho hiện tại. Mỗi utility file là một scenario riêng, không ghép các meter trùng ID của nhiều scenario thành một lịch sử. Occupancy thiếu dữ liệu không được điền số0. Lodging draft giữ `humanVerified=false`; JSON này dùng xem xét dữ liệu, không phải file gửi BCA.

Kiểm chứng bằng thuật toán thật, không mở API hoặc ghi DB:

```powershell
cd backend
.\mvnw.cmd '-Dtest=OccupancyHistoryFixtureTest,OccupancyForecastFixtureTest,UtilityAnomalyFixtureTest,GroupPreviewFixtureTest' -DfailIfNoTests=true test
```

Bốn class này đã chạy14 test PASS trên nguồn fixture gốc tương ứng. Test generator còn kiểm hash, tính tái lập, thiếu oracle, nhãn synthetic, nguồn bị sửa và chống ghi đè. Các input JSON là bản phục vụ demo; API/UI business của U22/Group và adapter dữ liệu thật vẫn là phạm vi tích hợp riêng. Khi nhận dữ liệu thật sẽ kiểm mapping, scope/IAM, completeness, unit/epoch và tính hợp lệ trước khi nối nguồn. Không chuyển một mock thành dữ liệu thật bằng cách đổi nhãn.

Bản pack đã tạo trên máy kiểm chứng: `C:/Users/admin/.codex/outputs/member1-persistent-rag-http-2026-10-31/v03-mock-data/`; ZIP cùng thư mục cha, SHA-256 nằm trong `mock-pack-receipt.json`. File sinh ra được giữ ngoài Git; generator và hướng dẫn được commit để team tạo lại.

## V03-D01 — Kiểm tra lại gói sau khi sao chép

Chạy trực tiếp trên thư mục gói mock:

```powershell
python tools/acceptance/verify_m1_mock_pack.py --input-directory C:/Temp/m1-v03-mock
```

Verifier so sánh đủ24file/248104byte với canonical export từ frozen sources, gồm manifest; không tin hash, đường dẫn hoặc nhãn tự khai trong manifest nhận vào. Thiếu/thừa file, sửa nội dung dù đã tính lại manifest hash, đổi nhãn REAL/productionReady hoặc chèn oracle đều làm cả gói FAIL. Chuẩn hóa lại JSON, đổi LF/CRLF hoặc thêm BOM cũng bị từ chối: đây là kiểm chứng exact bytes của gói mock, không phải kiểm định JSON tùy ý.

stdout là một JSON an toàn: PASS có dataMode=SYNTHETIC/productionReady=false, counts/filesVerified/bytesVerified và verificationBasis=EXACT_FROZEN_EXPORT_V1; FAIL chỉ có reason cố định, không có nội dung/đường dẫn. Exit0PASS,1FAIL,2sai arguments; `--help` in hướng dẫn và exit0. Công cụ không sửa input, tạo report, gọi mạng hoặc nạp DB. Lưu stdout ngoài thư mục pack nếu cần receipt; thêm file receipt vào pack sẽ làm sai exact layout.

Chỉ dùng thư mục local thường; đường dẫn không chứa `..`, cả ancestor/root/subfolder không là symlink/junction/reparse point. File phải regular/single-link, metadata identity khả dụng; special files bị từ chối. Scan bounded27entries hợp lệ, đọc mỗi file tối đa expectedLength+1 với trusted expected total không quá1MB. Trên Windows dùng lstat thay DirEntry metadata và so ctime trong cùng API để giữ compatibility. Đây là kiểm byte/namespace thường; không phải scanner toàn bộ filesystem metadata hoặc bảo đảm atomic trước tác nhân có quyền đổi ancestor trong lúc chạy. Đặt gói trong thư mục riêng không bị chỉnh sửa đồng thời.

PASS chỉ xác nhận đúng bản mock hiện hữu; không xác nhận nguồn dữ liệu thật, IAM/provider, inventory hiện tại hoặc định dạng BCA. Khi có dữ liệu thật phải dùng contract/adapter được kiểm riêng, không đổi nhãn mock để đóng V03. [Báo cáo verifier](v03-pack-verifier-2026-11-01.md) ghi bằng chứng Windows/Linux, review/fix và Git delivery.
