package vn.edu.utc.hotel_booking.modules.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionSearchDto {

    @Schema(description = "Trang hiện tại (bắt đầu từ 0)", example = "0")
    Integer page;

    @Schema(description = "Số lượng bản ghi trên một trang", example = "20")
    Integer pageSize;

    @Schema(description = "Từ khóa tìm kiếm (theo hành động hoặc tài nguyên)", example = "BOOKING")
    String keyword;

    @Schema(description = "Tài nguyên bảo vệ (HOTEL, STAFF, BOOKING...)", example = "BOOKING")
    String resource;

    @Schema(description = "Hành động (VIEW, CREATE, UPDATE, DELETE...)", example = "VIEW")
    String action;

    @Schema(description = "Trường sắp xếp", example = "resource")
    String sortBy;

    @Schema(description = "Chiều sắp xếp (ASC hoặc DESC)", example = "ASC")
    String sortDirection;
}

