package vn.edu.utc.hotel_booking.modules.operation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.LodgingGuestCategory;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LodgingExportRequest {

    @NotNull(message = "hotelId không được để trống")
    @Schema(description = "ID khách sạn cơ sở", example = "1")
    Short hotelId;

    @NotNull(message = "Ngày chốt xuất báo cáo không được để trống")
    @Schema(description = "Ngày báo cáo", example = "2026-10-04")
    LocalDate date;

    @Schema(description = "Giờ chốt dữ liệu (mặc định 23h00)", example = "23")
    @Builder.Default
    Integer cutOffHour = 23;

    @NotNull(message = "Phân loại khách không được để trống (VIETNAMESE hoặc FOREIGNER)")
    @Schema(description = "Phân loại khách xuất báo cáo", example = "VIETNAMESE")
    @Builder.Default
    LodgingGuestCategory category = LodgingGuestCategory.VIETNAMESE;
}
