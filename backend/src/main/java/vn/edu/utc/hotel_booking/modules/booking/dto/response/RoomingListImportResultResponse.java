package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.StayGuestResponse;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Kết quả xác nhận import Rooming List vào đơn đặt phòng")
public class RoomingListImportResultResponse {

    @Schema(description = "ID đơn đặt phòng", example = "10")
    Long bookingId;

    @Schema(description = "Mã đơn đặt phòng", example = "BK-20261030-0001")
    String bookingNumber;

    @Schema(description = "Tổng số dòng đã xử lý", example = "20")
    Integer totalRows;

    @Schema(description = "Số lượng bản ghi khách lưu trú mới được tạo thành công", example = "18")
    Integer importedCount;

    @Schema(description = "Số lượng bản ghi bị bỏ qua do đã tồn tại (chống trùng)", example = "0")
    Integer skippedCount;

    @Schema(description = "Số lượng dòng bị từ chối do lỗi", example = "2")
    Integer errorCount;

    @Schema(description = "Danh sách khách lưu trú vừa import thành công")
    @Builder.Default
    List<StayGuestResponse> importedGuests = new ArrayList<>();

    @Schema(description = "Danh sách lỗi chi tiết nếu có dòng không thể import")
    @Builder.Default
    List<RoomingListErrorDto> errors = new ArrayList<>();
}
