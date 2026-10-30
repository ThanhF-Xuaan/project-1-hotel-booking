package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Kết quả xem trước (Preview) Rooming List từ file Excel")
public class RoomingListPreviewResponse {

    @Schema(description = "ID đơn đặt phòng", example = "10")
    Long bookingId;

    @Schema(description = "Mã đơn đặt phòng", example = "BK-20261030-0001")
    String bookingNumber;

    @Schema(description = "Tổng số dòng dữ liệu đọc được từ Excel", example = "20")
    Integer totalRows;

    @Schema(description = "Số dòng dữ liệu hợp lệ sẵn sàng import", example = "18")
    Integer validCount;

    @Schema(description = "Số dòng dữ liệu phát hiện lỗi", example = "2")
    Integer errorCount;

    @Schema(description = "Danh sách chi tiết từng dòng dữ liệu kèm trạng thái hợp lệ")
    @Builder.Default
    List<RoomingListRowDto> rows = new ArrayList<>();

    @Schema(description = "Danh sách tất cả các lỗi được phân loại theo từng dòng")
    @Builder.Default
    List<RoomingListErrorDto> errors = new ArrayList<>();
}
