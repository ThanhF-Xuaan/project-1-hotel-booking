# Member 1 — Persistent RAG runtime và mock V03

**Verdict: PASS local/synthetic cho persistent RAG full HTTP/browser và bộ mock V03.** User cho phép implement/review/fix/commit/push/PR/merge; sau đó xác nhận tạo mock trước, dữ liệu thật sẽ gửi sau. Không ghi tự review thành human peer approval hoặc đổi gate dữ liệu thật thành PASS.

Base `0b81d319c27004746cc938a6c56fe1ac9ea9a21b`; branch `codex/m1-persistent-rag-runtime-20261031`; worktree ban đầu sạch. [Plan và allowlist](../plans/m1-persistent-rag-http-2026-10-31.md). Production Java, frontend, schema, DTO/API/IAM, validator và frozen gold không đổi.

## Thay đổi và review

- `Run-Application.ps1` nhận mode persistent đã được kiểm tra; ghép profile `rag-pgvector-local` với `ocr-local`, giữ default in-memory. Chỉ truyền credential RAG sang backend. JVM persistent dùng timezone chuẩn `Asia/Ho_Chi_Minh`.
- `Rag-Runtime.ps1` từ chối mode/URL/credential không hợp lệ, query/fragment/newline, foreign DB và demo+persistent. Có 15 kiểm tra cấu hình; GitHub CI chạy lại trên Linux.
- `Verify.ps1 -IncludePersistentRag` thêm bước full Boot với Keycloak thật, PostgreSQL/pgvector trên container/volume mới ở loopback15435. Writer không có quyền admin/CREATE/TEMP hoặc SELECT bảng ngoài RAG. Admin DB và writer dùng mật khẩu sinh ngẫu nhiên độc lập; không ghi credential/token vào báo cáo.
- `Test-PersistentRag.ps1` và `verify-persistent-rag.mjs` kiểm SQL thực sau lifecycle HTTP, restart JVM, DB outage/recovery, R1 còn verified cards và browser hiển thị citation từ persistent source. Snapshot riêng, dừng app trước restore, kiểm digest dữ liệu/sequence và khôi phục runtime config nguyên byte. Hash bốn file thực chạy được khóa tại đầu pha persistent và kiểm lại cuối pha.
- Theo steering mới: generator mock deterministic, chống ghi đè, khóa nguồn theo hash và bỏ oracle. [Hướng dẫn mock](m1-v03-mock-data-guide-2026-10-31.md) có cấu trúc file và lệnh chạy.

Lượt đầu `20261031-213913-654-3333b5f4` FAIL tại admin stage sau9 kiểm tra: JVM Windows gửi timezone `Asia/Saigon` mà image pgvector không có alias. Backend và bộ mặc định vẫn PASS; RAG/main data và config restore PASS. Fix đúng launcher persistent theo contract đã có; giữ nguyên assertion, không đổi database image hoặc nguồn dữ liệu để né lỗi. [Report FAIL được giữ](runs/acceptance-20261031-213913-654-3333b5f4.md).

Lượt sau `20261031-214855-835-8e10eb24` PASS; [report supervisor](runs/acceptance-20261031-214855-835-8e10eb24.md). Review bổ sung siết mật khẩu admin/writer độc lập và kiểm quyền role trước khi pha persistent chạy. Hash source của lượt này được ghi riêng; các thay đổi review cuối được kiểm trong lượt focused bên dưới.

Review cuối bổ sung xóa mật khẩu bootstrap khỏi environment trước khi khởi chạy app, kèm guard ngăn kế thừa giá trị đó. Lượt tập trung đầu phát hiện race: `pg_isready` qua Unix socket nhận nhầm server tạm của quá trình init PostgreSQL; SQL gặp server đang shutdown. Đã chuyển readiness sang TCP để chờ server chính. Lượt `focused-final-v2` trên đúng source cuối PASS lại cả 74 kiểm tra persistent; hash bốn file runtime khớp bản bàn giao. Main snapshot được restore và query hậu kiểm xác nhận0 bảng/0 sequence public, không còn app/container đang chạy. Lượt FAIL và volume riêng được giữ; không gọi lượt init lỗi trước snapshot RAG là đã restore RAG.

