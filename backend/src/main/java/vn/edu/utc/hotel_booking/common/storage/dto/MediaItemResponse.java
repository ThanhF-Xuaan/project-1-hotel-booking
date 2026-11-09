package vn.edu.utc.hotel_booking.common.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.storage.domain.MediaLifecycleStatus;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageAccessLevel;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageCategory;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MediaItemResponse {

    @Schema(description = "ID bản ghi hình ảnh", example = "101")
    Long id;

    @Schema(description = "Object key lưu trên storage", example = "public/hotels/1/rooms/202/uuid_room.jpg")
    String storageKey;

    @Schema(description = "URL công khai hoặc presigned URL xem trực tiếp ảnh")
    String url;

    @Schema(description = "Phân loại danh mục")
    StorageCategory category;

    @Schema(description = "Tên hiển thị tiếng Việt của phân loại")
    String categoryLabel;

    @Schema(description = "Tên tệp tin gốc", example = "deluxe_king_room.jpg")
    String originalFilename;

    @Schema(description = "Loại MIME type", example = "image/jpeg")
    String contentType;

    @Schema(description = "Kích thước tệp (bytes)", example = "1048576")
    Long sizeBytes;

    @Schema(description = "Mã băm SHA-256")
    String checksumSha256;

    @Schema(description = "ID khách sạn liên quan (nếu có)", example = "1")
    Short hotelId;

    @Schema(description = "Tên khách sạn (nếu có)", example = "Viettel Luxury Hà Nội")
    String hotelName;

    @Schema(description = "ID đối tượng tham chiếu (hạng phòng, người dùng...)")
    Long referenceId;

    @Schema(description = "Có phải là ảnh đại diện chính của đối tượng không", example = "true")
    Boolean isPrimary;

    @Schema(description = "Cấp độ truy cập bảo mật")
    StorageAccessLevel accessLevel;

    @Schema(description = "Trạng thái vòng đời")
    MediaLifecycleStatus lifecycleStatus;

    @Schema(description = "Người tải lên")
    String uploadedBy;

    @Schema(description = "Thời gian tạo")
    OffsetDateTime createdAt;
}

