package vn.edu.utc.hotel_booking.modules.pricing.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.pricing.entity.VatRule;

import java.time.LocalDate;
import java.util.List;
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

    Optional<VatRule> findByIdAndIsDeletedFalse(Integer id);

    boolean existsByVatCodeAndIsDeletedFalse(String vatCode);

    boolean existsByTaxCategoryIdAndIsDeletedFalse(Integer taxCategoryId);

    @Query("SELECT vr FROM VatRule vr JOIN FETCH vr.taxCategory tc WHERE vr.isDeleted = false " +
            "AND (:taxCategoryId IS NULL OR tc.id = :taxCategoryId) " +
            "AND (:keyword IS NULL OR :keyword = '' " +
            "     OR LOWER(vr.vatCode) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR LOWER(vr.vatName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR :status = '' OR vr.status = :status) " +
            "AND (:activeOnDate IS NULL OR (vr.startDate <= :activeOnDate AND (vr.endDate IS NULL OR vr.endDate >= :activeOnDate)))")
    Page<VatRule> filter(
            @Param("taxCategoryId") Integer taxCategoryId,
            @Param("keyword") String keyword,
            @Param("activeOnDate") LocalDate activeOnDate,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("SELECT vr FROM VatRule vr WHERE vr.taxCategory.id = :taxCategoryId " +
            "AND vr.isDeleted = false AND vr.status = 'ACTIVE' " +
            "AND (:excludeId IS NULL OR vr.id != :excludeId) " +
            "AND (vr.startDate <= :endDate OR :endDate IS NULL) " +
            "AND (vr.endDate IS NULL OR vr.endDate >= :startDate)")
    List<VatRule> findOverlappingActiveRules(
            @Param("taxCategoryId") Integer taxCategoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") Integer excludeId
    );
}
