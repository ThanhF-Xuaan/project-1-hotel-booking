package vn.edu.utc.hotel_booking.modules.pricing.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelAgePolicyResponse {

    @Schema(description = "ID chính sách độ tuổi", example = "1")
    Short id;

    @Schema(description = "ID khách sạn (null nghĩa là Quy chuẩn toàn chuỗi)", example = "1")
    Short hotelId;

    @Schema(description = "Tên khách sạn cơ sở", example = "SmartStay Central Hotel")
    String hotelName;

    @Schema(description = "Loại khách (INFANT, CHILD, ADULT)", example = "CHILD")
    String guestType;

    @Schema(description = "Độ tuổi tối thiểu", example = "6")
    Short minAge;

    @Schema(description = "Độ tuổi tối đa", example = "11")
    Short maxAge;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;

    @Schema(description = "Thời điểm tạo")
    OffsetDateTime createdAt;

    @Schema(description = "Cờ đánh dấu quy chuẩn toàn chuỗi", example = "false")
    Boolean isChainDefault;
}

