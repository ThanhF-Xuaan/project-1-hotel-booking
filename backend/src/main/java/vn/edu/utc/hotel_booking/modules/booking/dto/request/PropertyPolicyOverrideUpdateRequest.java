package vn.edu.utc.hotel_booking.modules.booking.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PropertyPolicyOverrideUpdateRequest {

    Integer cancellationPolicyId;

    Integer noShowPolicyId;

    @Min(value = 0, message = "Số ngày hủy miễn phí ghi đè phải >= 0")
    Integer freeCancellationBeforeDaysOverride;

    @JsonFormat(pattern = "HH:mm:ss")
    LocalTime noShowReleaseTimeOverride;

    @NotBlank(message = "Lý do ghi đè chính sách là bắt buộc")
    @Size(max = 255, message = "Lý do tối đa 255 ký tự")
    String overrideReason;

    Boolean isActive;
}
