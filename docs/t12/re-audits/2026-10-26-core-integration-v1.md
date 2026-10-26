# Đối soát source Core sau tích hợp develop ngày 2026-10-26

**CONFIRMED source reconciliation**, so sánh `368f6e466cacfd5f7836e9d24b0908b9e1c1a132` với develop `96d7815b4e5d13f5052ca0fa6a6c414c8297ea1e`. Đây là đối soát provenance và cấu trúc API, chưa phải kết quả nghiệm thu runtime hay phê duyệt gold/model.

## Phạm vi đã review

- ErrorCode.java: chỉ thêm nhóm lỗi Group Booking/master folio. Verifier đối chiếu toàn bộ tuple code/message/HTTP status cũ với Git baseline; các tuple cũ phải giữ nguyên.
- RoomAvailabilityServiceImpl.java: thêm BlockedInventoryProvider; available count trả ra đã trừ group-block và chặn giá trị âm. T12 tiếp tục đọc count từ Core, không tính lại tồn phòng. Diff upstream còn áp dụng group-block vào hold methods của Core; advisor không thêm đường gọi hold/write.
- Read path mới: GroupRoomBlockProviderImpl → GroupRoomBlockServiceImpl.getActiveBlockedRooms (`@Transactional(readOnly=true)`) → GroupRoomBlockDailyRepository.sumActiveBlockedRooms. Query tính phần chưa picked-up của daily ACTIVE, block DEFINITE, contract DEPOSIT_PAID/ROOMING_LIST_ATTACHED.
- RoomAvailabilitySearchDto, RoomAvailabilityResponse và PublicBookingCoreAdapter giống bản so sánh. Response structure và adapter contract không bị đổi bởi hai file source drift này.
- Các dependency mới được kiểm tra đúng Git source và hash trong JSON; điều này không phê duyệt toàn bộ nghiệp vụ phòng đoàn/folio/BI.

## Giữ lịch sử và kiểm tra

[JSON đối soát](2026-10-26-core-integration-v1.json) giữ nguyên toàn bộ entry audit cũ của hai file, source commit IDs, hash LF/CRLF và digest patch Git. Manifest nguồn chỉ cập nhật hai entry này; các entry còn lại và metadata lịch sử giữ nguyên.

PR 24 của ErrorCode là provenance của hash cũ, được chuyển sang previousReconciledFromPr và giữ nguyên trong previousSourceEvidence; không gán source Core mới cho PR cũ. Source mới được định danh bằng accepted develop commit, sourceChangeCommits và patch digest.

`baselineUnmodified=true` là field legacy validator yêu cầu cho source đã đối soát trong manifest hiện hành; không có nghĩa hai file còn giống snapshot lịch sử gốc. `historicalSha256`, previousSourceEvidence và các commit gốc vẫn giữ dấu vết đầy đủ. Không đổi validator, gold answers, threshold, dataset hoặc approval register.

```powershell
python tools/t12/verify_core_reaudit.py
python tools/t12/test_core_reaudit.py
python tools/t12/validate_r1_contracts.py
python tools/t12/test_portability_validation.py
```

Verifier mới chỉ đọc; kiểm tra source hiện tại đúng accepted Git source, patch và lịch sử metadata. Mutation test chạy in-memory, gồm source/hash cùng bị sửa, metadata lịch sử, provider, ErrorCode cũ và line endings. Verify.ps1 gọi thêm verifier này trước nghiệm thu API/browser.

Trước đối soát, validator FAIL với `Source changed after audit: .../ErrorCode.java`; log giữ tại runtime `post-merge-contract-before.log`. Kết quả sau đối soát và runtime được ghi trong [báo cáo tích hợp](../../member1/acceptance/post-merge-revalidation-2026-10-26.md).
