# Hệ thống agent-first cho Member 1 (AI & Data Specialist)

Dự án: Hệ thống Quản lý Khách sạn AI v4.0. Căn cứ: Kế hoạch Phân công 4 Fresher v4.0.
Phạm vi: 6 task của Member 1, tổng 18.0 ngày công.

| Giai đoạn | Task | Ngày |
|---|---|---|
| MVP | T04 Utility & Master Data BE | 2.5 |
| MVP | T05 Lodging & Payroll BE | 2.5 |
| MVP | T12 RAG Chatbot AI Core | 4.0 |
| Cải tiến 1 | U13 AI OCR Passport/CCCD | 3.0 |
| Cải tiến 1 | U14 AI Guest Personalization | 3.0 |
| Cải tiến 2 | U22 AI Predictive & Anomaly | 3.0 |

---

## 0. Hệ thống trong một đoạn

Dùng khung 5 tầng **ĐỊNH – CHIA – CHỨNG – CHẶN – CHẮT**, chỉnh theo bốn đặc thù của Member 1:

1. **Hai chế độ việc khác nhau.** T04, T05 là backend tất định (kiểm chứng bằng test). T12, U13, U14, U22 là AI phi tất định (kiểm chứng bằng bộ đánh giá, không chỉ bằng test pass).
2. **Dữ liệu cá nhân nhạy cảm.** CCCD/hộ chiếu, StayGuest, lịch sử lưu trú, dữ liệu khai báo lưu trú.
3. **Phụ thuộc vào người khác.** T04/T05 cần entities của T01 (Member 2). T12 cần API của T07, T08 (Member 2) và Keycloak của T02.
4. **Quy trình nhóm cố định.** Branch theo mã task, ≥1 peer review, chỉ Member 2 merge vào main/develop, DoD của phiếu.

Quy tắc xương sống: **không có bộ kiểm chứng thì chưa được `/goal`.** Với backend, bộ kiểm chứng là test. Với AI, đó là eval set do người duyệt.

---

## 1. Bản đồ task → đường đi

Mức tự chủ: L1 giám sát chặt, L2 duyệt từng bước, L3 `/goal` trong sandbox sau khi duyệt plan, L4 nhiều agent song song.

| Task | Chế độ | Đường đi | Tự chủ | Cổng chứng chính | Điều kiện bắt đầu |
|---|---|---|---|---|---|
| T04 | A: BE | `/plan` → `/goal` | L3 | Test CRUD + Swagger đồng bộ | T01 (entities) đã merge |
| T05 Payroll import | A: BE | `/plan` → `/goal` | L3 | Test parse + file mẫu lỗi dòng | T01 đã merge |
| T05 Lodging export | A: BE + PII | `/grill-me` → `/plan` → `/goal` | L2 | File xuất khớp biểu mẫu chính thức | Có tài liệu biểu mẫu gốc |
| T12 | B: AI | `/grill-me` → `/plan` → `/goal` theo lát | L2 → L3 | Eval set + test tool-calling | Qua Ready-gate (mục 4) |
| U13 | B: AI + PII | `/grill-me` → `/plan` → `/goal` | L2 | Eval trên ảnh mẫu tổng hợp | Chốt mô hình OCR, có ảnh mẫu giả |
| U14 | B: AI + PII | `/grill-me` → `/plan` → `/goal` | L2 | Eval offline + kiểm tra riêng tư | Dữ liệu ẩn danh hoặc tổng hợp |
| U22 | B: AI | `/plan` → `/goal` | L3 (pipeline), L2 (kết luận) | Backtest vượt baseline | Nguồn dữ liệu chốt với Member 2 |

Nâng mức tự chủ: sau 3 task cùng loại liên tiếp không phải sửa lớn. Hạ về L1 ngay sau sự cố (agent sửa ngoài phạm vi, lộ dữ liệu cá nhân, tự ý đổi API của người khác).

---

## 2. Vai trò agent

