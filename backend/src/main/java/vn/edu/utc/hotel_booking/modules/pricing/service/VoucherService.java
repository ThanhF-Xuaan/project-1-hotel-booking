package vn.edu.utc.hotel_booking.modules.pricing.service;

import vn.edu.utc.hotel_booking.modules.identity.entity.BookingGuest;
import vn.edu.utc.hotel_booking.modules.pricing.entity.Voucher;

import java.math.BigDecimal;
import java.util.Optional;

public interface VoucherService {

    Optional<Voucher> findByCode(String code);

    void recordVoucherUsage(String voucherCode, BookingGuest guest, BigDecimal discountAmount);
}
