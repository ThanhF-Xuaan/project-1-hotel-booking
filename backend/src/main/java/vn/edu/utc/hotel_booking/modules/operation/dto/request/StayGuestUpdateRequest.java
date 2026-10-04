package vn.edu.utc.hotel_booking.modules.operation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.DocumentType;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.Gender;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StayGuestUpdateRequest {

    @Schema(description = "Số phòng", example = "P101")
    String roomNumber;

    @Schema(description = "Họ và tên khách", example = "NGUYỄN VĂN A")
    String fullName;

    @Schema(description = "Ngày tháng năm sinh", example = "1995-05-15")
    LocalDate dateOfBirth;

    @Schema(description = "Giới tính", example = "MALE")
    Gender gender;

    @Schema(description = "Quốc tịch", example = "Việt Nam")
    String nationality;

    @Schema(description = "Loại giấy tờ", example = "CCCD")
    DocumentType documentType;

    @Schema(description = "Số giấy tờ định danh (CCCD/Passport)", example = "001200001234")
    String documentNumber;

    @Schema(description = "Nơi ĐKTT / Nơi cấp", example = "Phường Hàng Trống, Quận Hoàn Kiếm, Hà Nội")
    String permanentAddress;

    @Schema(description = "Chỗ ở hiện nay", example = "Quận Cầu Giấy, Hà Nội")
    String currentAddress;

    @Schema(description = "Thời gian dự kiến trả phòng")
    OffsetDateTime expectedCheckOutTime;

    @Schema(description = "Thời gian thực tế trả phòng")
    OffsetDateTime actualCheckOutTime;

    @Schema(description = "Lý do lưu trú", example = "Du lịch")
    String reasonForStay;

    @Schema(description = "Đường dẫn ảnh giấy tờ tuỳ thân")
    String documentImageUrl;
}
