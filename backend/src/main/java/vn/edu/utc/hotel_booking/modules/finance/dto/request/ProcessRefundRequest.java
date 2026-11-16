package vn.edu.utc.hotel_booking.modules.finance.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentMethod;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProcessRefundRequest {

    public enum RefundAction {
        APPROVE,
        REJECT
    }

    @NotNull(message = "Hành động phê duyệt không được để trống (APPROVE hoặc REJECT)")
    RefundAction action;

    PaymentMethod refundMethod; // BANK_TRANSFER, CASH, OTHER... (bắt buộc khi APPROVE)

    @Size(max = 100, message = "Mã giao dịch / mã tham chiếu không quá 100 ký tự")
    String transactionReference; // Mã giao dịch ngân hàng / Mã ủy nhiệm chi / Số phiếu chi

    @Size(max = 500, message = "Ghi chú không quá 500 ký tự")
    String note; // Diễn giải chứng từ kế toán hoặc lý do từ chối
}

