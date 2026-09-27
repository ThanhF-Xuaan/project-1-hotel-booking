package vn.edu.utc.hotel_booking.modules.operation.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceOrderResponse {

    Long id;
    String orderNumber;
    Long bookingId;
    String bookingNumber;
    Integer roomInstanceId;
    String roomNumber;
    BigDecimal subTotal;
    BigDecimal serviceFeeRate;
    BigDecimal serviceFeeAmount;
    BigDecimal vatAmount;
    BigDecimal totalAmount;
    ServiceOrderStatus status;
    OffsetDateTime issuedAt;
    OffsetDateTime createdAt;

    @Builder.Default
    List<ServiceOrderDetailResponse> details = new ArrayList<>();
}
