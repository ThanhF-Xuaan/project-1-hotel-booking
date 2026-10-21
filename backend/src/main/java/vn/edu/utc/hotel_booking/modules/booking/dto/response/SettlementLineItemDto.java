package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SettlementLineItemDto {

    /**
     * Phân loại dòng chi phí: ROOM_RATE, SURCHARGE, PENALTY, SERVICE, PRODUCT
     */
    String lineType;

    /**
     * ID tham chiếu bản ghi gốc (booking_daily_rate_id, booking_charge_id, service_order_detail_id)
     */
    Long referenceId;

    /**
     * Tên mô tả khoản mục
     */
    String description;

    /**
     * Số lượng
     */
    Integer quantity;

    /**
     * Đơn giá trước thuế phí
     */
    BigDecimal unitPrice;

    /**
     * Tiền trước thuế phí (unitPrice * quantity)
     */
    BigDecimal subtotal;

    /**
     * Tỷ lệ phí dịch vụ (%)
     */
    BigDecimal serviceFeeRate;

    /**
     * Tiền phí dịch vụ
     */
    BigDecimal serviceFeeAmount;

    /**
     * Tỷ lệ thuế VAT (%)
     */
    BigDecimal vatRate;

    /**
     * Tiền thuế VAT
     */
    BigDecimal vatAmount;

    /**
     * Tổng số tiền dòng chi phí (subtotal + serviceFee + vat)
     */
    BigDecimal totalAmount;
}
