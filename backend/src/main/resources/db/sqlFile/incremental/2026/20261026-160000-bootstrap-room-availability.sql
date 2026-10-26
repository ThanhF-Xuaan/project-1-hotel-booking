-- =========================================================================
-- Migration: Bootstrap room_availability cho 365 ngày mở bán cho mọi hạng phòng ACTIVE
-- Tuân thủ: Luật Du lịch 2017 (Điều 53, 65), Luật Bảo vệ quyền lợi người tiêu dùng 2023 (Điều 10, 38)
--           Nghị định 45/2019/NĐ-CP & Tiêu chuẩn vận hành chuỗi khách sạn quốc tế
-- =========================================================================

INSERT INTO room_availability (
    hotel_room_type_id,
    date,
    total_rooms,
    booked_rooms,
    locked_rooms,
    ooo_rooms,
    version,
    created_at,
    updated_at
)
SELECT 
    hrt.id AS hotel_room_type_id,
    d.date::date AS date,
    hrt.total_quantity AS total_rooms,
    0 AS booked_rooms,
    0 AS locked_rooms,
    0 AS ooo_rooms,
    0 AS version,
    CURRENT_TIMESTAMP AS created_at,
    CURRENT_TIMESTAMP AS updated_at
FROM hotel_room_types hrt
CROSS JOIN generate_series(
    CURRENT_DATE,
    CURRENT_DATE + INTERVAL '364 days',
    INTERVAL '1 day'
) AS d(date)
WHERE hrt.is_deleted = false AND hrt.status = 'ACTIVE'
ON CONFLICT (hotel_room_type_id, date) DO NOTHING;
