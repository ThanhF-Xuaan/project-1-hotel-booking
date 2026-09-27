package vn.edu.utc.hotel_booking.modules.operation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NightAuditRequest {

    @NotNull(message = "ID khách sạn không được để trống")
    Short hotelId;

    @NotNull(message = "Ngày đối soát không được để trống")
    LocalDate auditDate;
}
