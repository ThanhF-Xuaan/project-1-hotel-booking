-- Migration: Add description column to pricing_rule_types and discount_rule_types and backfill master-data descriptions
-- Version: 2026-11-11

ALTER TABLE pricing_rule_types
    ADD COLUMN IF NOT EXISTS description TEXT;

ALTER TABLE discount_rule_types
    ADD COLUMN IF NOT EXISTS description TEXT;

UPDATE pricing_rule_types
SET description = 'Áp dụng phụ thu cho các dịp Lễ, Tết hoặc kỳ nghỉ dài ngày theo lịch nhà nước (mức ưu tiên cao nhất).'
WHERE code = 'HOLIDAY'
  AND (description IS NULL OR BTRIM(description) = '');

UPDATE pricing_rule_types
SET description = 'Áp dụng phụ thu cho giai đoạn mùa cao điểm du lịch, nghỉ hè hoặc lễ hội địa phương.'
WHERE code = 'PEAK_SEASON'
  AND (description IS NULL OR BTRIM(description) = '');

UPDATE pricing_rule_types
SET description = 'Áp dụng phụ thu định kỳ cho các đêm cuối tuần (Thứ 6, Thứ 7).'
WHERE code = 'WEEKEND'
  AND (description IS NULL OR BTRIM(description) = '');

UPDATE discount_rule_types
SET description = 'Ưu đãi giảm giá cho khách đặt phòng lưu trú dài ngày (ví dụ từ 3 đêm liên tiếp trở lên).'
WHERE code = 'LONG_STAY'
  AND (description IS NULL OR BTRIM(description) = '');

UPDATE discount_rule_types
SET description = 'Ưu đãi giảm giá theo chiến dịch khuyến mãi đặc biệt hoặc Flash Sale theo khung thời gian.'
WHERE code = 'SPECIAL_CAMPAIGN'
  AND (description IS NULL OR BTRIM(description) = '');

UPDATE discount_rule_types
SET description = 'Ưu đãi giảm giá cho khách đặt phòng sớm trước ngày nhận phòng (ví dụ trước 30 ngày).'
WHERE code = 'EARLY_BIRD'
  AND (description IS NULL OR BTRIM(description) = '');

UPDATE discount_rule_types
SET description = 'Ưu đãi giảm giá giờ chót nhằm tối ưu công suất phòng trống sát ngày nhận phòng.'
WHERE code = 'LAST_MINUTE'
  AND (description IS NULL OR BTRIM(description) = '');

