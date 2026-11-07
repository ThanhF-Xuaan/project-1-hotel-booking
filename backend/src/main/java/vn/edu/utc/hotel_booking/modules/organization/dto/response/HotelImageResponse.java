package vn.edu.utc.hotel_booking.modules.organization.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelImageResponse {

    @Schema(description = "ID bản ghi ảnh trong hệ thống", example = "101")
    Long id;

    @Schema(description = "URL truy cập hình ảnh", example = "http://localhost:9000/hotel-booking-storage/public/hotels/1/properties/hotel_101.jpg")
    String url;

    @Schema(description = "Tên tệp gốc khi tải lên", example = "hotel_facade.jpg")
    String originalFilename;

    @Schema(description = "Dung lượng tệp (bytes)", example = "2048576")
    Long sizeBytes;

    @Schema(description = "Cờ đánh dấu ảnh đại diện chính của khách sạn", example = "true")
    boolean isPrimary;

    @Schema(description = "Thời gian tải lên")
    OffsetDateTime createdAt;
}

