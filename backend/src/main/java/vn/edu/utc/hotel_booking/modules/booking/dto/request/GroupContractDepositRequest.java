package vn.edu.utc.hotel_booking.modules.booking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupContractDepositRequest {

    @NotNull(message = "Số tiền đặt cọc không được để trống")
    @DecimalMin(value = "0.01", message = "Số tiền đặt cọc phải lớn hơn 0")
    BigDecimal amount;

    Integer paymentMethodId;

    @NotBlank(message = "Mã giao dịch / số phiếu thu không được để trống")
    String transactionReference;

    String notes;
}
