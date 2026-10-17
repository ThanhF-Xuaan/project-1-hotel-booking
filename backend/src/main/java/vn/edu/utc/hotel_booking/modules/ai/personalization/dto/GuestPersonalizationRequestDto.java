package vn.edu.utc.hotel_booking.modules.ai.personalization.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO yêu cầu phân tích và gợi ý cá nhân hoá cho khách hàng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuestPersonalizationRequestDto {

    @NotNull(message = "guestId cannot be null")
    @Min(value = 1, message = "guestId must be >= 1")
    private Long guestId;

    /** Khách sạn mục tiêu đang xem phòng (optional). */
    @Min(value = 1, message = "targetHotelId must be >= 1")
    private Integer targetHotelId;

    /** Khách hàng đã đồng ý cho phép sử dụng dữ liệu lịch sử lưu trú để cá nhân hoá (bắt buộc). */
    private boolean consentGranted;

    /** Ngày tham chiếu để tính độ suy giảm theo thời gian (mặc định LocalDate.now()). */
    private LocalDate referenceDate;

    /** Số lượng gợi ý hạng phòng tối đa (1 đến 10, mặc định 3). */
    @Min(value = 1, message = "maxSuggestions must be >= 1")
    @Max(value = 10, message = "maxSuggestions cannot exceed 10")
    @Builder.Default
    private Integer maxSuggestions = 3;

    /** Danh sách các kỳ lưu trú trong lịch sử (tối đa 100 bản ghi để phòng ngừa DoS). */
    @Valid
    @Size(max = 100, message = "stayHistory cannot exceed 100 records")
    private List<StayHistoryRecordDto> stayHistory;
}
