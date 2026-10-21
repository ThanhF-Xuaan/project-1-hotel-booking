package vn.edu.utc.hotel_booking.modules.booking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Yêu cầu hủy đặt phòng")
public class BookingCancelRequest {

    @NotBlank(message = "Lý do hủy đặt phòng không được để trống")
    @Schema(description = "Lý do hủy phòng", example = "Thay đổi kế hoạch công tác")
    String reason;

    @Schema(description = "Ghi chú chi tiết thêm", example = "Khách liên hệ qua hotline xin hủy do lịch trình thay đổi")
    String note;
}
