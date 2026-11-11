package vn.edu.utc.hotel_booking.modules.operation.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.operation.entity.MenuType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MasterItemSearchDto {

    String code;
    String name;
    MenuType itemType;
    Integer taxCategoryId;
    String status;

    @Builder.Default
    Integer page = 0;

    @Builder.Default
    Integer pageSize = 10;
}
