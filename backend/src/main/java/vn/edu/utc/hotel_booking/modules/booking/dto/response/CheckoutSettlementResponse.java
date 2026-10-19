package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.PaymentResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CheckoutSettlementResponse {

    Long bookingId;
    Long bookingRoomId;
    String bookingNumber;
    String roomNumber;
    String roomTypeName;
    String guestName;
    String guestPhone;

    // Tổng hợp tài chính
    BigDecimal totalRoomCharges;
    BigDecimal totalSurcharges;
    BigDecimal totalServiceCharges;
    BigDecimal grandTotal;
    BigDecimal totalPaid;
    BigDecimal balanceDue;
    boolean canCheckOut;

    // Bổ sung cho Folio Netting khi có phòng No-Show hoặc tiền cọc thừa
    @Builder.Default
    BigDecimal noShowPenaltyAmount = BigDecimal.ZERO;

    @Builder.Default
    BigDecimal netOverpaymentAmount = BigDecimal.ZERO;

    // Chi tiết từng dòng chi phí
    @Builder.Default
    List<SettlementLineItemDto> lineItems = new ArrayList<>();

    // Lịch sử các đợt thanh toán trước đó
    @Builder.Default
    List<PaymentResponse> paymentHistory = new ArrayList<>();
}
