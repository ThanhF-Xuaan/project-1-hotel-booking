package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupContractStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupContractResponse {

    Long id;
    String contractNumber;
    Short hotelId;
    String hotelName;
    Long companyId;
    String companyName;
    Integer salesStaffId;
    String salesStaffName;
    String groupName;
    String companyTaxCode;
    String companyLegalRepresentative;
    String signerName;
    String signerTitle;
    LocalDate checkInDate;
    LocalDate checkOutDate;
    Integer totalRooms;
    Integer totalGuests;
    BigDecimal subtotalAmount;
    BigDecimal serviceFeeRate;
    BigDecimal serviceFeeAmount;
    BigDecimal vatRate;
    BigDecimal vatAmount;
    BigDecimal totalContractValue;
    BigDecimal depositRate;
    BigDecimal depositAmount;
    BigDecimal depositPaidAmount;
    OffsetDateTime depositDueDate;
    OffsetDateTime depositPaidAt;
    OffsetDateTime cutoffDate;
    OffsetDateTime finalPaymentDueDate;
    Boolean lockCheckin;
    String governingLaw;
    GroupContractStatus status;
    String cancellationReason;
    String specialRequests;
    Long masterFolioId;
    String masterFolioNumber;
    OffsetDateTime createdAt;
    List<GroupRoomBlockResponse> roomBlocks;
}
