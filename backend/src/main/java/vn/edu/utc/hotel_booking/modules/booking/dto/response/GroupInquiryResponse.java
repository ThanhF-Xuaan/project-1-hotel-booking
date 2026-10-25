package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupInquiryStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupInquiryResponse {

    Long id;
    String inquiryNumber;
    Short hotelId;
    String hotelName;
    String companyName;
    String taxCode;
    String contactName;
    String contactEmail;
    String contactPhone;
    LocalDate checkInDate;
    LocalDate checkOutDate;
    Integer totalRooms;
    Integer totalGuests;
    BigDecimal estimatedBudget;
    String specialRequests;
    GroupInquiryStatus status;
    Integer salesStaffId;
    String salesStaffName;
    Long convertedContractId;
    String convertedContractNumber;
    String rejectionReason;
    OffsetDateTime createdAt;
    List<InquiryRoomTypeResponse> requestedRoomTypes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class InquiryRoomTypeResponse {
        Long id;
        Integer hotelRoomTypeId;
        String roomTypeName;
        Integer requestedRooms;
        BigDecimal estimatedRatePerNight;
    }
}
