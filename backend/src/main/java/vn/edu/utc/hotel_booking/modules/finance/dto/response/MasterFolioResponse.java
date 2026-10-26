package vn.edu.utc.hotel_booking.modules.finance.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.finance.entity.MasterFolioStatus;

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
public class MasterFolioResponse {

    Long id;
    String folioNumber;
    Long contractId;
    String contractNumber;
    Short hotelId;
    String hotelName;
    Long companyId;
    String companyName;

    // Pháp nhân xuất hóa đơn điện tử GTGT (NĐ 123/2020 & TT 78/2021)
    String buyerLegalName;
    String buyerTaxCode;
    String buyerAddress;
    String buyerEmailForInvoice;

    // Tài chính Sổ cái Tổng hợp
    BigDecimal subtotalAmount;
    BigDecimal serviceFeeAmount;
    BigDecimal totalVatAmount;
    BigDecimal totalAmount;
    BigDecimal depositCredited;
    BigDecimal totalPaid;
    BigDecimal balance;

    MasterFolioStatus status;
    OffsetDateTime closedAt;
    OffsetDateTime createdAt;

    @Builder.Default
    List<BillingRoutingRuleResponse> routingRules = new ArrayList<>();

    @Builder.Default
    List<MasterFolioLineItemDto> debits = new ArrayList<>();

    @Builder.Default
    List<SubFolioSummaryDto> subFolios = new ArrayList<>();
}
