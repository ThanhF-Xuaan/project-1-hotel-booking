package vn.edu.utc.hotel_booking.modules.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompanyCreateRequest {

    @Schema(description = "Tên doanh nghiệp / đối tác", example = "Tập đoàn Viễn thông Quân đội Viettel")
    @NotBlank(message = "Tên công ty không được để trống")
    @Size(max = 255, message = "Tên công ty tối đa 255 ký tự")
    String name;

    @Schema(description = "Mã số thuế doanh nghiệp", example = "0100109106")
    @Size(max = 50, message = "Mã số thuế tối đa 50 ký tự")
    String taxCode;

    @Schema(description = "Địa chỉ trụ sở công ty", example = "Số 1 Giang Văn Minh, Ba Đình, Hà Nội")
    String address;

    @Schema(description = "Tên người đại diện liên hệ", example = "Nguyễn Văn Đại")
    @Size(max = 100, message = "Tên người liên hệ tối đa 100 ký tự")
    String contactName;

    @Schema(description = "Số điện thoại liên hệ", example = "02462556789")
    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    String contactPhone;

    @Schema(description = "Email liên hệ đối tác", example = "contact@viettel.com.vn")
    @Email(message = "Định dạng email không hợp lệ")
    String contactEmail;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    @Builder.Default
    String status = "ACTIVE";
}
