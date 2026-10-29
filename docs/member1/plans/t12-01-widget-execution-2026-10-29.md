# T12-01 — contract và widget FAQ

Baseline2958ad43c9ed63bd8985b54787918e9bf776713c, origin/develop freshly verified. User đồng ý chuyển T12-01 theo đề xuất; quyền sửa cross-owner đã được user cấp, không giả member khác sign-off. OCR-02 đã xong, không sửa tiếp OCR.

Scope: giữ frozen R1 route/schema/gold. Thêm mode FAQ riêng trên widget, không dùng LLM đoán intent. Dropdown public hotels hiện có, chain FAQ khi không chọn; backend kiểm hotel ACTIVE/not deleted qua narrow read interface, không đưa repository/write capability vào RAG. Không hotelCode unresolved hoặc topK>5; asOfDate giữ contract ISO/date hiện có, omission vẫn service clock. Exception generic503, không raw question/exception logs. FAQ errors/citations state độc lập R1; abort/epoch và xóa state khi scope đổi; citations hiển thị text với scope check, không HTML/link thực thi.

Allowlist: RAG controller/queryDTO/controller test; narrow PublicHotelEligibilityReader và adapter+test trong organization; frontend widget,new HotelKnowledgePanel,knowledge service/types, browser tests/fixture; docs contract/plan/report. Không migration/provider/pgvector/cookie/CORS/role hoặc giá/tồn/business writes. G2 local decisions theo quyền user: mode explicit, maxK5, ACTIVE !deleted eligibility theo public booking code, giữ service date policy, generic503 giữ enum INSUFFICIENT_EVIDENCE vì HTTP503 phân biệt outage.

Verification: targeted Advisor/Rag và reader tests, frozen contract verifier, frontend build/lint, browser stale/scope/citations/outage/R1 independence; real runtime synthetic sandbox nếu sẵn. No real identity/prompt/key/corpus. Rollback removeFAQ mode/revert scoped changes; R1 giữ nguyên. Handoff kết quả truthfully; không gọi model thật.

## Bổ sung: kiểm tra nguồn và luồng đọc ngày 29/10

CONFIRMED tại HEAD31276ac: getAvailability có thể gọi generation khi thiếu ngày, trái invariant T12. User đã cho phép sửa cross-owner; không ghi nhận approval của member khác. Mở allowlist hẹp cho RoomAvailabilityServiceImpl.java, RoomAvailabilityServiceTest.java và runtime test T12: bỏ generation khỏi nhánh đọc, giữ generation trong các lệnh ghi, xác nhận sparse defaults và số phòng đoàn. Viết test thất bại trước sửa rồi chạy regression Inventory/Advisor/Rag. Không nhập dirty diff ở D:.

Kiểm tra source evidence theo Git provenance bằng audit bổ sung và verifier/test riêng trong tools/t12, docs/member1. Giữ nguyên manifest lịch sử, schema, examples, eval/gold và thresholds. Mọi ngoại lệ hash phải có prior/current commit, hash và patch đã đối chiếu; không cho bỏ qua file hoặc lệch nội dung hiện tại. Kết quả chỉ là artifact validation, không thay thế model eval hoặc production release. Rollback revert commit bổ sung; không chạy migration.

## Bổ sung: tách fixture audit lịch sử

Preflight: branch codex/m1-t12-widget, HEAD53af8cf01555156f3407e9b4bfaab7bacfaf6c58 sạch; origin/develop vẫn2958ad4. User tiếp tục cho phép triển khai. CONFIRMED: Core sources ở commit96d7815 khớp manifest; cả3nguồn/patch U14 ở commite827013 khớp audit cũ. Các unit test lịch sử đang đọc nhầm working tree mới.

Allowlist: tools/t12/verify_core_reaudit.py (chỉ thêm đầu vào patch để test verifier thuần, default CLI giữ nguyên), test_core_reaudit.py, test_portability_validation.py; docs plan/report và append canonical handoff. Không sửa application source, frozen contract/eval/manifest, approval, schema hoặc security config.

AC: các test lịch sử chạy trên Git fixture đúng snapshot, giữ nguyên assertions chống tampering; thêm kiểm tra từ chối source/patch giả và default current-tree drift. Test portability dùng source96d7815 cho hash lịch sử và chạy full validator bằng explicit reviewed-widget-sources cho snapshot hiện tại. Chạy toàn bộ unittest tools/t12 và current artifact validator. Không cần chạy lại Maven/browser/build khi application không đổi. Rollback revert đúng commit tooling/docs. Git publish chỉ scoped feature branch, không import vào repoD đang dirty.

Bổ sung verification gap: chạy riêng4RedisRoomAdvisorSessionStoreRealRedisTest từng skip bằng Redis7 tạm mới bind127.0.0.1:16380, xác minh instance độc quyền/DB rỗng trước khi bật T12_REDIS_IT và exclusive gate. Không dùng Redis16379 đang giữ sandbox, không FLUSHDB/FLUSHALL. Suite chỉ cleanup advisor:* và sentinel của nó. Không sửa Java/test để lấy PASS; dừng instance sau test.
