# SAFE-01 — Deterministic local safety v1

Decision: USER_DELEGATED_LOCAL,31/10/2026; user yêu cầu tự chọn phương án và hoàn thiện feature. Chỉ synthetic/public corpus thuộc local owner decision đó. Không human peer sign-off, không sửa chữ ký/checkbox/gold lịch sử, không live Gemini quality hoặc production approval.

Chốt trước run:32case trong rag-safety-cases.jsonl, SHA256 `df744d01fabd19923ef53faa2ed16e2de3577093f62817bbe3ca6726b7a245bc`. Gate100%critical security và100%fixed fact/citation assertions;0prohibited business writes. Không đổi cases/answers/threshold sau fail. Model local-multilingual-hashing-v1/256, chunking paragraph-v1, synthetic corpus safe-local-v1, asOf31/10/2026. Đây là deterministic security/grounding checks, không semantic groundedness score cho mô hình sinh tự do.

Runner RagSafetyEvaluationSuiteTest đọc file này, stage/activate corpus riêng từng case, kiểm status/fact/citation/scope/sanitized output và knowledge-store method trace; chỉ read sau setup. Timeout scenario kiểm controller HTTP503+fallback marker thay vì đổi envelope hiện có. Existing RagIndependentDegradationTest và architecture tests kiểm R1 độc lập/no business write. Test double cho malformed/poisoned store là fault injection, không chứng minh deployed DB/IAM.

Chạy từ backend: `.\mvnw.cmd '-Dtest=RagSafetyEvaluationSuiteTest' test`; sau fixes chạy related Rag/Advisor suites và full acceptance. Không paid/external model/data thật. Metric/test failures phải giữ nguyên log; unresolved security failure chặn release local.
