package vn.edu.utc.hotel_booking.modules.booking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InitiateDraftSessionRequest {

    @Schema(description = "Mã session cũ (nếu muốn gia hạn/cập nhật giỏ hàng hiện tại)", example = "550e8400-e29b-41d4-a716-446655440000")
    String sessionId;

    @NotNull(message = "ID khách sạn không được để trống")
    @Schema(description = "ID khách sạn", example = "1")
    Short hotelId;

    @NotNull(message = "Ngày nhận phòng không được để trống")
    @Schema(description = "Ngày check-in", example = "2026-10-15")
    LocalDate checkInDate;

    @NotNull(message = "Ngày trả phòng không được để trống")
    @Schema(description = "Ngày check-out", example = "2026-10-17")
    LocalDate checkOutDate;

    @Schema(description = "Mã voucher giảm giá (nếu có)", example = "SUMMER2026")
    String voucherCode;

    @NotEmpty(message = "Danh sách hạng phòng không được để trống")
    @Valid
    @Builder.Default
    @Schema(description = "Danh sách hạng phòng trong giỏ hàng")
    List<CartPriceQuoteRequest.CartItemQuoteRequest> items = new ArrayList<>();

    @Valid
    @Builder.Default
    @Schema(description = "Danh sách dịch vụ kèm theo (SERVICE only)")
    List<CartPriceQuoteRequest.SelectedServiceRequest> selectedServices = new ArrayList<>();

    @Valid
    @Schema(description = "Thông tin khách đại diện (nếu đã nhập)")
    LeadGuestRequest leadGuest;
}
