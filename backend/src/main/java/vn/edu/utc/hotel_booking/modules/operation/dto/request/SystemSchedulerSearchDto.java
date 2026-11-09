package vn.edu.utc.hotel_booking.modules.operation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SystemSchedulerSearchDto {

    @Schema(description = "Từ khóa tìm kiếm theo tên hoặc mã tác vụ")
    String keyword;

    @Schema(description = "Lọc theo phân hệ (INVENTORY, BOOKING, IDENTITY, OPERATION)")
    String category;

    @Schema(description = "Lọc theo khả năng kích hoạt thủ công (true: Cho phép thủ công, false: Chỉ tự động)")
    Boolean allowManualTrigger;
}

