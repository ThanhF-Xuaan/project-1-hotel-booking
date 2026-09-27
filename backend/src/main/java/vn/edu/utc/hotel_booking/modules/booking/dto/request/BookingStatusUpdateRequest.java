package vn.edu.utc.hotel_booking.modules.booking.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingStatusUpdateRequest {

    @NotNull(message = "Trạng thái mới không được để trống")
    BookingStatus status;
}
