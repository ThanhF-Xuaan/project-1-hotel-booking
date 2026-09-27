package vn.edu.utc.hotel_booking.modules.booking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingChargeType;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingChargeCreateRequest {

    @NotNull(message = "Loại phụ phí không được để trống")
    BookingChargeType chargeType;

    @NotBlank(message = "Tên hạng mục không được để trống")
    String itemName;

    String description;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn hoặc bằng 1")
    @Builder.Default
    Integer quantity = 1;

    @NotNull(message = "Đơn giá không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Đơn giá không được âm")
    BigDecimal unitPrice;

    @Builder.Default
    BigDecimal serviceFeeRate = BigDecimal.ZERO;

    @Builder.Default
    BigDecimal vatRate = BigDecimal.ZERO;
}
