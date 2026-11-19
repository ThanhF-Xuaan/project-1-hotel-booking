package vn.edu.utc.hotel_booking.modules.finance.service;

import vn.edu.utc.hotel_booking.modules.booking.dto.response.CheckoutSettlementResponse;
import vn.edu.utc.hotel_booking.modules.finance.entity.Folio;
import vn.edu.utc.hotel_booking.modules.finance.entity.FolioStatus;
import vn.edu.utc.hotel_booking.modules.finance.entity.FolioTransaction;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Service quản lý Sổ cái Bút toán kép (Double-Entry Folio Ledger) theo chuẩn Opera PMS.
 * Chịu trách nhiệm ghi nhận các bút toán Nợ (DEBIT - chi phí phòng, dịch vụ, phụ thu)
 * và Có (CREDIT - thanh toán, cọc, hoàn tiền), tính toán số dư thời gian thực.
 */
public interface FolioLedgerService {

    /**
     * Lấy hoặc tạo mới Folio cho một booking hoặc phòng cụ thể.
     */
    Folio getOrCreateFolio(Long bookingId, Long bookingRoomId);

    /**
     * Tra cứu Folio theo bookingRoomId
     */
    Optional<Folio> findFolioByBookingRoomId(Long bookingRoomId);

    /**
     * Tra cứu Folio theo bookingId
     */
    Optional<Folio> findFolioByBookingId(Long bookingId);

    /**
     * Ghi nhận bút toán Nợ (DEBIT) - Chi phí phát sinh trong kỳ lưu trú.
     */
    FolioTransaction postDebit(
            Long folioId,
            String category,
            String sourceType,
            Long sourceId,
            String description,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal,
            BigDecimal serviceFeeRate,
            BigDecimal serviceFeeAmount,
            BigDecimal vatRate,
            BigDecimal vatAmount,
            BigDecimal totalAmount
    );

    /**
     * Bút toán Nợ đơn giản với tổng tiền gộp.
     */
    default FolioTransaction postDebit(
            Long folioId,
            String category,
            String sourceType,
            Long sourceId,
            String description,
            BigDecimal totalAmount
    ) {
        return postDebit(
                folioId, category, sourceType, sourceId, description,
                1, totalAmount, totalAmount,
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                totalAmount
        );
    }

    /**
     * Ghi nhận bút toán Có (CREDIT) - Khoản thanh toán hoặc hoàn tiền thành công.
     */
    FolioTransaction postCredit(
            Long folioId,
            String category,
            String sourceType,
            Long sourceId,
            String description,
            BigDecimal amount,
            String paymentMethod
    );

    /**
     * Bút toán Có đơn giản cho các khoản điều chỉnh/hủy bỏ.
     */
    default FolioTransaction postCredit(
            Long folioId,
            String category,
            String sourceType,
            Long sourceId,
            String description,
            BigDecimal amount
    ) {
        return postCredit(folioId, category, sourceType, sourceId, description, amount, "ADJUSTMENT");
    }

    /**
     * Tái tính toán tổng số Nợ (total_debits), Có (total_credits) và số dư nợ (balance).
     */
    void recalculateBalance(Long folioId);

    /**
     * Lấy bảng quyết toán chi phí và công nợ Check-out cho phòng lưu trú.
     */
    CheckoutSettlementResponse getSettlementByBookingRoomId(Long bookingRoomId);

    /**
     * Cập nhật trạng thái của Folio (OPEN, LOCKED_FOR_SETTLEMENT, SETTLED, CLOSED, VOID).
     */
    void updateFolioStatus(Long folioId, FolioStatus status);

    /**
     * Đóng toàn bộ Folio liên quan đến booking khi hoàn tất thủ tục Check-out.
     */
    void closeFolio(Long bookingId);

    /**
     * Lấy tổng chi phí phát sinh (Total Debits) của toàn bộ các Folio thuộc booking.
     */
    BigDecimal getTotalDebitsByBookingId(Long bookingId);

    /**
     * Lấy số dư nợ còn lại (Balance Due) của toàn bộ các Folio thuộc booking.
     */
    BigDecimal getRemainingBalanceByBookingId(Long bookingId);
}

