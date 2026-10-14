# T12 R1-00 — Contract & Evaluation Foundation

**Trạng thái:** hoàn thành bộ draft để review; chưa được phép integration hoặc triển khai R1-01/R1-02. Contract artifact `2.0.0-draft.1`, wire `2.0`, evaluation `0.1.0-draft`. Chỉ quyết định sản phẩm và quyền thực hiện R1-00 đã được người dùng duyệt; Member 2/4 chưa duyệt contract chi tiết.

## Phạm vi bàn giao

- [DTO contracts](dto-contracts.md): RoomSearchCriteria, CHAT, SEARCH, REFRESH, RoomRecommendation và quy tắc semantic validation.
- [OpenAPI 3.1](openapi.json), [Advisor JSON Schema](schemas/advisor.schema.json), [Core fixture schemas](schemas/core.schema.json), [examples](examples/results-response.json).
- [Baseline audit](baseline-audit.md): commit, API thực tế, khác biệt và giới hạn xác minh.
- [Decision register](decisions-and-approvals.json): assumptions, owner, blockers, trạng thái human approval.
- [Đối chiếu governance mới](governance-alignment.md): nguồn hướng dẫn vừa cập nhật, CR mapping và evaluation coverage.
- [Member 2 Backend Contract Review](member2-backend-review.md) và [Member 4 Frontend Contract Review](member4-frontend-review.md).
- Fixtures chỉ ở `backend/src/test/resources/contracts/t12/r1/v2.0.0-draft.1/`; evaluation ở `eval/t12/r1/v0.1.0-draft/`.
- [Verification report](verification-report.md): kết quả kiểm tra và toàn bộ file thay đổi.

## Những điểm đã chốt

Mỗi phương án gồm một HotelRoomType cho một phòng. Trả Top 3–5 khi có đủ phòng phù hợp; trả ít hơn nếu thiếu. Giữ `roomsCount`; yêu cầu từ hai phòng phải hỏi lại, không gọi Pricing để nhân giá. Chỉ có hành động xem chi tiết phòng. Java kiểm soát lựa chọn, ranking, ID, giá và URL; AI không có quyền hold, release, booking, cancel, payment hay refund.

Release 1 hoạt động độc lập với RAG. Contract giữ `knowledgeStatus=DISABLED`, `sources=[]`. Không thêm dependency Spring AI, provider credentials, vector store hoặc gọi Gemini ở task này.

## Cách kiểm tra lại

Chạy từ root của worktree, với Python 3.12 và môi trường validation riêng:

```powershell
python -m venv "$env:TEMP\t12-r100-validation-venv"
& "$env:TEMP\t12-r100-validation-venv\Scripts\python.exe" -m pip install -r tools/t12/requirements-contract-validation.txt
& "$env:TEMP\t12-r100-validation-venv\Scripts\python.exe" -B tools/t12/validate_r1_contracts.py
git diff --check
```

Validator kiểm tra cấu trúc, references, fixture invariants và negative contract cases; không chạy assistant hoặc đo accuracy/ranking/latency. Bộ evaluation có 74 cases và mapping 35 yêu cầu Release 1 của draft v2 mới; chưa có kết quả execution. Chỉ freeze đáp án sau review theo quy trình tại `eval/t12/r1/v0.1.0-draft/README.md`.

## Điều kiện dừng

Member 1 có thể dùng synthetic fixtures để phát triển độc lập sau khi được duyệt task tiếp theo. Public room discovery/detail, currency/fee semantics, session ownership và detail route là blockers trước integration. Không sửa module của Member 2/4 để vượt blockers. Task hiện tại dừng tại review R1-00.
