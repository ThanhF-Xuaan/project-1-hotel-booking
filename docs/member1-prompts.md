# Prompt dùng hằng ngày — Member 1

Cách dùng: Prompt 2 cho mỗi task mới (làm rõ → plan → thực thi). Prompt 3 khi kết thúc task.
Prompt cài đặt kit (một lần) nằm ở Phần 0 của file member1-agent-kit.md.
Thay các chỗ trong `[ngoặc vuông]` trước khi dán.

---

## Prompt 2 — Bắt đầu một task

### 2a. Làm rõ (dán trước)

```
/grill-me Bắt đầu task [T04 | T05 | T12 | U13 | U14 | U22] của Member 1.
Đọc: AGENTS.member1.md và phần tương ứng trong docs/member1-agent-system.md
(bản đồ task, playbook, và Ready-gate nếu là T12).
Nội dung task theo phiếu: [dán dòng task từ phiếu phân công].

Làm theo thứ tự:
1. Kiểm tra "điều kiện bắt đầu" của task. Nếu chưa đủ: nêu cái còn thiếu, đề xuất việc làm trước
   (contract-first hoặc pre-T12), rồi dừng.
2. Kiểm tra 4 mục: đích, tiêu chí xong đo được, ranh giới, lối thoát.
   Thiếu mục nào thì hỏi tôi, mỗi lần một câu, kèm lựa chọn bạn đề xuất.
3. Với task AI (T12, U13, U14, U22): nếu chưa có eval set được tôi duyệt, soạn NHÁP eval set vào eval/
   (dữ liệu tổng hợp, không dùng dữ liệu thật) và chờ tôi duyệt đáp án chuẩn.
   Không viết mã gọi/chạy mô hình trước khi eval set được duyệt.
4. Khi đủ thông tin: tóm tắt các quyết định đã chốt thành danh sách, rồi dừng.
```

### 2b. Lập kế hoạch (sau khi chốt quyết định)

```
/plan Dựa trên các quyết định đã chốt, lập kế hoạch cho [mã task].
Mỗi bước nêu: file sẽ đổi, lệnh kiểm chứng, rủi ro.
Task AI: chia thành các lát, mỗi lát một PR và có chỉ số eval riêng.
```

Đọc kỹ plan, comment trực tiếp lên artifact, chỉ duyệt khi mỗi bước có lệnh kiểm chứng đo được.

### 2c. Thực thi (sau khi duyệt plan)

```
/goal Thực hiện đúng plan đã duyệt cho [mã task] [lát nếu có].
Xong khi: [lệnh test] pass 100%; lint/compile sạch; Swagger đồng bộ;
[task AI: eval đạt ngưỡng đã chốt và bộ eval không bị sửa].
Ranh giới: chỉ sửa file trong plan; tuân thủ AGENTS.member1.md mục 4 đến 7.
Kẹt: lỗi lặp 3 lần hoặc chỉ số không cải thiện sau 3 thử nghiệm thì dừng và báo cáo.
```

---

## Prompt 3 — Kết thúc task (rà soát và chuẩn bị PR)

```
Task [mã task] đã thực thi xong. Chuẩn bị đóng task. KHÔNG commit, push hay merge.

1. Gọi subagent plan-reviewer với: đường dẫn plan, mã task, danh sách file đã đổi.
2. Nếu task có dữ liệu cá nhân (T05, U13, U14, T12): gọi pii-auditor.
3. Nếu task AI: gọi rag-evaluator với lệnh eval, bộ eval và ngưỡng đã chốt.
4. Tổng hợp một báo cáo: findings theo mức (chặn / nên sửa / gợi ý), bảng chỉ số,
   bằng chứng DoD (lint, test, Swagger, không hồi quy).
5. Soạn mô tả PR theo checklist ở mục 8 của docs/member1-agent-system.md.
   Ghi rõ mọi thay đổi ngoài plan.
6. Điền một dòng vào agent-log.md: ước tính, thực tế, số lần can thiệp, lỗi lặp lại, quota.
7. Đề xuất các mục nên thêm vào AGENTS.member1.md mục 11 (chạy /learn nếu phù hợp).
   Không tự sửa các mục khác.

Báo cáo của bạn bổ sung cho peer review của người, không thay thế.
```
