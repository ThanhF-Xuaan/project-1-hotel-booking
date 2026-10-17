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

        return bookingChargeRepository.save(charge);
    }
}