| Giai đoạn | Agent | Quyền |
|---|---|---|
| Khám phá | Subagent `research` có sẵn | Chỉ đọc |
| Làm rõ | Agent chính + `/grill-me` | Chỉ hỏi |
| Lập kế hoạch | Agent chính + `/plan` | Chỉ đọc + tạo plan |
| Thực thi | Agent chính + `/goal` | Ghi trong phạm vi plan, sandbox |
| Rà soát | `plan-reviewer` (custom) | Chỉ đọc |
| Đo AI | `rag-evaluator` (custom) | Chạy eval, không sửa bộ chuẩn |
| Dữ liệu cá nhân | `pii-auditor` (custom) | Chỉ đọc |

Ba file custom được cài vào `.agents/agents/` của repo (FILE 1–3 trong kit). Subagent không thấy lịch sử hội thoại, nên mỗi lần giao việc phải đưa đường dẫn plan, tiêu chí và ranh giới.

Song song: bắt đầu với **tối đa 2 agent** (quota dùng chung giữa app, CLI, SDK; theo dõi bằng `/usage`). Chỉ song song khi không chạm cùng file, dùng worktree riêng.

---

## 3. Lộ trình và nhịp làm việc

### Giai đoạn 0: trong lúc chờ T01 (contract-first)
- Dùng `research` khảo sát phiếu và Swagger hiện có. Soạn OpenAPI nháp cho T04/T05, dữ liệu mẫu tổng hợp, danh sách câu hỏi cho `/grill-me`.
- Bắt đầu soạn eval set nháp cho T12 từ tài liệu chính sách/mô tả phòng. Agent soạn nháp, người duyệt đáp án chuẩn.
- Chưa tạo entity hay migration.

### Sprint 1–2 (MVP)
1. Sau khi T01 merge: chạy **T04 ∥ T05** trong hai worktree riêng. Hai PR riêng.
2. Chạy Ready-gate T12 (mục 4). Qua gate thì bắt đầu T12, theo lát (mục 5).
3. T12 chia nhiều PR nhỏ theo lát, không gom một PR cuối.

### Cải tiến 1
U13 trước. Trong lúc chờ review U13, chuẩn bị dữ liệu cho U14 (không chạy hai việc có PII song song khi chưa quen).

### Cải tiến 2
U22. Chốt nguồn dữ liệu với Member 2 (U21) ngay từ đầu sprint.

### Nhịp một ngày
| Giờ | Việc |
|---|---|
| Đầu ngày (30–45 phút) | Chọn việc. Kiểm tra bốn mục ĐỊNH (đích, tiêu chí xong, ranh giới, lối thoát). Mơ hồ thì `/grill-me`. Phát agent. |
| Trong lúc agent chạy | Việc của riêng bạn: duyệt eval set, đọc tài liệu biểu mẫu, review PR của người khác. |
| Hai khung duyệt cố định (giữa và cuối ngày) | Đọc artifact, log test/eval. Duyệt, từ chối hoặc comment. |
| Cuối ngày (15 phút) | Mở PR nháp, ghi `agent-log.md`, `/learn` nếu có bài học. |

---

## 4. Ready-gate cho T12 (trước khi bắt đầu RAG Chatbot)

Phiếu nói T12 bắt đầu "khi BE APIs đã sẵn sàng". Biến câu đó thành checklist đo được:

- [ ] Swagger đã công bố và ổn định cho: tìm phòng/tồn phòng, hạng phòng và rate plan, đặt phòng FIT kèm hold 15 phút.
- [ ] Luồng token Keycloak cho role GUEST dùng được ở môi trường dev.
- [ ] Có dữ liệu seed: ít nhất vài property, hạng phòng, rate plan, chính sách.
- [ ] Đã chốt qua `/grill-me`: Spring AI hay LangChain (Python), vector store, mô hình, nơi chạy, cách triển khai.
- [ ] Eval set v0 đã được người duyệt đáp án.
- [ ] Đã thống nhất với Member 4 về UI chatbot (xem mục 9, điểm 2).

Thiếu mục nào, làm "pre-T12": ingestion, eval set, mock server dựng từ Swagger. Đừng bắt đầu `/goal` trên API chưa ổn định.

