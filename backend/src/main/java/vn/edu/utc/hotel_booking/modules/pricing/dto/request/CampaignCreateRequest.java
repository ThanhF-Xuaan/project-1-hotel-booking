package vn.edu.utc.hotel_booking.modules.pricing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CampaignCreateRequest {

    @NotNull(message = "ID khách sạn không được để trống")
    @Schema(description = "ID khách sạn", example = "1")
    Short hotelId;

    @NotBlank(message = "Tên chiến dịch không được để trống")
    @Size(max = 150, message = "Tên chiến dịch tối đa 150 ký tự")
    @Schema(description = "Tên chiến dịch khuyến mại", example = "Chào hè sôi động 2026")
    String name;

    @Schema(description = "Mô tả chi tiết chiến dịch", example = "Giảm giá 15% cho đặt phòng sớm")
    String description;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    @Schema(description = "Ngày bắt đầu chiến dịch", example = "2026-06-01")
    LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    @Schema(description = "Ngày kết thúc chiến dịch", example = "2026-08-31")
    LocalDate endDate;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    @Builder.Default
    String status = "ACTIVE";
}
