package vn.edu.utc.hotel_booking.modules.booking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LeadGuestRequest {

    @NotBlank(message = "Họ và tên người đại diện không được để trống")
    @Schema(description = "Họ và tên người đại diện đặt phòng", example = "Nguyễn Văn A")
    String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)(2[0-9]{9}|[35789][0-9]{8})$", message = "Số điện thoại không đúng định dạng (VD: 0987654321 hoặc 02466668888)")
    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    @Schema(description = "Số điện thoại liên hệ", example = "0987654321")
    String phone;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Định dạng email không hợp lệ")
    @Schema(description = "Email nhận xác nhận đặt phòng", example = "nguyenvana@gmail.com")
    String email;

    @Schema(description = "Số CMND/CCCD/Hộ chiếu", example = "001201012345")
    String identityNumber;

    @Schema(description = "Loại giấy tờ tùy thân (CCCD, PASSPORT)", example = "CCCD")
    @Builder.Default
    String identityType = "CCCD";

    @Schema(description = "Quốc tịch của khách", example = "VIETNAM")
    @Builder.Default
    String nationality = "VIETNAM";

    @Schema(description = "Ngày sinh của khách (YYYY-MM-DD)", example = "1995-08-15")
    LocalDate birthDate;

    @Schema(description = "Ghi chú hoặc yêu cầu đặc biệt của khách hàng", example = "Phòng tầng cao, view thoáng")
    String notes;
}
