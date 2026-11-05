package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Thông tin chi tiết dòng lỗi khi import danh sách phòng")
public class RoomingListErrorDto {

    @Schema(description = "Số dòng Excel bị lỗi (1-based, tính cả tiêu đề)", example = "2")
    Integer rowNumber;

    @Schema(description = "Tên cột hoặc trường bị lỗi", example = "Full Name")
    String column;

    @Schema(description = "Mô tả nguyên nhân lỗi", example = "Họ và tên khách không được để trống")
    String error;
}
