package vn.edu.utc.hotel_booking.modules.finance.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.finance.entity.MasterFolioStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MasterFolioStatusUpdateRequest {

    @NotNull(message = "Trạng thái Master Folio không được để trống")
    MasterFolioStatus status;

    String note;
}
