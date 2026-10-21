package vn.edu.utc.hotel_booking.modules.booking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingChargeType;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SurchargePreviewRequest {

    @NotNull(message = "Loại phụ phí không được để trống (EARLY_CHECKIN hoặc LATE_CHECKOUT)")
    @Schema(description = "Loại phụ phí cần xem trước đề xuất", example = "EARLY_CHECKIN")
    BookingChargeType chargeType;

    @Schema(description = "Thời điểm thực tế (mặc định là thời điểm hiện tại)", example = "2026-10-18T08:30:00+07:00")
    OffsetDateTime actualTime;
}
