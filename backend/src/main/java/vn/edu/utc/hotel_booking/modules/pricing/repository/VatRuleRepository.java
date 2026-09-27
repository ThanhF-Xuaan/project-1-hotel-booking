package vn.edu.utc.hotel_booking.modules.pricing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.pricing.entity.VatRule;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface VatRuleRepository extends JpaRepository<VatRule, Integer> {

    @Query("SELECT vr FROM VatRule vr " +
            "WHERE vr.taxCategory.id = :taxCategoryId " +
            "AND vr.status = 'ACTIVE' " +
            "AND vr.isDeleted = false " +
            "AND vr.startDate <= :date " +
            "AND (vr.endDate IS NULL OR vr.endDate >= :date) " +
            "ORDER BY vr.startDate DESC")
    Optional<VatRule> findActiveVatRule(
            @Param("taxCategoryId") Integer taxCategoryId,
            @Param("date") LocalDate date
    );
}
