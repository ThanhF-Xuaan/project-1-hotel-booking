package vn.edu.utc.hotel_booking.modules.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupContractCancelRequest {

    @NotBlank(message = "Lý do hủy hợp đồng không được để trống")
    String cancellationReason;
}
