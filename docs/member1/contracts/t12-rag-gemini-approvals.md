# T12 RAG Gemini — approval register và receipt TASK-01

- Ngày: 04/11/2026 (UTC+07).
- Contract: [t12-rag-gemini-v1.md](t12-rag-gemini-v1.md).
- Trạng thái: **PROPOSED contract / TASK-01 documentation only**. Đây không phải approval triển khai toàn feature hoặc biên bản peer review.
- Baseline đã pull: `63a26ba4c81c92e9892383de29bf76bc96b617b9`.
- Branch: `codex/m1-t12-rag-gemini-contract-20261104`.

## 1. Bằng chứng quyết định thực có trong chat

| ID | Người quyết định / nguồn | Nội dung được xác nhận | Giới hạn |
|---|---|---|---|
| U01 | User tự nhận Member 1; câu trả lời lựa chọn khi lập plan | Chọn RAG Gemini thực, hỏi đáp độc lập từng lượt, 3–5 ngày, synthetic được duyệt | Không là approval shared dependency/config/security/DB hoặc UI của owner khác |
| U02 | User chọn “Trích tài liệu” khi Gemini gặp lỗi | Ưu tiên trả evidence có nguồn khi generation lỗi | Warning/envelope/UI delta vẫn PROPOSED chờ owner review |
| U03 | User chọn “Tối đa 5 USD”; câu hỏi ghi rõ chưa cấp phép gọi model | Hạn mức dự kiến cho đợt kiểm chứng | Không được hiểu là đã cấp quyền gọi live/chi tiền ở TASK-01 |
| U04 | Yêu cầu hiện tại: “hãy pull origin sau đó tạo branch mới và triển khai TASK-01” kèm hai file và acceptance | Pull origin, tạo branch và viết đúng hai tài liệu | Không cấp quyền TASK-02–10, stage/commit/push/PR/merge hoặc provisioning/migration/model call |

Không có thông điệp Member 2/Member 4/corpus owner phê duyệt delta Gemini v1 trong evidence đã kiểm. Không suy approval từ tài liệu lịch sử, tên branch, quyền agent, quyền tự quyết ở task cũ hoặc việc source cũ đã merge. Không gửi tin nhắn xin duyệt cho người khác trong TASK-01.

## 2. Gate và contract request theo owner

`PENDING` là chưa có approval; blocker áp dụng đúng phần liên quan. Các task ở đây tham chiếu task trong kế hoạch đã trình user; bảng này không tự cho phép thực hiện task đó.

| Gate | Owner cần quyết định | Delta cần review / evidence cần có | Trạng thái | Phần bị chặn |
|---|---|---|---|---|
| G01 | Member 1 / user | Chấp thuận nội dung contract sau khi đọc hai file bàn giao; scope/allowlist/task dependencies | PENDING contract review; U04 chỉ cho phép viết tài liệu | Bắt đầu implementation từ TASK-02–10 cần lệnh riêng và review tương ứng |
| G02 | Member 2 | Spring AI BOM 2.0.2, hai model modules, bean lazy/conditional, key riêng, Boot compatibility; dependency diff + offline context evidence | PENDING, chưa có sign-off | TASK-03 dependency/config và integration phụ thuộc |
| G03 | Member 2 | Public 429/Retry-After, request/concurrency/deadline, legacy 503, fallback warning, admin stage 503; không đổi alias/role/session/CORS/frozen R1 | PENDING | Shared API/security delta TASK-04/06/07; behavior UI tương ứng |
| G04 | Member 2 | Target DB/port, pgvector/prerequisites, reviewed DDL/context, grants, backup/restore và rollback; không primary DB/production | PENDING | TASK-08 provisioning/migration/DB writes, persistent integration |
| G05 | Member 1 + người chịu trách nhiệm provider/privacy/account; Member 2 cho config/security | Chốt chat/embedding model/dimension sau PoC, account/quota/processing terms/retention, giá và ledger, key handling | UNKNOWN account/provider approval; PENDING quyết định | Live PoC TASK-03, outbound/ingestion/eval live TASK-04/08/10 |
| G06 | Owner corpus + Member 1 | Manifest 12 tài liệu synthetic, scope/hotel IDs/effective window/hash, không PII/live facts; user chọn synthetic không đồng nghĩa duyệt từng file | PENDING; corpus mới chưa tạo | TASK-02 publication/ingestion; TASK-04/08/10 dùng corpus đó |
| G07 | Người duyệt eval + Member 1 | Dev/acceptance set mới tách biệt, hashes và thresholds trước run; gold cũ bất biến | PENDING; chưa có dataset/runner Gemini đã duyệt | Chứng nhận quality TASK-05/10; không chặn unit test với fixture synthetic riêng |
| G08 | Member 4; Member 2 nếu phát sinh auth/CORS thay đổi | Warning fallback thành microcopy, 429 UI, synthetic label; giữ scope/abort/safe text, R1 hoạt động | PENDING | TASK-09 frontend changes; không cần sửa frontend ở TASK-01 |
| G09 | User | Cho phép live run cụ thể: scope, config/corpus/eval hash, account và cap <=5 USD | PENDING; U03 chỉ là budget preference | Mọi paid/live call, kể cả PoC |
| G10 | User và quy trình Git/team tương ứng | Commit/push/PR/merge khi có yêu cầu riêng; Member 2 điều phối shared merge | NOT AUTHORIZED trong lượt này | Stage/commit/push/PR/merge |

