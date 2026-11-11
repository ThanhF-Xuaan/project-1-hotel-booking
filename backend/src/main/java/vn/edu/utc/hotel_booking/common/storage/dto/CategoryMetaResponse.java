package vn.edu.utc.hotel_booking.common.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageCategory;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryMetaResponse {

    @Schema(description = "Mã phân loại")
    StorageCategory category;

    @Schema(description = "Tên hiển thị tiếng Việt")
    String label;

    @Schema(description = "Thư mục tiền tố")
    String folderPrefix;

    @Schema(description = "Có phải tài nguyên công khai không")
    boolean isPublic;

    @Schema(description = "Kích thước tệp tối đa (bytes)")
    Long maxSizeBytes;

    @Schema(description = "Danh sách định dạng MIME type được phép")
    Set<String> allowedMimeTypes;
}