---

## 5. Playbook từng task

Công thức mỗi `/goal`: **đích + tiêu chí xong đo được + ranh giới + lối thoát khi kẹt.** Điền `[ngoặc vuông]` theo repo thật.

### T04: Utility & Master Data BE
```
/plan Triển khai T04: CRUD Region, Property, RoomType; UtilityMeter, UtilityReading.
Đọc entities của T01 tại [đường dẫn]. Mỗi bước nêu file sẽ đổi + lệnh test.
Không tạo migration mới nếu plan chưa nêu và chưa thống nhất với Member 2.
```
```
/goal Hoàn thành T04 đúng plan đã duyệt.
Xong khi: [lệnh test] pass 100%, lint/compile sạch, Swagger cập nhật đồng bộ.
Ranh giới: chỉ sửa file trong plan; không sửa entities của T01.
Kẹt: cùng một lỗi lặp 3 lần thì dừng và báo cáo.
```

### T05: Lodging & Payroll BE
```
/grill-me T05: Lodging Queue, thu thập StayGuest, xuất file khai báo, API import Payroll Summary.
Tôi sẽ đính kèm biểu mẫu khai báo chính thức: [đường dẫn].
Hỏi tôi về: trạng thái hàng chờ, xử lý trùng/thiếu dữ liệu, ai được xem dữ liệu định danh,
thời gian lưu trữ và xóa, định dạng file payroll và cách báo lỗi theo dòng.
```
Sau đó `/plan` và `/goal` như T04. Tách hai PR: Payroll import (L3) và Lodging export (L2). Không để agent suy đoán định dạng khai báo: nó phải bám tài liệu gốc bạn cung cấp.

### T12: RAG Chatbot (chia lát, mỗi lát một PR)
| Lát | Nội dung | Tự chủ | Cổng chứng |
|---|---|---|---|
| S1 Ingest | Chia đoạn, embedding, lưu vector store (chính sách, mô tả phòng) | L3 | Test ingest + đếm tài liệu |
| S2 Retrieve | Truy xuất, đo trên eval set | L3 | hit@k đạt ngưỡng |
| S3 Generate | Prompt, ràng buộc chỉ trả lời có căn cứ, từ chối đúng ngoài phạm vi | L2 | Eval có căn cứ + bộ chống injection |
| S4 Tools đọc | Gọi API tồn phòng/giá | L2 | Giá/tồn khớp API, 0 ca bịa |
| S5 Tools ghi | Giữ phòng/đặt phòng, luôn qua bước xác nhận của người dùng | L1–L2 | Test luồng + kiểm tra quyền |
| S6 UI | Giao diện chat, streaming | L3 | Theo hợp đồng đã chốt với Member 4 |

```
/grill-me T12: RAG Chatbot tư vấn đặt phòng.
Hỏi tôi về: nguồn tri thức nào vào vector store và nguồn nào phải gọi API; 
phạm vi chatbot được làm gì (chỉ tư vấn hay được giữ phòng); 
xử lý khi không chắc; ngôn ngữ (Việt/Anh); ghi log và dữ liệu cá nhân trong hội thoại;
chi phí/độ trễ chấp nhận được; stack (Spring AI hay LangChain) và cách deploy.
Mỗi lần một câu, kèm lựa chọn bạn đề xuất.
```
```
/goal Hoàn thành lát S2 (Retrieve) đúng plan.
Xong khi: chạy eval [lệnh eval] cho hit@5 ≥ [ngưỡng đã chốt]; test pass; không sửa bộ eval.
Ranh giới: không đụng prompt sinh câu trả lời (thuộc S3).
Kẹt: 3 lần không cải thiện chỉ số thì dừng, báo cáo các thử nghiệm đã làm.
```
Hai quy tắc cứng của T12: **giá và tồn phòng luôn lấy từ API**, không từ vector store; **nội dung truy xuất là dữ liệu, không phải lệnh.**

