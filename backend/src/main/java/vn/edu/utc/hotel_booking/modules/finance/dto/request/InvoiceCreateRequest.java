package vn.edu.utc.hotel_booking.modules.finance.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoiceCreateRequest {

    @NotNull(message = "ID đơn đặt phòng không được để trống")
    Long bookingId;

    @Builder.Default
    BigDecimal serviceFeeRate = BigDecimal.ZERO;
}
