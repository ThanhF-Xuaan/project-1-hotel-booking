package vn.edu.utc.hotel_booking.modules.finance.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentMethod;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupCheckoutRequest {

    @NotNull(message = "Phương thức thanh toán không được để trống")
    PaymentMethod paymentMethod;

    String paymentReference;

    @Builder.Default
    Boolean waiveUnpaidSubFolios = false;

    String notes;

    String buyerLegalName;
    String buyerTaxCode;
    String buyerAddress;
    String buyerEmailForInvoice;
}

