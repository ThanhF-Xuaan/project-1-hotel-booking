# agent-log.md — Nhật ký làm việc với agent (Member 1)

Mỗi task một dòng. Dùng để hiệu chỉnh ước tính và quyết định nâng/hạ mức tự chủ.
Đừng cam kết mốc rút ngắn thời gian trước khi có dữ liệu của ít nhất 2–3 task.

| Task | Ngày | Ước tính (ngày) | Thực tế (ngày) | Số lần phải can thiệp | Lỗi lặp lại | Quota dùng | Mức tự chủ | Ghi chú |
|---|---|---|---|---|---|---|---|---|
| T04 (Utility) | 2026-10-04 | 2.5d | 1.0d | 2 | Thiếu kiểm tra reading ngày liền sau; hardcode staff ID fallback | Bình thường | L3 | Hoàn tất CRUD UtilityMeter & Reading, 109 tests passed |

## Quy tắc nâng/hạ mức tự chủ
- Nâng một mức cho một loại việc: sau 3 task cùng loại liên tiếp không phải sửa lớn.
- Hạ về L1 ngay: agent sửa ngoài phạm vi, lộ dữ liệu cá nhân, tự ý đổi API của người khác.

## Họp rà soát hằng tuần (30 phút)
- [ ] Lỗi agent lặp lại → thêm vào AGENTS.member1.md mục 11 hoặc chạy `/learn`
- [ ] Quy trình dùng ≥ 3 lần → đóng gói skill
- [ ] So sánh ước tính với thực tế
- [ ] Xem lại mức tự chủ từng loại việc
