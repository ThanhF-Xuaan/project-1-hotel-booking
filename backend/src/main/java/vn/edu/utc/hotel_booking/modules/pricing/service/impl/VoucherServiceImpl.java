package vn.edu.utc.hotel_booking.modules.pricing.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.modules.identity.entity.BookingGuest;
import vn.edu.utc.hotel_booking.modules.pricing.entity.Voucher;
import vn.edu.utc.hotel_booking.modules.pricing.entity.VoucherUsage;
import vn.edu.utc.hotel_booking.modules.pricing.repository.VoucherRepository;
import vn.edu.utc.hotel_booking.modules.pricing.repository.VoucherUsageRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.VoucherService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VoucherServiceImpl implements VoucherService {

    VoucherRepository voucherRepository;
    VoucherUsageRepository voucherUsageRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Voucher> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return voucherRepository.findByVoucherCodeAndIsDeletedFalse(code.trim());
    }

    @Override
    @Transactional
    public void recordVoucherUsage(String voucherCode, BookingGuest guest, BigDecimal discountAmount) {
        if (voucherCode == null || voucherCode.isBlank()) {
            return;
        }
        voucherRepository.findByVoucherCodeAndIsDeletedFalse(voucherCode.trim()).ifPresent(v -> {
            v.setUsedQuantity(v.getUsedQuantity() != null ? v.getUsedQuantity() + 1 : 1);
            if (v.getTotalQuantity() != null && v.getUsedQuantity() >= v.getTotalQuantity()) {
                v.setStatus("EXPIRED");
            }
            voucherRepository.save(v);

            VoucherUsage usage = VoucherUsage.builder()
                    .voucher(v)
                    .bookingGuest(guest)
                    .discountAmount(discountAmount != null ? discountAmount : BigDecimal.ZERO)
                    .usedAt(OffsetDateTime.now())
                    .build();
            voucherUsageRepository.save(usage);
            log.info("Recorded voucher usage for code {} by guest ID {}", voucherCode, guest != null ? guest.getId() : "null");
        });
    }
}
