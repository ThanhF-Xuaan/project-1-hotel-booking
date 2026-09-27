package vn.edu.utc.hotel_booking.modules.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GuestCreateRequest {

    @Schema(description = "Ngày sinh của khách", example = "1990-05-15")
    LocalDate birthDate;

    @Schema(description = "Loại giấy tờ tùy thân: CCCD, PASSPORT, DRIVER_LICENSE, OTHER", example = "CCCD")
    String identityType;

    @Schema(description = "Số giấy tờ tùy thân", example = "001200001234")
    @Size(max = 50, message = "Số giấy tờ tối đa 50 ký tự")
    String identityNumber;

    @Schema(description = "Quốc tịch", example = "Việt Nam")
    @Size(max = 100, message = "Quốc tịch tối đa 100 ký tự")
    String nationality;

    @Schema(description = "Email liên hệ", example = "guest@gmail.com")
    @Email(message = "Định dạng email không hợp lệ")
    String email;

    @Schema(description = "Số điện thoại chính của khách", example = "0912345678")
    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    String phone;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    @Builder.Default
    String status = "ACTIVE";
}
