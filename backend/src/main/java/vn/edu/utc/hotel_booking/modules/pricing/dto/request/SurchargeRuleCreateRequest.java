package vn.edu.utc.hotel_booking.modules.pricing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SurchargeRuleCreateRequest {

    @NotNull(message = "ID loại phòng không được để trống")
    @Schema(description = "ID loại phòng cơ sở áp dụng", example = "10")
    Integer hotelRoomTypeId;

    @Schema(description = "ID chính sách độ tuổi (áp dụng khi là EXTRA_PERSON)", example = "1")
    Short agePolicyId;

    @NotBlank(message = "Loại quy tắc phụ thu không được để trống")
    @Pattern(regexp = "EXTRA_PERSON|EXTRA_BED|EARLY_CHECKIN|LATE_CHECKOUT",
            message = "Loại phụ thu phải là EXTRA_PERSON, EXTRA_BED, EARLY_CHECKIN hoặc LATE_CHECKOUT")
    @Schema(description = "Loại phụ thu", example = "EXTRA_PERSON")
    String ruleType;

    @NotBlank(message = "Hình thức tính giá không được để trống")
    @Pattern(regexp = "PER_NIGHT|PER_STAY", message = "Hình thức tính giá phải là PER_NIGHT hoặc PER_STAY")
    @Schema(description = "Hình thức tính: Theo đêm hoặc Theo kỳ lưu trú", example = "PER_NIGHT")
    String pricingType;

    @NotBlank(message = "Kiểu điều chỉnh không được để trống")
    @Pattern(regexp = "PERCENT|FIXED", message = "Kiểu điều chỉnh phải là PERCENT hoặc FIXED")
    @Schema(description = "Kiểu tính: Tỷ lệ phần trăm hoặc Số tiền cố định", example = "FIXED")
    String adjustmentType;

    @NotNull(message = "Giá trị phụ thu không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Giá trị phụ thu phải lớn hơn hoặc bằng 0")
    @Schema(description = "Giá trị phụ thu (VNĐ hoặc %)", example = "150000")
    BigDecimal adjustmentValue;

    @NotNull(message = "Ngày bắt đầu áp dụng không được để trống")
    @Schema(description = "Ngày bắt đầu áp dụng", example = "2026-06-01")
    LocalDate startDate;

    @NotNull(message = "Ngày kết thúc áp dụng không được để trống")
    @Schema(description = "Ngày kết thúc áp dụng", example = "2026-12-31")
    LocalDate endDate;

    @Schema(description = "Điều kiện phụ thu dạng JSON (ví dụ giờ nhận sớm, giờ trả muộn)", example = "{\"checkInBefore\": \"12:00\"}")
    @Builder.Default
    String conditions = "{}";

    @Pattern(regexp = "ACTIVE|INACTIVE", message = "Trạng thái phải là ACTIVE hoặc INACTIVE")
    @Schema(description = "Trạng thái quy tắc", example = "ACTIVE")
    @Builder.Default
    String status = "ACTIVE";
}
