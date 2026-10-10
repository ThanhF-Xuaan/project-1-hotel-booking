package vn.edu.utc.hotel_booking.modules.pricing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.pricing.entity.VoucherUsage;

import java.util.List;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {

    List<VoucherUsage> findByVoucherId(Integer voucherId);

    List<VoucherUsage> findByBookingGuestId(Long bookingGuestId);

    long countByVoucherIdAndBookingGuestId(Integer voucherId, Long bookingGuestId);
}