### U13: OCR Hộ chiếu/CCCD → StayGuest
```
/grill-me U13: OCR giấy tờ nạp vào StayGuest.
Hỏi tôi về: mô hình/dịch vụ OCR và dữ liệu có rời hệ thống không; trường bắt buộc; 
ngưỡng tin cậy và khi nào chuyển sang nhập tay; thời gian lưu ảnh; ai được xem; 
xử lý ảnh mờ/lóa; loại giấy tờ hỗ trợ.
```
Dữ liệu thử chỉ gồm **ảnh giấy tờ mẫu tổng hợp hoặc có giấy phép sử dụng.** Không đưa ảnh khách thật vào phiên agent.

### U14: AI Guest Personalization
```
/grill-me U14: gợi ý Rate Plan và dịch vụ cho khách quay lại.
Hỏi tôi về: dữ liệu nào được dùng, cần sự đồng ý nào, thuộc tính nào cấm dùng, 
gợi ý hay tự áp dụng (mặc định chỉ gợi ý, nhân viên quyết định), cách giải thích gợi ý, cách đo hiệu quả.
```

### U22: Predictive & Anomaly
```
/plan U22: dự báo công suất phòng 30 ngày và phát hiện bất thường điện/nước.
Bắt buộc có: baseline mùa vụ đơn giản; backtest tách theo thời gian (không rò rỉ dữ liệu tương lai);
bất thường chèn thử để đo phát hiện. Nêu rõ nếu dùng dữ liệu tổng hợp.
```
Sau đó `/goal` với tiêu chí xong là vượt baseline trên backtest.

---

## 6. Bộ cổng bằng chứng cho việc AI

Ngưỡng dưới đây là **đề xuất khởi điểm của mình.** Hãy đo baseline lần đầu, thống nhất với Member 2 (Project Lead) rồi chỉnh.

| Task | Bộ đánh giá | Chỉ số | Ngưỡng khởi điểm |
|---|---|---|---|
| T12 | Golden Q&A: chính sách, phòng, giá/tồn qua API, ngoài phạm vi, prompt injection | hit@5; tỷ lệ trả lời có căn cứ; ca bịa giá/tồn; tool-call đúng | hit@5 ≥ 0.85; có căn cứ ≥ 0.95; bịa giá/tồn = 0; injection pass 100% |
| U13 | Ảnh giấy tờ mẫu có nhãn chuẩn | Khớp chính xác từng trường; tỷ lệ ca độ tin cậy thấp được chuyển nhập tay | Trường bắt buộc ≥ 0.95; chuyển nhập tay đúng 100% |
| U14 | Lịch sử ẩn danh/tổng hợp, tách train/test | precision@k; không dùng thuộc tính cấm | Vượt baseline "gợi ý phổ biến nhất"; 0 vi phạm thuộc tính cấm |
| U22 | Backtest tách thời gian; bất thường chèn thử | MAPE; precision/recall phát hiện | Tốt hơn baseline mùa vụ đơn giản; ngưỡng phát hiện chốt với Member 2 |

Nguyên tắc đánh giá:
- **Agent soạn nháp bộ eval, người duyệt đáp án chuẩn.** Agent không tự chấm bằng đáp án do chính nó viết.
- Ưu tiên kiểm tra tất định (khớp với API, khớp trường) trước; dùng mô hình làm giám khảo chỉ cho phần mềm khó đo, và ghi rõ trong báo cáo.
- Cố định phiên bản prompt, mô hình, embedding, tham số khi chạy eval. Ghi vào báo cáo.
- Dữ liệu tổng hợp thì ghi rõ trong báo cáo và không kết luận chất lượng "ngoài đời".

---

## 7. Dữ liệu cá nhân và an toàn

