package vn.edu.utc.hotel_booking.modules.booking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartPriceQuoteRequest {

    @NotNull(message = "ID khách sạn không được để trống")
    @Schema(description = "ID khách sạn", example = "1")
    Short hotelId;

    @NotNull(message = "Ngày nhận phòng không được để trống")
    @Schema(description = "Ngày nhận phòng", example = "2026-10-15")
    LocalDate checkInDate;

    @NotNull(message = "Ngày trả phòng không được để trống")
    @Schema(description = "Ngày trả phòng", example = "2026-10-17")
    LocalDate checkOutDate;

    @Schema(description = "Mã voucher giảm giá (nếu có)", example = "SUMMER2026")
    String voucherCode;

    @NotEmpty(message = "Danh sách hạng phòng trong giỏ hàng không được để trống")
    @Valid
    @Builder.Default
    @Schema(description = "Danh sách các hạng phòng cần báo giá trong giỏ hàng")
    List<CartItemQuoteRequest> items = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CartItemQuoteRequest {

        @NotNull(message = "ID cấu hình hạng phòng không được để trống")
        @Schema(description = "ID cấu hình hạng phòng", example = "1")
        Integer hotelRoomTypeId;

        @Min(value = 1, message = "Số lượng phòng tối thiểu là 1")
        @Schema(description = "Số lượng phòng của hạng này", example = "1")
        @Builder.Default
        Short quantity = 1;

        @Min(value = 1, message = "Số người lớn tối thiểu là 1")
        @Schema(description = "Số người lớn trên mỗi phòng", example = "2")
        @Builder.Default
        Short adults = 2;

        @Min(value = 0, message = "Số trẻ em không được âm")
        @Schema(description = "Số trẻ em trên mỗi phòng", example = "0")
        @Builder.Default
        Short children = 0;

        @Min(value = 0, message = "Số trẻ sơ sinh không được âm")
        @Schema(description = "Số trẻ sơ sinh trên mỗi phòng", example = "0")
        @Builder.Default
        Short infants = 0;

        @Min(value = 0, message = "Số giường phụ không được âm")
        @Schema(description = "Số giường phụ kê thêm trên mỗi phòng", example = "0")
        @Builder.Default
        Short extraBeds = 0;
    }
}
