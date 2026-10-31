# Member 1 — Tích hợp và nghiệm thu local ngày 31/10/2026

**Verdict: PASS cho technical acceptance local/synthetic; feature tích hợp dữ liệu thật còn NOT READY.** Hoàn thiện 15/16 task local theo plan; V03 đã đạt full technical verification nhưng phần business integration còn thiếu nguồn dữ liệu. OCR-02 đã COMPLETE/MERGED từ PR40, không mở lại task đó. Không tuyên bố toàn SRS hoặc production hoàn tất.

## Những việc đã làm

Tạo managed worktree riêng `C:/Users/admin/.codex/worktrees/m1-feature-final/project-1-hotel-booking` từ origin/develop vừa fetch tại `20e06e5135def269b91531b3997b784b271e17c8`, detached HEAD. Nhập đúng byte 69 file đã kiểm chứng từ T12-pgvector (19), U22-anomaly (33) và Group-preview (17). Giữ nguyên source gốc và dirty/staged work tại repo D; user đã cho phép tự quyết và sửa phần liên quan, không giả review của member khác.

Các patch được ghép gồm Gemini opt-in mặc định tắt; pgvector datasource riêng/lifecycle opt-in; occupancy history và forecast/backtest; causal utility anomaly; Group preview read-only. Các thuật toán U22/Group hiện là module/port đã test, chưa có provider/API/UI business thật. Không dùng synthetic facts làm dữ liệu production.

SAFE-01 thêm bộ 32 case đã khóa trước run và sửa guard/scope/date/metadata/topK, citation, injection/live availability, source conflicts. Full runtime phát hiện thêm câu hỏi giờ nhận phòng dùng nhầm bằng chứng bữa sáng/hủy phòng; thêm 5 test và lọc evidence đúng chủ đề. Giữ nguyên 32 case, gold cũ và assertion API khi sửa. Xem safe-01-local-2026-10-31.md.

PERF-01 thêm benchmark lặp hai vòng với oracle và raw timing metadata; V02 giữ nguyên T04/T05 vì regression đạt; V03 sửa các runner để chạy đủ test OCR/Keycloak và hai U14 PostgreSQL suite trên DB trống riêng, bật các runtime browser test thay vì skip. Source auditors đối chiếu các delta đã review tại commit3f0cb2e với historical audit, không sửa manifest/gold. Bổ sung tamper tests. Runtime volume-prefix tách sandbox mới; OCR opt-in và offline provider flags được chốt rõ.

