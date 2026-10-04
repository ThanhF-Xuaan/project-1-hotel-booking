package vn.edu.utc.hotel_booking.modules.operation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class StayGuestCreateRequest {

    @NotNull(message = "hotelId không được để trống")
    @Schema(description = "ID khách sạn", example = "1")
    Short hotelId;

    @Schema(description = "ID đặt phòng nếu có", example = "100")
    Long bookingId;

    @Schema(description = "ID phòng vật lý", example = "5")
    Integer roomId;

    @NotBlank(message = "Số phòng không được để trống")
    @Schema(description = "Số phòng", example = "P101")
    String roomNumber;

    @NotBlank(message = "Họ và tên khách không được để trống")
    @Schema(description = "Họ và tên khách", example = "NGUYỄN VĂN A")
    String fullName;

    @Schema(description = "Ngày tháng năm sinh", example = "1995-05-15")
    LocalDate dateOfBirth;

    @Schema(description = "Giới tính", example = "MALE")
    Gender gender;

    @Schema(description = "Quốc tịch", example = "Việt Nam")
    @Builder.Default
    String nationality = "Việt Nam";

    @NotNull(message = "Loại giấy tờ không được để trống")
    @Schema(description = "Loại giấy tờ", example = "CCCD")
    @Builder.Default
    DocumentType documentType = DocumentType.CCCD;

    @NotBlank(message = "Số giấy tờ không được để trống")
    @Schema(description = "Số giấy tờ định danh (CCCD/Passport)", example = "001200001234")
    String documentNumber;

    @Schema(description = "Nơi ĐKTT / Nơi cấp", example = "Phường Hàng Trống, Quận Hoàn Kiếm, Hà Nội")
    String permanentAddress;

    @Schema(description = "Chỗ ở hiện nay", example = "Quận Cầu Giấy, Hà Nội")
    String currentAddress;

    @NotNull(message = "Thời gian nhận phòng không được để trống")
    @Schema(description = "Thời gian bắt đầu lưu trú")
    OffsetDateTime checkInTime;

    @NotNull(message = "Thời gian dự kiến trả phòng không được để trống")
    @Schema(description = "Thời gian dự kiến trả phòng")
    OffsetDateTime expectedCheckOutTime;

    @Schema(description = "Lý do lưu trú", example = "Du lịch")
    @Builder.Default
    String reasonForStay = "Du lịch";

    @Schema(description = "Đường dẫn ảnh giấy tờ tuỳ thân")
    String documentImageUrl;
}
