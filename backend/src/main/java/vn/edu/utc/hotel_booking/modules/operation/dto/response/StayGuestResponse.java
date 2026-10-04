package vn.edu.utc.hotel_booking.modules.operation.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.DocumentType;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.Gender;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StayGuestResponse {

    Long id;
    Short hotelId;
    String hotelName;
    Long bookingId;
    Integer roomId;
    String roomNumber;
    String fullName;
    LocalDate dateOfBirth;
    Gender gender;
    String nationality;
    DocumentType documentType;
    String documentNumber;
    String permanentAddress;
    String currentAddress;
    OffsetDateTime checkInTime;
    OffsetDateTime expectedCheckOutTime;
    OffsetDateTime actualCheckOutTime;
    String reasonForStay;
    String documentImageUrl;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
