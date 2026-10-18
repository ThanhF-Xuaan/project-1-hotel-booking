package vn.edu.utc.hotel_booking.modules.operation.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderReferenceType;
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
    Short hotelId;
    String hotelName;
    ServiceOrderReferenceType referenceType;
    Long referenceId;

    // Backward compatibility fields
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
    vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderPaymentMethod paymentMethod;
    vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderPaymentStatus paymentStatus;
    OffsetDateTime issuedAt;
    OffsetDateTime createdAt;

    @Builder.Default
    List<ServiceOrderDetailResponse> details = new ArrayList<>();
}
