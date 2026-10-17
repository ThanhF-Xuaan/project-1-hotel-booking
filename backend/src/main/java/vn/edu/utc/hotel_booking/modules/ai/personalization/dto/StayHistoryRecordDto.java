package vn.edu.utc.hotel_booking.modules.ai.personalization.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Bản ghi dữ liệu một kỳ lưu trú trong lịch sử của khách hàng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StayHistoryRecordDto {

    @NotNull(message = "hotelId cannot be null")
    @Min(value = 1, message = "hotelId must be >= 1")
    private Integer hotelId;

    @NotNull(message = "hotelRoomTypeId cannot be null")
    @Min(value = 1, message = "hotelRoomTypeId must be >= 1")
    private Integer hotelRoomTypeId;

    @NotBlank(message = "roomTypeCode cannot be blank")
    @Size(max = 64, message = "roomTypeCode cannot exceed 64 characters")
    private String roomTypeCode;

    @NotBlank(message = "roomTypeName cannot be blank")
    @Size(max = 255, message = "roomTypeName cannot exceed 255 characters")
    private String roomTypeName;

    @NotNull(message = "checkInDate cannot be null")
    private LocalDate checkInDate;

    @NotNull(message = "checkOutDate cannot be null")
    private LocalDate checkOutDate;

    @Min(value = 1, message = "adults must be >= 1")
    @Max(value = 20, message = "adults cannot exceed 20")
    @Builder.Default
    private int adults = 1;

    @Min(value = 0, message = "children must be >= 0")
    @Max(value = 20, message = "children cannot exceed 20")
    @Builder.Default
    private int children = 0;

    /** Danh sách mã đặc tính phòng yêu thích (ví dụ: SEA_VIEW, BALCONY, HIGH_FLOOR, BATHTUB, QUIET_ROOM, WORK_DESK). */
    @Size(max = 50, message = "preferredFeatureCodes cannot exceed 50 items")
    private List<String> preferredFeatureCodes;

    /** Danh sách mã dịch vụ đã sử dụng (ví dụ: BREAKFAST, AIRPORT_TRANSFER, SPA, LAUNDRY). */
    @Size(max = 50, message = "usedServiceCodes cannot exceed 50 items")
    private List<String> usedServiceCodes;

    /** Ghi chú sở thích cá nhân (optional). */
    @Size(max = 500, message = "guestPreferenceNote cannot exceed 500 characters")
    private String guestPreferenceNote;
}