- Không đưa dữ liệu khách thật (CCCD/hộ chiếu, tên, số điện thoại, email, lịch sử lưu trú) vào prompt, file đính kèm, log, fixture hoặc repo. Dùng dữ liệu tổng hợp/ẩn danh.
- Không đưa secret vào repo; agent không đọc `.env`.
- Che dữ liệu cá nhân trong log. Chạy `pii-auditor` trước mỗi PR của T05, U13, U14 và T12.
- Chatbot: token theo role GUEST, quyền tối thiểu. Thao tác ghi (giữ phòng/đặt phòng) luôn qua xác nhận rõ ràng của người dùng.
- Không bật chế độ bỏ qua quyền. Dùng `request-review` hoặc `proceed-in-sandbox`.
- Trước T05, U13, U14: hỏi người phụ trách pháp lý/tuân thủ của dự án về quy định bảo vệ dữ liệu cá nhân hiện hành và chính sách lưu trữ. Mình không phải tư vấn pháp lý.

---

## 8. Git, PR và Definition of Done

Theo phiếu:
- Branch: `feature/<mã-task>-<mô-tả-ngắn>`, ví dụ `feature/T04-utility-master-data`, `feature/T12-rag-chatbot`.
- PR cần ít nhất 1 peer review. **Chỉ Member 2 merge vào main/develop.**

Hệ quả cho agent:
- Agent chỉ commit trên feature branch của bạn. Không push, không merge vào main/develop. Bạn là người mở PR.
- AI reviewer (`plan-reviewer`) là lớp bổ sung. **Không thay thế peer review của người.**
- Migration và thay đổi schema: thống nhất với Member 2 trước (đề xuất cách đánh số version riêng để tránh xung đột Flyway).

Checklist PR:
- [ ] Lint/compile không lỗi (DoD phiếu)
- [ ] 100% unit test pass (DoD phiếu)
- [ ] Swagger đồng bộ (DoD phiếu)
- [ ] Không hồi quy (DoD phiếu)
- [ ] Việc AI: đính kèm báo cáo eval (chỉ số, bộ dữ liệu, phiên bản mô hình/prompt)
- [ ] Việc có dữ liệu cá nhân: `pii-auditor` sạch
- [ ] T12: ghi chú chi phí và độ trễ mỗi lượt hội thoại
- [ ] Mô tả PR nêu thay đổi ngoài plan (nếu có) và lý do

---

## 9. Điểm cần xác nhận trong phiếu

1. **Bảng workload ghi sai số task.** MVP của Member 1 ghi "4 tasks" nhưng phiếu chỉ giao 3 (T04 + T05 + T12 = 9.0 ngày). Số ngày đúng, số task sai. Bảng Member 4 cũng lệch: T03 + T10 + T11 = 8.5 ngày, bảng ghi 11.5 ngày (4 tasks).
2. **T12 gồm "UI Chatbot" nhưng stack của Member 1 không có React.** Cần thống nhất với Member 4: Member 4 dựng widget chat theo hợp đồng (ví dụ endpoint streaming) hay Member 1 dựng bản tối thiểu.
3. **Stack RAG chưa chốt** (Spring AI hay LangChain). Chốt ở `/grill-me` của T12, vì ảnh hưởng triển khai và CI.
4. **Biểu mẫu khai báo lưu trú (T05)** cần tài liệu gốc. Không để agent đoán.
5. **Phụ thuộc T01/Swagger.** Nếu T01 trễ, T04/T05/T12 trượt theo. Theo dõi sát từ Sprint 1.
6. **Ngưỡng eval ở mục 6 là đề xuất**, chưa phải chuẩn của dự án.
7. Lệnh và đường dẫn cấu hình dựa trên tài liệu Antigravity đã tra. Kiểm tra lại với phiên bản đang dùng.

---

## 10. Theo dõi và học lại

Tạo `agent-log.md`, mỗi task một dòng:

| Task | Ước tính | Thực tế | Số lần phải can thiệp | Lỗi lặp lại | Quota dùng |
|---|---|---|---|---|---|

Mỗi tuần 30 phút:
- Lỗi agent lặp lại → thêm vào `AGENTS.member1.md` hoặc chạy `/learn`.
- Quy trình dùng ≥ 3 lần → đóng gói skill.
- So sánh ước tính với thực tế để hiệu chỉnh. Đừng cam kết mốc rút ngắn trước khi có dữ liệu của ít nhất 2–3 task.
- Xem lại mức tự chủ từng loại việc theo quy tắc ở mục 1.
