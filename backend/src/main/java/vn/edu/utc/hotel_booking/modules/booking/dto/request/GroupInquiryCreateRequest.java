package vn.edu.utc.hotel_booking.modules.booking.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupInquiryCreateRequest {

    @NotNull(message = "ID khách sạn không được để trống")
    Short hotelId;

    @NotBlank(message = "Tên công ty/đoàn không được để trống")
    @Size(max = 255, message = "Tên công ty không được vượt quá 255 ký tự")
    String companyName;

    @Size(max = 50, message = "Mã số thuế không được vượt quá 50 ký tự")
    String taxCode;

    @NotBlank(message = "Tên người liên hệ không được để trống")
    @Size(max = 150, message = "Tên người liên hệ không được vượt quá 150 ký tự")
    String contactName;

    @NotBlank(message = "Số điện thoại liên hệ không được để trống")
    @Pattern(regexp = "^[0-9+() -]{8,20}$", message = "Số điện thoại không đúng định dạng")
    String contactPhone;

    @NotBlank(message = "Email liên hệ không được để trống")
    @Email(message = "Email không đúng định dạng")
    String contactEmail;

    @NotNull(message = "Ngày check-in không được để trống")
    LocalDate checkInDate;

    @NotNull(message = "Ngày check-out không được để trống")
    LocalDate checkOutDate;

    @NotNull(message = "Tổng số phòng yêu cầu không được để trống")
    @Min(value = 6, message = "Đặt phòng đoàn yêu cầu tối thiểu 6 phòng theo quy định nghiệp vụ")
    Integer totalRooms;

    @NotNull(message = "Tổng số khách không được để trống")
    @Min(value = 1, message = "Tổng số khách phải lớn hơn hoặc bằng 1")
    Integer totalGuests;

    @DecimalMin(value = "0.0", message = "Ngân sách dự kiến không được âm")
    BigDecimal estimatedBudget;

    String specialRequests;

    @NotEmpty(message = "Danh sách hạng phòng yêu cầu không được để trống")
    @Valid
    List<InquiryRoomTypeItem> requestedRoomTypes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class InquiryRoomTypeItem {

        @NotNull(message = "ID hạng phòng không được để trống")
        Integer hotelRoomTypeId;

        @NotNull(message = "Số lượng phòng yêu cầu không được để trống")
        @Min(value = 1, message = "Số lượng phòng yêu cầu phải lớn hơn hoặc bằng 1")
        Integer requestedRooms;
    }
}
