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
public class GroupContractCreateRequest {

    Long inquiryId;

    @NotNull(message = "ID khách sạn không được để trống")
    Short hotelId;

    @NotNull(message = "ID công ty không được để trống")
    Long companyId;

    @NotBlank(message = "Tên đoàn không được để trống")
    @Size(max = 255, message = "Tên đoàn không được quá 255 ký tự")
    String groupName;

    @NotBlank(message = "Mã số thuế không được để trống")
    @Size(max = 50, message = "Mã số thuế không được quá 50 ký tự")
    String companyTaxCode;

    @Size(max = 150, message = "Người đại diện pháp luật không được quá 150 ký tự")
    String companyLegalRepresentative;

    @Size(max = 150, message = "Tên người ký không được quá 150 ký tự")
    String signerName;

    @Size(max = 100, message = "Chức danh người ký không được quá 100 ký tự")
    String signerTitle;

    @NotNull(message = "Ngày check-in không được để trống")
    LocalDate checkInDate;

    @NotNull(message = "Ngày check-out không được để trống")
    LocalDate checkOutDate;

    @NotNull(message = "Tổng số phòng không được để trống")
    @Min(value = 6, message = "Hợp đồng khách đoàn yêu cầu tối thiểu 6 phòng")
    Integer totalRooms;

    @NotNull(message = "Tổng số khách không được để trống")
    @Min(value = 1, message = "Tổng số khách phải lớn hơn hoặc bằng 1")
    Integer totalGuests;

    @DecimalMin(value = "0.0", message = "Tỷ lệ phí dịch vụ không được âm")
    @Builder.Default
    BigDecimal serviceFeeRate = BigDecimal.valueOf(5.00);

    @DecimalMin(value = "0.0", message = "Tỷ lệ VAT không được âm")
    @Builder.Default
    BigDecimal vatRate = BigDecimal.valueOf(10.00);

    @DecimalMin(value = "50.0", message = "Tỷ lệ đặt cọc tối thiểu là 50% theo Điều 328 BLDS 2015")
    @Builder.Default
    BigDecimal depositRate = BigDecimal.valueOf(50.00);

    String specialRequests;

    @Builder.Default
    String governingLaw = "VIETNAMESE_LAW";

    @NotEmpty(message = "Danh sách phân bổ block phòng không được để trống")
    @Valid
    List<ContractRoomBlockItem> roomBlocks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ContractRoomBlockItem {

        @NotNull(message = "ID hạng phòng không được để trống")
        Integer hotelRoomTypeId;

        @NotNull(message = "Số phòng block không được để trống")
        @Min(value = 1, message = "Số phòng block phải lớn hơn hoặc bằng 1")
        Integer blockedRooms;

        @NotNull(message = "Đơn giá phòng thỏa thuận không được để trống")
        @DecimalMin(value = "0.0", message = "Đơn giá phòng không được âm")
        BigDecimal negotiatedRatePerNight;
    }
}
