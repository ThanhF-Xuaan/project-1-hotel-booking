package vn.edu.utc.hotel_booking.modules.pricing.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SurchargeRuleSearchDto {

    @Schema(description = "Số trang (bắt đầu từ 0)", example = "0")
    @Builder.Default
    Integer page = 0;

    @Schema(description = "Số bản ghi mỗi trang", example = "10")
    @Builder.Default
    Integer pageSize = 10;

    @Schema(description = "ID khách sạn lọc theo cơ sở", example = "1")
    Short hotelId;

    @Schema(description = "ID cấu hình loại phòng", example = "10")
    Integer hotelRoomTypeId;

    @Schema(description = "ID chính sách độ tuổi", example = "1")
    Short agePolicyId;

    @Schema(description = "Loại phụ thu (EXTRA_PERSON, EXTRA_BED, EARLY_CHECKIN, LATE_CHECKOUT)", example = "EXTRA_PERSON")
    String ruleType;

    @Schema(description = "Hình thức tính (PER_NIGHT, PER_STAY)", example = "PER_NIGHT")
    String pricingType;

    @Schema(description = "Trạng thái (ACTIVE, INACTIVE)", example = "ACTIVE")
    String status;

    @Schema(description = "Ngày cần kiểm tra hiệu lực", example = "2026-06-15")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate activeOnDate;

    @Schema(description = "Cột sắp xếp", example = "id")
    @Builder.Default
    String sortBy = "id";

    @Schema(description = "Chiều sắp xếp (ASC, DESC)", example = "DESC")
    @Builder.Default
    String sortDirection = "DESC";
}
