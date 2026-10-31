# PERF-01 — Benchmark Member 1 local

CONFIRMED — PASS trên profile synthetic đã khóa trước khi gửi request. Base `20e06e5135def269b91531b3997b784b271e17c8`, worktree `m1-feature-final`, run `20261031-195348-650-37abd3bf`. Không phải SLA production hoặc tải 200 CCU.

Chạy từ root: `.\tools\acceptance\Verify.ps1 -IncludeM1Benchmark -IncludeM1AdditionalTests`. Supervisor gọi `Benchmark-M1.ps1` smoke 3 request, rồi lượt đầy đủ. Script tạo `method-freeze.json` trước request, SHA script `35be97966126fab6bcf2e3c6f9ecb2829a0213377adddf77e03568cd180acf15`, SHA ảnh synthetic `c496c4cff31d735f9c3a2aceaa352168004e37eac93d2cd5353c53770857fc38`. Gate chốt sẵn p95 <2.000 ms và success >=99% cho từng endpoint/concurrency/vòng; không sửa threshold hoặc bỏ lỗi.

Mỗi endpoint Advisor/FAQ/OCR có 20 warm-up và 100 mẫu đo ở concurrency 1 và 5, lặp hai vòng: 1.200 mẫu đo, 240 warm-up; smoke 3 request báo riêng. Lỗi mẫu đo: 0; lỗi warm-up: 0. Percentile nearest-rank trên toàn bộ mẫu đo, kể cả lỗi; timeout 10 giây, không retry. Latency bao gồm chờ HTTP, decode JSON, kiểm oracle và với OCR cả queue/engine/parse; không tính khoảng pacing giữa batch.

Giữ rate limiter thật: Advisor dùng session anonymous mới mỗi request, IP loopback thật, không giả forwarded IP hoặc nới limit. Concurrency 1 gửi cách tối thiểu 1,1 giây; concurrency 5 gửi batch 5 cách tối thiểu 5,5 giây. Trước lượt đầy đủ có 60 giây chờ hết window từ browser adversarial test. Đây là tải được pacing với batch song song, không phải 5 CCU bão hòa liên tục. FAQ/OCR gửi các batch liên tiếp. Các hạn chế này áp dụng cho mọi kết luận hiệu năng.

Profile: Gemini tắt, extractor deterministic; RAG in-memory/hash256 với demo corpus và policy được API/browser test tạo; OCR Tesseract `vie+eng`, tối đa 2 process và deadline/quality gốc. Không thêm tài nguyên hoặc giảm chất lượng để qua gate. Oracle Advisor yêu cầu RESULTS và 1..5 card; FAQ cần GROUNDED_ANSWER, 14:00 và citation; OCR cần draft human verification và đúng tên/số giấy tờ/ngày sinh synthetic. Không log response, raw prompt, ảnh hoặc token.

Hardware: Intel(R) Core(TM) i7-9850H CPU @ 2.60GHz, 12 logical CPU, RAM 32 GiB; Windows_NT 10.0.19045; Node v24.14.1; tesseract v5.5.3.20260724. Backend/Maven thực dùng JDK21.0.12, javac --release21, được xác nhận bằng context log và executable của PID đang listen18080. Probe `java -version` do Node gọi theo PATH trong result.json trả JDK25.0.4; đó không phải JVM backend. Evidence bổ sung actual-java-runtime.json ghi rõ hai nguồn, không sửa raw benchmark JSON để che khác biệt. PostgreSQL/Redis/Keycloak cùng máy qua Docker; không loại overhead local khỏi latency.

| Vòng | Endpoint | Đồng thời | Thành công/100 | Lỗi | p50 ms | p95 ms | p99 ms | Gate |
|---:|---|---:|---:|---:|---:|---:|---:|---|
| 1 | advisor | 1 | 100 | 0 | 70.36 | 102.64 | 119.22 | PASS |
| 1 | advisor | 5 | 100 | 0 | 73.17 | 95.47 | 100.10 | PASS |
| 1 | faq | 1 | 100 | 0 | 5.55 | 6.49 | 7.98 | PASS |
| 1 | faq | 5 | 100 | 0 | 7.93 | 9.53 | 10.36 | PASS |
| 1 | ocr | 1 | 100 | 0 | 387.92 | 417.74 | 423.07 | PASS |
| 1 | ocr | 5 | 100 | 0 | 785.15 | 1180.77 | 1197.53 | PASS |
| 2 | advisor | 1 | 100 | 0 | 67.99 | 101.50 | 105.62 | PASS |
| 2 | advisor | 5 | 100 | 0 | 70.31 | 97.67 | 101.90 | PASS |
| 2 | faq | 1 | 100 | 0 | 4.98 | 5.72 | 5.93 | PASS |
| 2 | faq | 5 | 100 | 0 | 7.59 | 10.51 | 11.26 | PASS |
| 2 | ocr | 1 | 100 | 0 | 379.84 | 405.99 | 411.36 | PASS |
| 2 | ocr | 5 | 100 | 0 | 781.74 | 1185.75 | 1210.23 | PASS |

DB nghiệp vụ không đổi qua benchmark: `True`; digest trước/sau bao gồm dữ liệu và sequence từ pg_dump đã bỏ nonce restrict. Redis session có thể thay đổi theo contract, không gọi đó là mọi storage đều bất biến. RAG admin setup nằm trước đo; không có business mutation trong request benchmark. Supervisor đã restore snapshot và dừng ứng dụng/container sau đo.

Evidence: `tools/acceptance/.runtime/runs/20261031-195348-650-37abd3bf/benchmark/` gồm method-freeze.json, samples.json và result.json; `benchmark-smoke/` báo riêng. Snapshot và runtime chứa cấu hình synthetic được Git ignore. Gemini thật/chi phí provider, persistent RAG HTTP, dữ liệu khách thật, load bão hòa/distributed limiter và production: NOT RUN. Không stage/commit/push/merge.