Mỗi approval mới phải ghi gate ID, danh tính/role người duyệt, nội dung/version/hash được duyệt, điều kiện/expiry nếu có và link hoặc trích dẫn quyết định thật. Không điền ngày/chữ ký hộ. Thay model/dimension, route/status/security/schema, corpus hoặc ngưỡng sau duyệt phải review lại delta; không dùng approval cũ cho thay đổi mới.

## 3. Quy tắc bắt đầu và dừng theo task

- TASK-01 được phép hoàn thành độc lập: ghi hiện trạng, đề xuất và pending gates; không cần giả approval để đóng phần tài liệu.
- TASK-02 cần lệnh triển khai riêng; có thể soạn draft synthetic, chỉ ingest/đánh giá chính thức sau G06/G07 tương ứng.
- TASK-03 cần G02 trước sửa dependency/config; live compatibility PoC cần thêm G05/G09.
- TASK-04–07 cần dependencies trong plan và approvals đúng delta G02/G03/G05; unit tests dùng mock/synthetic không cấp quyền outbound.
- TASK-08 cần G04/G05/G06/G09 và kiểm sandbox/backup; không chạy Docker/migration để hoàn thành TASK-01.
- TASK-09 cần G08 và contract delta liên quan được duyệt; không sửa file owner khác chỉ vì user là Member 1.
- TASK-10 cần G07; live run cần G05/G09, corpus cần G06. Kết quả offline không thay live quality.
- Task không bị gate có thể tiếp tục sau khi được user giao và dependency đã đạt; không tự bắt đầu task khác trong lượt TASK-01.
- Dừng affected slice nếu dirty/hash bất ngờ, model mismatch, scope chưa rõ, corpus/provider approval thiếu, target DB không đúng hoặc phát hiện đường business write. Ghi UNKNOWN/BLOCKED và owner cần quyết định; bảo vệ dữ liệu/công việc hiện có.

## 4. Baseline và Git receipt

