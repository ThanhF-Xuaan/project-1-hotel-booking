package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomInclusionDto {

    @Schema(description = "ID cấu hình tặng kèm", example = "1")
    Long id;

    @Schema(description = "ID món trong thực đơn cơ sở (nếu gán theo Menu)", example = "5")
    Integer menuId;

    @Schema(description = "ID danh mục chuỗi chuẩn hóa (nếu gán theo MasterItem)", example = "12")
    Integer masterItemId;

    @Schema(description = "Nguồn gốc gói tặng kèm (CHAIN_MASTER hoặc HOTEL_MENU)", example = "CHAIN_MASTER")
    String sourceType;

    @Schema(description = "Loại tham chiếu (tương thích ngược)", example = "POS_PRODUCT")
    String referenceType;

    @Schema(description = "ID tham chiếu món/dịch vụ (tương thích ngược)", example = "5")
    Integer referenceId;

    @Schema(description = "Tên quà/dịch vụ tặng kèm", example = "Buffet Sáng Tiêu Chuẩn")
    String name;

    @Schema(description = "Đơn vị tính", example = "PHẦN")
    String unit;

    @Schema(description = "Số lượng tặng kèm", example = "1")
    Integer quantity;

    @Schema(description = "Tần suất áp dụng (PER_NIGHT hoặc PER_STAY)", example = "PER_NIGHT")
    String frequency;

    @Schema(description = "Đối tượng áp dụng (PER_ROOM, PER_ADULT, PER_GUEST)", example = "PER_ROOM")
    String targetType;

    @Schema(description = "Mô tả chi tiết quà tặng kèm", example = "Miễn phí buffet sáng hàng ngày cho khách lưu trú")
    String description;
}
