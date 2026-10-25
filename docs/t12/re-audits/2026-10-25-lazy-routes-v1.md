# T12 source re-audit — lazy routes v1

**CONFIRMED:** `App.tsx` trên `develop@52dc842c4e361ce729f8dd742fca5236eebe535a` chứa thay đổi lazy loading từ commit `8d5a34791013e03478163f476799f4f776b460a8`, đã merge qua PR #27. So sánh với parent `25ac13fc4db8087984a584e572c5ee639e03e9e1`.

Đây là đối soát nguồn được ủy quyền trong lượt follow-up; không phải phê duyệt schema/gold hay đánh giá engine. Snapshot đầy đủ entry nguồn cũ, các route và import mapping được lưu ở [audit JSON v1](2026-10-25-lazy-routes-v1.json).

## Kiểm tra source

- TypeScript AST xác nhận **39 node Route** giữ nguyên cây cha/con và toàn bộ attribute; bao gồm path, index, ProtectedRoute, role lists và Navigate redirect/replace.
- **22 component** chuyển sang lazy loading, giữ nguyên module và component thực thi. ProtectedRoute tiếp tục import đồng bộ.
- Bốn page SearchPage, CheckoutPage, MyBookingsPage, RoomDetailsPage đổi cách truy cập từ named export sang default. Source của từng module không đổi giữa hai commit; AST xác nhận `export default` trỏ đúng cùng named component. Audit JSON ghi hash và alias của cả bốn.
- Chỉ thêm Suspense/loading status và đổi cách import; không thay route hoặc logic Booking/Pricing.
- Hash App.tsx canonical LF mới: `59ee585d21004047316138394d4e3b6fe966ca40f7da1a986cae4becd3fe7b34`.
- Hash CRLF mới: `cd4e7b5eb8818a0a3d742e01a35f48da45580bbb7163d144be5c7b9207a3415d`.

## Cập nhật bằng chứng

Chỉ entry `frontend/src/App.tsx` trong `baseline-source-evidence.json` được reconcile theo mẫu metadata reconciliation đã có. Hash trước đó và commit a6b2088 được giữ ở `previousReconciled*`; entry cũ nguyên vẹn được giữ trong JSON audit mới. `gitSha` của dossier gốc vẫn là mốc lịch sử; `reconciledFromDevelopCommit` chỉ rõ phiên bản nguồn hiện được kiểm tra. Trường legacy `baselineUnmodified` tiếp tục được validator dùng để kiểm tra expected source record, không khẳng định file bằng source tại mốc Git lịch sử ban đầu.

Không sửa validator, schema, fixture, gold answers, ngưỡng hoặc quyết định approval. Các artifact R1-00 lịch sử khác giữ nguyên.

## Tái hiện

Sau `npm ci` trong frontend, chạy tại repository root:

```powershell
node tools/t12/verify_lazy_route_reaudit.cjs
python tools/t12/validate_r1_contracts.py
python tools/t12/test_portability_validation.py
```

Runner đối soát chỉ đọc; yêu cầu hai commit đã ghi trong JSON còn trong Git local. Kết quả: AST đối soát PASS; contract artifact validator PASS (184 instance, 74 eval case, 23 protected hash, 34 source hash); portability 11/11 PASS. Artifact validator vẫn báo `engineEvaluation=NOT_RUN`; không sử dụng kết quả đó để tuyên bố engine đã được đánh giá bởi gold suite.
