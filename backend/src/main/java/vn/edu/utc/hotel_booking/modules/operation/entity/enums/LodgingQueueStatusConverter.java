package vn.edu.utc.hotel_booking.modules.operation.entity.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converter đồng bộ trạng thái giữa Java Enum và DB Check Constraint.
 * Trong DB constraint: chk_lodging_queue_status cho phép ('PENDING', 'PROCESSING', 'EXPORTED', 'FAILED', 'CANCELLED').
 * Java Enum có 'ERROR' đại diện cho việc thiếu thông tin chuẩn BCA.
 * Converter đảm bảo khi lưu xuống DB, ERROR được chuyển đổi an toàn thành 'FAILED',
 * và khi đọc lên từ DB, 'FAILED' được chuyển đổi về LodgingQueueStatus.ERROR để giữ tương thích với API/FE.
 */
@Converter(autoApply = true)
public class LodgingQueueStatusConverter implements AttributeConverter<LodgingQueueStatus, String> {

    @Override
    public String convertToDatabaseColumn(LodgingQueueStatus attribute) {
        if (attribute == null) {
            return null;
        }
        if (attribute == LodgingQueueStatus.ERROR) {
            return "FAILED";
        }
        return attribute.name();
    }

    @Override
    public LodgingQueueStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        if ("FAILED".equalsIgnoreCase(dbData.trim())) {
            return LodgingQueueStatus.ERROR;
        }
        try {
            return LodgingQueueStatus.valueOf(dbData.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return LodgingQueueStatus.ERROR;
        }
    }
}
