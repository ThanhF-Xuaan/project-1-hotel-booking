# T12-02 — Gemini opt-in an toàn

- Authorization: user cho phép tiếp tục các task phù hợp của Member 1, tự quyết lựa chọn triển khai và sửa phần liên quan. Không giả sign-off của member khác.
- Owner: Member 1. Base mới fetch: `origin/develop@20e06e5135def269b91531b3997b784b271e17c8`.
- Worktree riêng: `C:/Users/admin/.codex/worktrees/m1-t12-provider/project-1-hotel-booking`, detached HEAD tại base trên; sạch trước task. Worktree widget có hai tài liệu đang sửa được bảo vệ bằng hash/status.
- Contract: T12-02 trong kế hoạch feature-completion; `t12-rag-widget-v1.md` ghi rõ key không được tự bật provider. Không đổi API/DTO/session/Core reads hoặc gold/eval thresholds.

## Evidence và khoảng thiếu

CONFIRMED: `HybridRoomAdvisorLlmService.extractCriteriaPatch` hiện tự gọi Gemini khi process có `GEMINI_API_KEY`; timeout request 4 giây, chưa giới hạn output token, key nằm trong query URL, catch log nội dung exception. `Run-Application.ps1` chỉ xóa Gemini key kế thừa. Existing deterministic constructor và ba test extraction phải tiếp tục chạy offline.

CONFIRMED: code hiện dùng JDK 21 HttpClient trực tiếp, không có Spring AI dependency trong pom. Giữ adapter hiện có, không thêm dependency hoặc thay model. Endpoint/model `gemini-2.5-flash` được giữ từ baseline; khả dụng thực tế của model và live-provider quality không được nghiệm thu trong task này.

## Acceptance và allowlist

1. Thêm `AdvisorProviderProperties.java` tại package advisor/llm: prefix `hotel.ai.advisor.provider`, `gemini-enabled=false`, timeout mặc định 4000 ms, khoảng 100–4000 ms; output mặc định 512 token, khoảng 1–1024. Spring bind/validate cấu hình. Chỉ khi flag true, key không trống và bounds hợp lệ mới có outbound.
2. Sửa `HybridRoomAdvisorLlmService.java`: constructor injection, transport/key supplier có thể thay bằng test double; no-arg constructor giữ chế độ offline. Giới hạn toàn bộ future HTTP theo timeout, hủy khi timeout/interruption, giữ interrupt flag. Giữ endpoint cố định/no redirects, key chỉ trong header, không retry. Bounded response body 64 KiB; chỉ nhận JSON candidate hoàn tất, lỗi thì deterministic fallback. Redaction trước provider, không log exception/prompt/key/body.
3. Mở rộng `HybridRoomAdvisorLlmServiceTest.java`: ma trận flag/key/config, transport không được gọi khi tắt; cấu hình/binding; payload/token cap/header/redaction; JSON/date parsing; malformed/error/timeout/interrupt và body overflow. Không dùng key thật hoặc gọi network ngoài.
4. Sửa `tools/acceptance/Run-Application.ps1`: force flag false, xóa cả GEMINI_API_KEY và GOOGLE_API_KEY khỏi process trước chạy app. Kiểm parser và thực thi riêng block này với sentinel env; không launch sandbox.
5. Thêm báo cáo `docs/member1/acceptance/t12-02-provider-2026-10-30.md`; append plan/handoff canonical khi có kết quả thật. Không sửa tài liệu OCR/U14 đang dirty ở worktree khác.

## Verification, rollback và giới hạn

- Targeted Maven `HybridRoomAdvisorLlmServiceTest`, sau đó các suite Advisor/RAG không cần external services; không bật Redis/Keycloak integration flags trong lượt offline.
- PowerShell parse và block sandbox-env test; current-source artifact validator `--reviewed-widget-sources`, tooling tests nếu cần xác nhận provenance không đổi.
- Giữ nguyên hash contract/gold/schema; `git diff --check`, allowlist và protected-worktree checks.
- Không gọi Gemini thật, không dùng dữ liệu thật, không Docker/migration. Không thay provider/model dựa trên phỏng đoán.
- Rollback bằng tắt opt-in; có thể hoàn nguyên riêng patch sau review. Không tự reset worktree hoặc thay file người dùng.
- Không commit/push/PR/merge trong slice này; bàn giao patch và kết quả local để review. Không gọi thiếu live-provider evidence là lỗi local khi mọi test transport dùng synthetic.