| Kiểm tra thực chạy | Kết quả |
|---|---|
| Full backend: clean test rồi context smoke riêng | 1.399 test tổng hợp,0 skip; PASS ở hai lượt |
| U14 PostgreSQL bổ sung, source Java giữ nguyên | 6/6 PASS ở lượt đầu, DB tạm đã xóa; lượt sau không chạy lặp |
| API/browser mặc định | 25 API +27 browser PASS,0 skip ở hai lượt |
| Persistent lifecycle HTTP/SQL/Keycloak | 50 kiểm tra PASS: deny anonymous/customer, stage/activate, scope1/2, staged visibility, replacement, active-delete rejection, rollback/delete |
| Restart JVM | 9 kiểm tra PASS, giữ policy/citation và active versions trong SQL |
| DB outage | 4 kiểm tra PASS: knowledge503, không citation bịa, R1 trả verified cards từ Core |
| Recovery và browser | 11 kiểm tra PASS: policy/citation phục hồi, widget hiển thị đúng nguồn persistent |
| Source cuối sau review credential/readiness | Chạy riêng lại cả 4 pha/74 kiểm tra PASS tại focused-final-v2; không chạy lặp backend/API/browser mặc định vì Java/frontend không đổi |
| Config, parser, workflow syntax | 15 config  kiểm tra PASS; PowerShell parser, Node check, actionlint1.7.12 và diff check PASS |
| Mock exporter | 6/6 PASS; 24file trong ZIP, 840 occupancy rows,516 utility readings/12scenario,7Group scenario,1lodging draft |
| Fixture algorithms | 14/14 PASS; nằm trong full backend, không cộng lặp |
| Contract/source audit, lint/build, high-critical audit | PASS trong supervisor; còn12moderate npm tooling advisories |

`application-suites` có exit1 theo contract cũ của child `Test.ps1 -SkipMaven`: child là PARTIAL vì Maven được chạy riêng. Supervisor kiểm đúng danh sách suite và yêu cầu tất cả sáu suite còn lại PASS; không bỏ qua failure. Tổng74 kiểm tra persistent là các assertion HTTP/SQL/browser của bốn pha, không phải74case eval T12 frozen.

## Phạm vi kết luận và vận hành

Chạy lại bằng PowerShell7 trên sandbox rảnh, đã có Docker/dependency và cấu hình synthetic hợp lệ:

```powershell
pwsh -NoProfile -File tools/acceptance/Verify.ps1 -IncludeM1AdditionalTests -IncludePersistentRag
```

Model runtime là `local-multilingual-hashing-v1`, không gọi Gemini/embedding trả phí. Dedicated RAG schema dùng DDL đã review trên DB mới; primary business datasource giữ riêng. Các snapshot/credential/log chi tiết ở `.runtime` hoặc outputs ngoài Git; container/volume synthetic được giữ và dừng sau kiểm chứng. Restore chứng minh dữ liệu/sequence và config, không tuyên bố reset toàn bộ physical state/ACL của server.

Mock V03 sẵn sàng theo yêu cầu mới. U22/Group mock chạy qua service/fixture tests; adapter/API/UI business dùng dữ liệu thật chưa được nối. Historical capacity/stays, utility unit/day/reset/epoch, Group physical beds/floors/all-night quota/IAM và mẫu BCA chính thức tiếp tục PENDING_INPUT. Bộ lodging mock chỉ là human-review draft phi chính thức. OCR-02 giữ COMPLETE/MERGED; không công bố toàn SRS/production hoàn tất.

Evidence ngoài Git: `C:/Users/admin/.codex/outputs/member1-persistent-rag-http-2026-10-31/`. CI cuối/PR/merge và hậu kiểm được ghi trong receipt và canonical handoff sau delivery. Rollback bằng revert scoped tooling qua PR; default production không bị đổi, không cần rollback schema production. Không reset/stash/force-push hoặc sửa worktree của người khác.
