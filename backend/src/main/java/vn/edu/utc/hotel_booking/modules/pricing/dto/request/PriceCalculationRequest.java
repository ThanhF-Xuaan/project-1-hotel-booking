package vn.edu.utc.hotel_booking.modules.pricing.dto.request;

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
public class PriceCalculationRequest {

    @NotNull(message = "ID cấu hình loại phòng không được để trống")
    @Schema(description = "ID cấu hình loại phòng", example = "1")
    Integer hotelRoomTypeId;

    @NotNull(message = "Ngày nhận phòng không được để trống")
    @Schema(description = "Ngày nhận phòng (Check-in)", example = "2026-10-01")
    LocalDate checkInDate;

    @NotNull(message = "Ngày trả phòng không được để trống")
    @Schema(description = "Ngày trả phòng (Check-out)", example = "2026-10-04")
    LocalDate checkOutDate;

    @Min(value = 1, message = "Số người lớn tối thiểu là 1")
    @Schema(description = "Số người lớn", example = "2")
    @Builder.Default
    Short adults = 2;

    @Min(value = 0, message = "Số trẻ em tối thiểu là 0")
    @Schema(description = "Số trẻ em", example = "0")
    @Builder.Default
    Short children = 0;

    @Min(value = 0, message = "Số giường phụ tối thiểu là 0")
    @Schema(description = "Số giường phụ yêu cầu", example = "0")
    @Builder.Default
    Short extraBeds = 0;

    @Schema(description = "Mã voucher người dùng nhập/chọn (nếu có)", example = "NEWYEAR10")
    String voucherCode;

    @Schema(description = "Danh sách dịch vụ add-on đi kèm")
    java.util.List<SelectedServiceRequest> selectedServices;
}
