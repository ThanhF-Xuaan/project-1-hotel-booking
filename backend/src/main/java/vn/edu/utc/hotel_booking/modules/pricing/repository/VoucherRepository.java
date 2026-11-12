package vn.edu.utc.hotel_booking.modules.pricing.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.pricing.entity.Voucher;

import java.time.OffsetDateTime;
import java.util.List;
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

    Optional<Voucher> findByIdAndIsDeletedFalse(Integer id);

    boolean existsByVoucherCodeAndIsDeletedFalse(String voucherCode);

    @Query("SELECT v FROM Voucher v WHERE v.isDeleted = false " +
            "AND (:keyword IS NULL OR LOWER(v.voucherCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(v.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:hotelId IS NULL OR v.hotelId = :hotelId) " +
            "AND (:discountType IS NULL OR v.discountType = :discountType) " +
            "AND (:applyScope IS NULL OR v.applyScope = :applyScope) " +
            "AND (:status IS NULL OR v.status = :status) " +
            "AND (:activeAt IS NULL OR (v.validFrom <= :activeAt AND v.validTo >= :activeAt))")
    Page<Voucher> filterVouchers(
            @Param("keyword") String keyword,
            @Param("hotelId") Short hotelId,
            @Param("discountType") String discountType,
            @Param("applyScope") String applyScope,
            @Param("status") String status,
            @Param("activeAt") OffsetDateTime activeAt,
            Pageable pageable
    );

    @Modifying
    @Query("UPDATE Voucher v SET v.isDeleted = true, v.updatedAt = CURRENT_TIMESTAMP WHERE v.id IN :ids")
    void softDeleteByIds(@Param("ids") List<Integer> ids);
}
