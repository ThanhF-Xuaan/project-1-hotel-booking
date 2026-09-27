package vn.edu.utc.hotel_booking.modules.identity.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompanyResponse {

    @Schema(description = "ID doanh nghiệp", example = "1")
    Long id;

    @Schema(description = "Tên doanh nghiệp", example = "Tập đoàn Viettel")
    String name;

    @Schema(description = "Mã số thuế", example = "0100109106")
    String taxCode;

    @Schema(description = "Địa chỉ trụ sở")
    String address;

    @Schema(description = "Tên người đại diện liên hệ", example = "Nguyễn Văn Đại")
    String contactName;

    @Schema(description = "Số điện thoại liên hệ", example = "02462556789")
    String contactPhone;

    @Schema(description = "Email liên hệ đối tác", example = "contact@viettel.com.vn")
    String contactEmail;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;

    @Schema(description = "Thời gian tạo hồ sơ")
    OffsetDateTime createdAt;

    @Schema(description = "Thời gian cập nhật")
    OffsetDateTime updatedAt;
}
