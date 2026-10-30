package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.DocumentType;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.Gender;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Dữ liệu một dòng khách trong Rooming List")
public class RoomingListRowDto {

    @Schema(description = "Số dòng trong Excel (1-based, tính cả hàng tiêu đề)", example = "2")
    Integer rowNumber;

    @Schema(description = "Họ và tên khách", example = "NGUYỄN VĂN A")
    String fullName;

    @Schema(description = "Giới tính chuẩn hóa (MALE, FEMALE, OTHER)", example = "MALE")
    Gender gender;

    @Schema(description = "Giá trị giới tính thô từ Excel", example = "Nam")
    String genderRaw;

    @Schema(description = "Ngày sinh chuẩn hóa", example = "1990-05-15")
    LocalDate dateOfBirth;

    @Schema(description = "Giá trị ngày sinh thô từ Excel", example = "15/05/1990")
    String dateOfBirthRaw;

    @Schema(description = "Quốc tịch", example = "Việt Nam")
    @Builder.Default
    String nationality = "Việt Nam";

    @Schema(description = "Loại giấy tờ định danh (CCCD, PASSPORT, OTHER)", example = "CCCD")
    @Builder.Default
    DocumentType documentType = DocumentType.CCCD;

    @Schema(description = "Số giấy tờ tùy thân (CCCD / Hộ chiếu)", example = "001200001234")
    String idNumber;

    @Schema(description = "Số điện thoại", example = "0912345678")
    String phone;

    @Schema(description = "Email khách", example = "guest@example.com")
    String email;

    @Schema(description = "Số phòng lưu trú nếu có", example = "101")
    String roomNumber;

    @Schema(description = "Ghi chú hoặc lý do lưu trú", example = "Khách đoàn công ty ABC")
    String note;

    @Schema(description = "Trạng thái hợp lệ của dòng dữ liệu", example = "true")
    @Builder.Default
    Boolean isValid = true;

    @Schema(description = "Danh sách lỗi chi tiết của dòng này (nếu có)")
    @Builder.Default
    List<String> errors = new ArrayList<>();
}
