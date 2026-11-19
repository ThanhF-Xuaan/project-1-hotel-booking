package vn.edu.utc.hotel_booking.modules.booking.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.BookingChargeCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingCharge;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingChargeRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingChargeService;

import vn.edu.utc.hotel_booking.modules.finance.entity.Folio;
import vn.edu.utc.hotel_booking.modules.finance.service.FolioLedgerService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingChargeServiceImpl implements BookingChargeService {

    BookingChargeRepository bookingChargeRepository;
    FolioLedgerService folioLedgerService;

    @Override
    @Transactional
    public BookingCharge save(BookingCharge charge) {
        return bookingChargeRepository.save(charge);
    }

    @Override
    @Transactional
    public List<BookingCharge> saveAll(List<BookingCharge> charges) {
        return bookingChargeRepository.saveAll(charges);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingCharge> findByBookingRoomId(Long bookingRoomId) {
        return bookingChargeRepository.findByBookingRoomId(bookingRoomId);
    }

    @Override
    @Transactional
    public BookingCharge createCharge(BookingRoom bookingRoom, BookingChargeCreateRequest request) {
        BigDecimal unitPrice = request.getUnitPrice() != null ? request.getUnitPrice() : BigDecimal.ZERO;
        int qty = request.getQuantity() != null && request.getQuantity() > 0 ? request.getQuantity() : 1;
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(qty));
        BigDecimal serviceFee = subtotal.multiply(request.getServiceFeeRate()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal taxable = subtotal.add(serviceFee);
        BigDecimal vatAmount = taxable.multiply(request.getVatRate()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = taxable.add(vatAmount);

        BookingCharge charge = BookingCharge.builder()
                .bookingRoom(bookingRoom)
                .chargeType(request.getChargeType())
                .itemName(request.getItemName())
                .description(request.getDescription())
                .quantity(qty)
                .unitPrice(unitPrice)
                .subtotal(subtotal)
                .serviceFeeRate(request.getServiceFeeRate())
                .serviceFeeAmount(serviceFee)
                .vatRate(request.getVatRate())
                .vatAmount(vatAmount)
                .totalAmount(totalAmount)
                .issuedAt(OffsetDateTime.now())
                .build();

        BookingCharge saved = bookingChargeRepository.save(charge);

        // Đồng bộ ghi Nợ (DEBIT) vào Folio Ledger
        try {
            Long bookingId = null;
            if (bookingRoom != null && bookingRoom.getBookingDetail() != null && bookingRoom.getBookingDetail().getBooking() != null) {
                bookingId = bookingRoom.getBookingDetail().getBooking().getId();
            }
            if (bookingId != null && bookingRoom != null) {
                Folio folio = folioLedgerService.getOrCreateFolio(bookingId, bookingRoom.getId());
                folioLedgerService.postDebit(
                        folio.getId(),
                        saved.getChargeType() != null ? saved.getChargeType().name() : "SURCHARGE",
                        "BOOKING_CHARGE",
                        saved.getId(),
                        saved.getItemName() != null ? saved.getItemName() : "Phụ thu phòng",
                        saved.getQuantity() != null ? saved.getQuantity() : 1,
                        saved.getUnitPrice(),
                        saved.getSubtotal(),
                        saved.getServiceFeeRate(),
                        saved.getServiceFeeAmount(),
                        saved.getVatRate(),
                        saved.getVatAmount(),
                        saved.getTotalAmount()
                );
            }
        } catch (Exception e) {
            log.error("Failed to post booking charge {} to folio ledger: {}", saved.getId(), e.getMessage());
        }

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public BookingCharge getById(Long id) {
        return bookingChargeRepository.findById(id)
                .orElseThrow(() -> new vn.edu.utc.hotel_booking.common.exception.AppException(
                        vn.edu.utc.hotel_booking.common.exception.ErrorCode.BOOKING_CHARGE_NOT_FOUND,
                        "Không tìm thấy khoản phụ thu ID: " + id));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        BookingCharge charge = getById(id);
        bookingChargeRepository.delete(charge);

        // Hạch toán bút toán điều chỉnh hoàn trả/hủy (ADJUSTMENT CREDIT) vào Folio Ledger
        try {
            BookingRoom br = charge.getBookingRoom();
            Long bookingId = null;
            if (br != null && br.getBookingDetail() != null && br.getBookingDetail().getBooking() != null) {
                bookingId = br.getBookingDetail().getBooking().getId();
            }
            if (bookingId != null && br != null) {
                Folio folio = folioLedgerService.getOrCreateFolio(bookingId, br.getId());
                folioLedgerService.postCredit(
                        folio.getId(),
                        "ADJUSTMENT",
                        "BOOKING_CHARGE_VOID",
                        charge.getId(),
                        "Hủy phụ thu: " + (charge.getItemName() != null ? charge.getItemName() : "Charge #" + charge.getId()),
                        charge.getTotalAmount()
                );
            }
        } catch (Exception e) {
            log.error("Failed to post booking charge void {} to folio ledger: {}", id, e.getMessage());
        }
    }
}
