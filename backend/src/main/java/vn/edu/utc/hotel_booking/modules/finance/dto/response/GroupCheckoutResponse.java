package vn.edu.utc.hotel_booking.modules.finance.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.finance.entity.MasterFolioStatus;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupCheckoutResponse {

    Long masterFolioId;
    String folioNumber;
    Long contractId;
    String contractNumber;
    String groupName;
    MasterFolioStatus status;

    // Bảng quyết toán tài chính
    BigDecimal subtotalAmount;
    BigDecimal serviceFeeAmount;
    BigDecimal totalVatAmount;
    BigDecimal totalChargesAmount;
    BigDecimal depositCredited;
    BigDecimal finalPaymentAmount;
    BigDecimal remainingBalance;

    PaymentMethod paymentMethod;
    String paymentReference;

    // Thống kê phòng
    Integer checkedOutRoomsCount;
    @Builder.Default
    List<String> checkedOutRoomNumbers = new ArrayList<>();

    OffsetDateTime settledAt;
    String settledBy;
}

