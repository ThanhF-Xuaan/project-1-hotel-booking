package vn.edu.utc.hotel_booking.modules.pricing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.pricing.entity.Voucher;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {

    @Query("SELECT v FROM Voucher v WHERE v.voucherCode = :code " +
            "AND v.status = 'ACTIVE' AND v.isDeleted = false " +
            "AND v.validFrom <= :now AND v.validTo >= :now " +
            "AND v.usedQuantity < v.totalQuantity " +
            "AND (v.hotelId IS NULL OR v.hotelId = :hotelId)")
    Optional<Voucher> findValidVoucher(
            @Param("code") String code,
            @Param("hotelId") Short hotelId,
            @Param("now") OffsetDateTime now
    );

    Optional<Voucher> findByVoucherCodeAndIsDeletedFalse(String voucherCode);
}
