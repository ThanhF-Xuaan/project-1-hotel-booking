package vn.edu.utc.hotel_booking.common.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageCategory;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MediaSearchDto {

    @Schema(description = "Từ khóa tìm kiếm theo tên tệp gốc")
    String keyword;

    @Schema(description = "Phân loại danh mục lưu trữ (StorageCategory)")
    StorageCategory category;

    @Schema(description = "ID khách sạn cơ sở")
    Short hotelId;

    @Schema(description = "ID tham chiếu đối tượng (hạng phòng, người dùng...)")
    Long referenceId;

    @Schema(description = "Lọc ảnh đại diện chính (true/false)")
    Boolean isPrimary;

    @Schema(description = "Số trang (bắt đầu từ 0)", example = "0")
    @Builder.Default
    Integer page = 0;

    @Schema(description = "Số phần tử mỗi trang", example = "12")
    @Builder.Default
    Integer pageSize = 12;

    @Schema(description = "Trường sắp xếp", example = "createdAt")
    @Builder.Default
    String sortBy = "createdAt";

    @Schema(description = "Hướng sắp xếp (asc/desc)", example = "desc")
    @Builder.Default
    String sortDirection = "desc";
}

