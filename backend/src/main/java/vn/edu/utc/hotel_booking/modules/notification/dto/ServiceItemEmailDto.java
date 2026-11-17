package vn.edu.utc.hotel_booking.modules.notification.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceItemEmailDto {
    String name;
    Integer quantity;
    BigDecimal unitPrice;
    BigDecimal totalPrice;
    String unitPriceFormatted;
    String totalPriceFormatted;
}
