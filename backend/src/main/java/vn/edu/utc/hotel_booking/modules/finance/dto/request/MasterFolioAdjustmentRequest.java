package vn.edu.utc.hotel_booking.modules.finance.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.finance.entity.ChargeCategory;
import vn.edu.utc.hotel_booking.modules.finance.entity.RoutingTarget;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MasterFolioAdjustmentRequest {

    Long bookingRoomId;

    @NotNull(message = "Danh mục chi phí không được để trống")
    ChargeCategory chargeCategory;

    @NotBlank(message = "Loại khoản mục chi phí không được để trống")
    String itemType; // 'ROOM_RATE', 'BOOKING_CHARGE', 'SERVICE_ORDER_DETAIL'

    @NotNull(message = "Reference ID không được để trống")
    Long referenceId;

    @NotBlank(message = "Mô tả khoản mục không được để trống")
    String itemDescription;

    @NotNull(message = "Mục tiêu điều chuyển không được để trống")
    RoutingTarget adjustedTarget; // 'MASTER' hoặc 'SUB_FOLIO'

    @NotNull(message = "Số tiền không được để trống")
    @DecimalMin(value = "0.01", message = "Số tiền điều chuyển phải lớn hơn 0")
    BigDecimal amount;

    @NotBlank(message = "Lý do điều chuyển không được để trống theo quy định Luật Kế toán 2015")
    String adjustmentReason;
}
