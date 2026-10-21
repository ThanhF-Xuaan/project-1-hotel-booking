package vn.edu.utc.hotel_booking.modules.booking.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingType;
import vn.edu.utc.hotel_booking.modules.booking.entity.CancellationFeeType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CancellationPolicySearchDto {

    String keyword;
    BookingType bookingType;
    CancellationFeeType cancellationFeeType;
    Boolean isActive;

    @Builder.Default
    Integer page = 0;

    @Builder.Default
    Integer pageSize = 10;

    @Builder.Default
    String sortBy = "id";

    @Builder.Default
    String sortDirection = "DESC";
}
