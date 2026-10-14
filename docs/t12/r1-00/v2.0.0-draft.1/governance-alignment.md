# Đối chiếu file mới với R1-00

## Nguồn đã review

Đã đọc AGENTS/Member 1 rules và skills cập nhật, agent review specs, [ADR](../../../T12-room-advisor-ADR.md), [contract tracker](../../../T12-contract-requests.md), [roadmap](../../../member1-agent-system.md), [task prompts](../../../member1-prompts.md) và [evaluation v2 draft](../../../../eval/eval-set-t12-room-recommendation-v2-draft.md). Các file này vẫn thuộc thay đổi của người dùng, chưa được R1-00 sửa hoặc đánh dấu approved.

Không cần đổi kiến trúc tổng thể. Có bốn cập nhật cụ thể cho bộ draft foundation:

| Phát hiện | Cập nhật R1-00 |
|---|---|
| Quy tắc mới yêu cầu phân biệt user/inferred/default và ambiguity | Thêm CriteriaExtractionProposal, CriteriaEvidence, criteriaProvenance/ambiguities trong schema và ví dụ. Java xác minh provenance; không nhận confirmation/scope từ client. |
| CR-10 nhắc card mang stay và quote basis | Thêm stay trên card, validate nhất quán với criteria và totalNights. Không chứa reservation/hold/payment/customer ID. |
| Draft v2 có public eligibility, location ambiguity, missing route, stale snapshot và trace-level security | Bổ sung 18 case, tăng 56 thành 74; [coverage mapping](../../../../eval/t12/r1/v0.1.0-draft/governance-coverage.json) đối chiếu 35 yêu cầu R1. Cases R2 036–042 được ghi ngoài scope. |
| Contract tracker có CR IDs và gates theo owner | Decision register map M2/M4 decisions sang CR-01..06/10; CR-07 và CR-08/09 ghi deferred. Tài liệu review không nhân đôi approval hoặc coi draft là API hiện hữu. |

56 assertions cũ của bộ R1-00 được giữ nguyên trong continuation; các case mới là DRAFT/PENDING. Legacy eval v1.1 và draft v2 do người dùng thêm được giữ nguyên SHA256. Không đổi đáp án human-approved hoặc numeric thresholds để đạt kết quả.

## Phạm vi được phép tiếp tục

Người dùng đã xác nhận nguồn các thay đổi worktree và yêu cầu tiếp tục R1-00. Vì vậy task tiếp tục trong bốn cây file riêng: docs/t12/r1-00, backend/src/test/resources/contracts/t12/r1, eval/t12/r1 và tools/t12. Các file governance/skills/ADR/tracker/eval bên ngoài được bảo vệ bằng digest, không chỉnh để giải quyết blockers.

Public API, money/date semantics, ownership/cookies/TTL, card route và revalidation vẫn PENDING Member 2/4. Provider approval và Gemini/Spring AI compatibility thuộc task sau; RAG/pgvector thuộc Release 2. Không tạo dependency giả để block development bằng synthetic fixtures; không dùng fixtures như production facts.

## Đánh giá coverage

Mapping chỉ chứng minh đã thiết kế case, không chứng minh runtime hành vi đúng. Các assertions PII/no-write cần spies, architecture rules và HTTP traces khi runner/engine được phép triển khai. Accuracy, graded ranking relevance, p95 latency, tokens và cost đều NOT RUN. Read-only agent specs là hướng dẫn review; task này không tự spawn agent hoặc gọi model.

## Compatibility và rollback

Chưa có consumer hoặc runtime endpoint T12 trên baseline để migration. Contract mới là draft độc lập, không thay wire Core APIs. Trước integration phải ký version/schema/error/session/route semantics với M2/4; thay breaking contract sau freeze tạo version mới và consumer tests.

Rollback R1-00 chỉ là loại khỏi PR đúng các file mới trong change set sau khi được cấp phép; không reset/clean toàn worktree, không đụng 23 file bảo vệ hoặc các thay đổi của người dùng. Task không stage/commit/push/merge hoặc triển khai R1-01/R1-02.
