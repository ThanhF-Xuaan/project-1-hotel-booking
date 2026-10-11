package vn.edu.utc.hotel_booking.modules.booking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomPriceQuoteRequest {

    @NotNull(message = "ID cấu hình loại phòng không được để trống")
    @Schema(description = "ID cấu hình hạng phòng", example = "1")
    Integer hotelRoomTypeId;

    @NotNull(message = "Ngày nhận phòng không được để trống")
    @Schema(description = "Ngày nhận phòng", example = "2026-10-15")
    LocalDate checkInDate;

    @NotNull(message = "Ngày trả phòng không được để trống")
    @Schema(description = "Ngày trả phòng", example = "2026-10-17")
    LocalDate checkOutDate;

    @NotNull(message = "Số người lớn không được để trống")
    @Min(value = 1, message = "Số người lớn tối thiểu là 1")
    @Schema(description = "Số người lớn", example = "3")
    Short adults;

    @Min(value = 0, message = "Số trẻ em không được âm")
    @Schema(description = "Số trẻ em (6-11 tuổi)", example = "1")
    @Builder.Default
    Short children = 0;

    @Min(value = 0, message = "Số trẻ sơ sinh không được âm")
    @Schema(description = "Số trẻ sơ sinh (0-5 tuổi)", example = "1")
    @Builder.Default
    Short infants = 0;

    @Min(value = 0, message = "Số giường phụ không được âm")
    @Schema(description = "Số giường phụ yêu cầu kê thêm", example = "1")
    @Builder.Default
    Short extraBeds = 0;

    @Schema(description = "Mã voucher giảm giá (nếu có)", example = "SUMMER2026")
    String voucherCode;
}
