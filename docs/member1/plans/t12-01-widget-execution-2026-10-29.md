# T12-01 — contract và widget FAQ

Baseline2958ad43c9ed63bd8985b54787918e9bf776713c, origin/develop freshly verified. User đồng ý chuyển T12-01 theo đề xuất; quyền sửa cross-owner đã được user cấp, không giả member khác sign-off. OCR-02 đã xong, không sửa tiếp OCR.

Scope: giữ frozen R1 route/schema/gold. Thêm mode FAQ riêng trên widget, không dùng LLM đoán intent. Dropdown public hotels hiện có, chain FAQ khi không chọn; backend kiểm hotel ACTIVE/not deleted qua narrow read interface, không đưa repository/write capability vào RAG. Không hotelCode unresolved hoặc topK>5; asOfDate giữ contract ISO/date hiện có, omission vẫn service clock. Exception generic503, không raw question/exception logs. FAQ errors/citations state độc lập R1; abort/epoch và xóa state khi scope đổi; citations hiển thị text với scope check, không HTML/link thực thi.

Allowlist: RAG controller/queryDTO/controller test; narrow PublicHotelEligibilityReader và adapter+test trong organization; frontend widget,new HotelKnowledgePanel,knowledge service/types, browser tests/fixture; docs contract/plan/report. Không migration/provider/pgvector/cookie/CORS/role hoặc giá/tồn/business writes. G2 local decisions theo quyền user: mode explicit, maxK5, ACTIVE !deleted eligibility theo public booking code, giữ service date policy, generic503 giữ enum INSUFFICIENT_EVIDENCE vì HTTP503 phân biệt outage.

Verification: targeted Advisor/Rag và reader tests, frozen contract verifier, frontend build/lint, browser stale/scope/citations/outage/R1 independence; real runtime synthetic sandbox nếu sẵn. No real identity/prompt/key/corpus. Rollback removeFAQ mode/revert scoped changes; R1 giữ nguyên. Handoff kết quả truthfully; không gọi model thật.