1. Preflight: `git status --short` chỉ có ` M tools/acceptance/Run-Application.ps1`; HEAD `6236b78bf2d4e5711a740b2e0379819f7d8a8af6`, không untracked.
2. Lần `git fetch origin` trong sandbox bị từ chối ghi `.git/FETCH_HEAD`; không ghi là fetch thành công. Chạy lại qua cơ chế cấp quyền tool: PASS, origin/develop từ `0b17efa...` lên `63a26ba...`.
3. Nhánh đầu có 5 commit riêng, remote develop có 6 commit riêng; không pull merge lên nhánh đó. `git switch develop`: PASS, giữ dirty file.
4. `git pull --ff-only origin develop`: PASS, fast-forward local develop `0b17efa8fed9eecbd073194f644b0c335c8e49c1` → `63a26ba4c81c92e9892383de29bf76bc96b617b9`.
5. `git switch -c codex/m1-t12-rag-gemini-contract-20261104`: PASS. Nhánh bắt đầu vẫn được giữ; không reset/rebase/stash/clean/merge commit.
6. SHA-256 launcher trước và sau pull/tạo branch: `FB71D39311A3D6F531F31234552AA9E68588CB53FCE65C65CF8715DC2ED2CA90`.
7. TASK-01 chỉ tạo hai file contract/approvals; không stage/commit/push/PR. Primary DB/RAG DB/Docker/provider không được gọi.

## 5. Tiêu chí nghiệm thu tài liệu

| Check | Điều kiện PASS |
|---|---|
| BASE | HEAD bằng origin/develop/base 63a26ba... lúc tạo nhánh; đúng branch TASK-01 |
| PROTECTED | Status tracked vẫn chỉ launcher có sẵn; SHA-256 khớp receipt |
| ALLOWLIST | Chỉ hai file mới dưới docs/member1/contracts; không sửa source/approval lịch sử |
| JSON | Tất cả fenced JSON parse được; request/fallback example dùng đúng field/type/status; UUID citation hợp lệ |
| LINKS | Hai link tài liệu nội bộ resolve; evidence source paths tồn tại trong Git base |
| OWNERS | Gemini model/version/dimension và 429/fallback/quota/privacy đều được gắn PROPOSED/gate, không fake sign-off |
| WHITESPACE | Kiểm whitespace hai file mới và `git diff --check`; không stage file để kiểm |
| RUNTIME | NOT RUN: Maven/frontend/browser/pgvector/Gemini; chỉ thay tài liệu, không lấy kết quả lịch sử làm PASS tại base này |

Rollback TASK-01 là loại bỏ đúng hai file mới sau quyết định của user; không reset develop đã pull, không xóa nhánh cũ, không chỉnh launcher được bảo vệ. Gate còn thiếu được bàn giao để owner review; không biến thành approval ngầm khi kiểm tra tài liệu PASS.

## 6. Receipt kiểm chứng tài liệu ngày 04/11/2026

- **PASS:** Python stdlib chạy inline qua `python -`, không tạo validator/file output trong repo: xác nhận HEAD/origin/develop/base, branch, protected SHA-256, chỉ hai file untracked đúng allowlist và index không có staged changes.
- **PASS:** cả 4 fenced JSON parse bằng `json.loads`; kiểm request bounds, result fields, UUID/version/hotel của citation và GROUNDED_ANSWER phải có citation.
- **PASS:** 2 relative document links resolve; 17 đường dẫn source được liệt kê tồn tại và được xác nhận tại base bằng `git cat-file -e <base>:<path>`; G01–G10 đều có mặt. Link tài liệu provider bên ngoài không được fetch lại trong TASK-01, không dùng làm bằng chứng model/account khả dụng.
- **PASS:** UTF-8 không có replacement character; hai file có newline cuối và không trailing whitespace. `git diff --check` exit 0; vì tài liệu mới chưa staged, whitespace của chúng được kiểm riêng bằng Python, không dựa riêng vào git diff.
- **PASS:** `git status --short` sau tạo/kiểm tra chỉ gồm launcher dirty có sẵn và hai tài liệu mới; không stage/commit/push, không source/config/schema delta do TASK-01.
- **NOT RUN:** Maven, frontend lint/build, API/browser, migration/backup/restore, pgvector và Gemini live. Hai tài liệu không thay executable behavior; runtime và semantic quality tại base mới chưa được chứng nhận.

Kết luận: **TASK-01 documentation acceptance PASS; contract Gemini vẫn PROPOSED, owner approvals vẫn PENDING/UNKNOWN theo mục 2.** Không tuyên bố PLAN_REVIEW/TASK_REVIEW bởi reviewer độc lập hoặc human peer approval.