Chỉ cập nhật transitive follow-redirects1.16.1→1.16.2 trong lockfile để xử lý [GHSA-mrwp-f3q8-3g9p](https://github.com/advisories/GHSA-mrwp-f3q8-3g9p); không force major upgrade. Audit production còn0 cảnh báo; audit toàn bộ không còn high/critical, còn12 moderate liên quan tooling ESLint/minimatch. Không coi lint/build là bằng chứng đã sửa các moderate đó.

## Bằng chứng cuối

| Kiểm tra thực chạy | Kết quả |
|---|---|
| Maven clean test rồi application-context smoke riêng | 1.385/1.385 PASS, 0 skip trong backend.json tổng hợp |
| U14 PostgreSQL opt-in, 2 database trống riêng | 6/6 PASS, database tạm đã xóa |
| pgvector opt-in trên container riêng, final Java source | 11/11 PASS; restore dữ liệu/sequence và dừng container |
| Tổng Surefire của các lệnh riêng | 1.402 test, 178 suite; 0 failure/error/skip; không gọi là một lệnh Maven |
| RAG targeted cuối | 83/83 PASS, gồm32 frozen và5 policy-time mới |
| Tooling unittest discover | 49/49 PASS |
| Contract validator reviewed-widget-sources | PASS: 184 instances,74 cases,23 protected hashes,33 negative checks |
| Frontend lint/build | PASS,0 lint error/warning |
| Playwright API/browser thật và mock được ghi theo từng case | 25/25 API +27/27 browser PASS,0 skip |
| T04/T05 subset | 126 backend +7 API PASS; không cộng lặp vào tổng |
| Benchmark | 1.200 mẫu đo +240 warm-up; smoke3 riêng; 0 lỗi đo; mọi cell đạt gate |
| Verify supervisor | PASS; snapshot restored; app và container acceptance đã dừng |

Lệnh chính: `.\tools\acceptance\Verify.ps1 -IncludeM1Benchmark -IncludeM1AdditionalTests`; lệnh pgvector riêng `.\mvnw.cmd -Dtest=PostgresPgVectorStoreIT -DfailIfNoTests=true test`; tooling `python -m unittest discover -s tools/t12 -p 'test*.py'`. Từng command/exit/count được giữ trong logs và JSON. Source Java được khóa tại java-final-source-freeze.json trước lượt Verify cuối; kiểm lại trước bàn giao. Head/base không thay đổi, dirty tree được định danh thêm bằng delivery manifest; SHA base không đại diện cho các patch chưa commit.

Run cuối `20261031-195348-650-37abd3bf` tại tools/acceptance/.runtime/runs/. Output bổ sung, patch và raw-byte ZIP ở `C:/Users/admin/.codex/outputs/member1-final-2026-10-31/`. Delivery manifest liệt kê từng file và SHA; Git patch dùng canonical LF, ZIP giữ byte Windows thực đã test. Chỉ kiểm apply/reverse trong Git repo tạm, không apply vào repo D hoặc source được bảo vệ.

Các lượt FAIL được giữ: ba lỗi setup detached HEAD; source audit cũ chưa nhận delta đã merge; API 24PASS/1FAIL do policy relevance trước fix. Sau sửa chạy lại full Verify với1.385 backend/25API/27browser và benchmark. Không đổi trạng thái lịch sử thành PASS, không gộp các lần chạy thành một lần. Bộ source audit được sửa trong lượt trước; run cuối ghi hash script ổn định từ đầu.

Run10:05 bị ngắt khi benchmark mới lưu100 mẫu Advisor đầu tiên: INCOMPLETE, không PASS. Khi user tiếp tục, Docker và tiến trình Verify đã dừng; đã kiểm hash snapshot, đúng volume m1-final, không có app/DB connection khác, restore về0bảng/0sequence và dừng sandbox trước lượt mới. Evidence interrupted-recovery.json và logs riêng được giữ. Không ghép mẫu benchmark trước/sau gián đoạn.

## Task và điều kiện còn thiếu

15 task local đã hoàn thiện: A00,C01,OCR-01,OCR-02,T12-01,T12-02,T12-03,U14-01,U22-01,U22-02,GIT-01,U22-03,SAFE-01,PERF-01,V02. **V03: technical PASS, business integration NOT READY.** Đây là thiếu dữ liệu/contract vận hành, không phải chờ member khác cấp quyền cho user.

| Điều kiện | Bằng chứng còn thiếu / việc để đóng gate |
|---|---|
| Occupancy forecast | Capacity hiệu lực từng ngày và actual stays/completeness; reconciliation source/version/coverage, adapter authorization/IAM và API/UI trên dữ liệu đó. Không backfill từ current room count. |
| Utility anomaly | Đơn vị vật lý, mốc ngày đọc, reset/replacement/counter epoch thật; normalized read adapter và IAM/API/UI. Không suy đơn vị chỉ từ ELECTRICITY/WATER hoặc chia usage qua gap. |
| Group preview | Nguồn physical beds/floors, quota/availability mọi đêm và scope/IAM; adapter và UI preview thật. Không hold/apply/guest write hoặc rooming-list tự động. |
| T05 định dạng BCA | Mẫu/đặc tả chính thức để đối chiếu. Workbook hiện tại chỉ được kiểm kỹ thuật; chưa gửi ra hệ thống bên ngoài. |

NOT RUN: Gemini/embedding provider thật, production, khách thật, persistent RAG qua full HTTP/browser và tải200CCU. PG adapter được test thật nhưng main Boot acceptance dùng in-memory; không đánh tráo hai profile. Kiểm tra32case deterministic không chứng minh semantic quality cho mọi câu hỏi hoặc miễn nhiễm mọi prompt injection. Warning startup S3 localhost9000 thuộc module storage baseline, không nằm trong hạ tầng acceptance Member1 này; không tự khởi động/đổi storage để che warning.

## Privacy, dữ liệu, rollback và Git

Chỉ fixture synthetic; OCR vẫn cần nhân viên xem/sửa/xác nhận trước lưu. Multipart `ocr-local` opt-in giữ in-memory/cap và có thể áp cap upload toàn process; cần giữ giới hạn này khi đánh giá rollout. Không đổi default production profile, Core booking/pricing/inventory/auth hoặc migrate production. Snapshot main tạo trước master-data migration trên volume mới m1-final-20261031-*; PGvector có backup riêng và restore so sánh dữ liệu/sequence. Volume cũ không bị xóa. Snapshot/runtime/token không vào patch/ZIP; không log raw request/identity/token trong benchmark.

Chưa stage/commit/push/PR/merge. Có thể review toàn bộ kết quả trong worktree final; các source worktree cũ và repo D giữ nguyên. Rollback ứng dụng bằng không áp dụng delta của worktree final, giữ provider/PG opt-in tắt; không reset/clean/stash công việc của người khác. Handoff/plan canonical chỉ append có kiểm prefix byte. Chữ ký peer review, corpus production và dữ liệu lịch sử thật không bị giả lập bằng quyền tự quyết.

## File thay đổi

Danh sách cumulative gồm69file nhập và phần SAFE/PERF/V03; các report lịch sử nhập nguyên giữ nguyên ngữ cảnh task nguồn. Delivery manifest/patch là nguồn đầy đủ cho hash và diff:

- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/advisor/llm/AdvisorProviderProperties.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/advisor/llm/HybridRoomAdvisorLlmService.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/forecast/dto/OccupancyForecastResponse.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/forecast/dto/UtilityAnomalyResponse.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/forecast/model/DailyOccupancySample.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/forecast/model/OccupancyHistoryBatch.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/forecast/model/OccupancyHistoryResult.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/forecast/model/UtilityReadingSample.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/forecast/port/OccupancyHistoryAuthorizationPort.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/forecast/port/OccupancyHistoryPort.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/forecast/port/UtilityReadingAuthorizationPort.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/forecast/port/UtilityReadingHistoryPort.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/forecast/service/ForecastBacktestEvaluator.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/forecast/service/OccupancyForecastService.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/forecast/service/OccupancyHistoryService.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/forecast/service/OccupancyModels.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/forecast/service/UtilityAnomalyDetectionService.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/groupassignment/dto/GroupRoomAssignmentPreviewRequest.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/groupassignment/model/GroupRoomDemand.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/groupassignment/model/RoomAssignmentCandidate.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/groupassignment/model/RoomAssignmentPreview.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/groupassignment/port/GroupAssignmentAuthorizationPort.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/groupassignment/port/GroupAssignmentDataPort.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/groupassignment/service/DeterministicGroupRoomAssignmentEngine.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/groupassignment/service/GroupAssignmentValidation.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/groupassignment/service/GroupRoomAssignmentPreviewService.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/rag/config/RagVectorStoreConfiguration.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/rag/corpus/DefaultPublicHotelCorpusInitializer.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/rag/governance/RagCorpusGovernanceValidator.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/rag/service/RagGroundedAnswerService.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/rag/service/RagKnowledgeLifecycleService.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/rag/service/ScopedRagRetrievalService.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/rag/store/InMemoryScopedVectorStore.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/rag/store/PostgresPgVectorStore.java`
- `backend/src/main/resources/application-rag-pgvector-local.yaml`
- `backend/src/main/resources/db/changelogs/incremental/2026/20261030-230000-create-rag-pgvector-store.xml`
- `backend/src/main/resources/db/sqlFile/incremental/2026/20261030-230000-create-rag-pgvector-store.sql`
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/ai/advisor/llm/HybridRoomAdvisorLlmServiceTest.java`
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/ai/forecast/DailyOccupancySampleTest.java`
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/ai/forecast/ForecastBacktestEvaluatorTest.java`
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/ai/forecast/OccupancyForecastFixtureTest.java`
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/ai/forecast/OccupancyForecastServiceTest.java`
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/ai/forecast/OccupancyHistoryFixtureTest.java`
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/ai/forecast/OccupancyHistoryServiceTest.java`
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/ai/forecast/OccupancyReadOnlyBoundaryTest.java`
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/ai/forecast/UtilityAnomalyDetectionServiceTest.java`
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/ai/forecast/UtilityAnomalyFixtureTest.java`
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/ai/forecast/UtilityAnomalyReadOnlyBoundaryTest.java`
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/ai/groupassignment/DeterministicGroupRoomAssignmentEngineTest.java`
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/ai/groupassignment/GroupPreviewFixtureTest.java`
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/ai/groupassignment/GroupPreviewReadOnlyBoundaryTest.java`
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/ai/groupassignment/GroupPreviewTestData.java`
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/ai/groupassignment/GroupRoomAssignmentPreviewServiceTest.java`
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/ai/rag/PostgresPgVectorStoreIT.java`
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/ai/rag/RagPersistentBoundaryTest.java`
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/ai/rag/RagPolicyTimeEvidenceTest.java`
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/ai/rag/RagSafetyEvaluationSuiteTest.java`
- `backend/src/test/resources/ai/group-assignment/preview-cases.json`
- `backend/src/test/resources/ai/u22/occupancy-history-420-days.json`
- `backend/src/test/resources/ai/u22/utility-readings-42-days.csv`
- `docs/member1/acceptance/README.md`
- `docs/member1/acceptance/git-01-preview-2026-10-31.md`
- `docs/member1/acceptance/m1-feature-completion-report-2026-10-31.md`
- `docs/member1/acceptance/m1-latency-benchmark.md`
- `docs/member1/acceptance/runs/acceptance-20261031-094622-582-a3b4b531.md`
- `docs/member1/acceptance/runs/acceptance-20261031-094635-184-dc0c5f6e.md`
- `docs/member1/acceptance/runs/acceptance-20261031-094646-331-4afba07b.md`
- `docs/member1/acceptance/runs/acceptance-20261031-094658-784-061ffb9c.md`
- `docs/member1/acceptance/runs/acceptance-20261031-095432-202-ba385d21.md`
- `docs/member1/acceptance/runs/acceptance-20261031-195348-650-37abd3bf.md`
- `docs/member1/acceptance/safe-01-local-2026-10-31.md`
- `docs/member1/acceptance/t12-02-provider-2026-10-30.md`
- `docs/member1/acceptance/t12-03-pgvector-2026-10-30.md`
- `docs/member1/acceptance/u22-01-history-2026-10-30.md`
- `docs/member1/acceptance/u22-02-forecast-2026-10-31.md`
- `docs/member1/acceptance/u22-03-anomaly-2026-10-31.md`
- `docs/member1/acceptance/v02-t04-t05-regression-2026-10-31.md`
- `docs/member1/contracts/group-room-assignment-preview-local-v1.md`
- `docs/member1/contracts/t12-pgvector-local-v1.md`
- `docs/member1/contracts/u22-analytics-local-v1.md`
- `docs/member1/contracts/u22-forecast-local-v1.md`
- `docs/member1/contracts/u22-utility-anomaly-local-v1.md`
- `docs/member1/plans/m1-final-execution-2026-10-31.md`
- `docs/member1/plans/t12-02-provider-execution-2026-10-30.md`
- `eval/t12/r2/.gitattributes`
- `eval/t12/r2/README.md`
- `eval/t12/r2/rag-safety-cases.jsonl`
- `frontend/e2e/browser/ocr-full-runtime.spec.ts`
- `frontend/e2e/browser/t12-knowledge-runtime.spec.ts`
- `frontend/package-lock.json`
- `tools/acceptance/Benchmark-M1.ps1`
- `tools/acceptance/Run-Application.ps1`
- `tools/acceptance/Run-BackendTests.ps1`
- `tools/acceptance/Run-M1AdditionalTests.ps1`
- `tools/acceptance/Runtime.ps1`
- `tools/acceptance/Test.ps1`
- `tools/acceptance/Verify.ps1`
- `tools/acceptance/benchmark-m1.mjs`
- `tools/t12/reconcile_widget_search.cjs`
- `tools/t12/test_current_core_reaudit.py`
- `tools/t12/verify_current_core_reaudit.py`
- `tools/t12/verify_lazy_route_reaudit.cjs`
- `tools/t12/verify_u14_source_reaudit.cjs`

Handoff canonical cuối: H042; prefix handoff/plan và contract C01 kiểm hash giữ nguyên. Hậu kiểm post-restore-state.json:0publictable/0sequence/0temporaryU14DB, app vàcontainer acceptance đãdừng.

Ba report setup lỗi sớm ghi branch rỗng tạo trailing whitespace. Bản gốc được lưu nguyên byte tại preserved-early-reports ngoài Git; bản review ghi not captured before setup failure. Trạng thái FAIL giữ nguyên; không bỏ kiểm whitespace để đóng gói.
