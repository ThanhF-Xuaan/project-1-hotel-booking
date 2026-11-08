package vn.edu.utc.hotel_booking.modules.pricing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelAgePolicyCreateRequest {

    @Schema(description = "ID khách sạn cơ sở (để trống hoặc null nếu là chính sách mặc định toàn chuỗi)", example = "1")
    Short hotelId;

    @NotBlank(message = "Loại khách không được để trống")
    @Pattern(regexp = "^(ADULT|CHILD|INFANT)$", message = "Loại khách chỉ được là ADULT, CHILD hoặc INFANT")
    @Schema(description = "Loại đối tượng khách (INFANT, CHILD, ADULT)", example = "CHILD")
    String guestType;

    @NotNull(message = "Độ tuổi tối thiểu không được để trống")
    @Min(value = 0, message = "Độ tuổi tối thiểu từ 0")
    @Max(value = 120, message = "Độ tuổi tối thiểu không vượt quá 120")
    @Schema(description = "Độ tuổi tối thiểu", example = "6")
    Short minAge;

    @NotNull(message = "Độ tuổi tối đa không được để trống")
    @Min(value = 0, message = "Độ tuổi tối đa từ 0")
    @Max(value = 120, message = "Độ tuổi tối đa không vượt quá 120")
    @Schema(description = "Độ tuổi tối đa", example = "11")
    Short maxAge;
}
