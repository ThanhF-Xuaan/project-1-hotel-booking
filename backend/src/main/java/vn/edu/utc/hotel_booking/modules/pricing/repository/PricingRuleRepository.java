package vn.edu.utc.hotel_booking.modules.pricing.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.pricing.entity.PricingRule;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, Integer> {

    @Query("SELECT pr FROM PricingRule pr " +
            "JOIN FETCH pr.hotelRoomType hrt " +
            "JOIN FETCH hrt.roomType rt " +
            "JOIN FETCH pr.ruleType prt " +
            "LEFT JOIN FETCH pr.holidayCalendar hc " +
            "WHERE pr.id = :id AND pr.isDeleted = false")
    Optional<PricingRule> findByIdWithDetails(@Param("id") Integer id);

    Optional<PricingRule> findByIdAndIsDeletedFalse(Integer id);

    @Query("SELECT pr FROM PricingRule pr " +
            "JOIN FETCH pr.ruleType prt " +
            "LEFT JOIN FETCH pr.holidayCalendar hc " +
            "WHERE pr.hotelRoomType.id = :hotelRoomTypeId " +
            "AND pr.status = 'ACTIVE' " +
            "AND pr.isDeleted = false " +
            "AND pr.startDate <= :endDate " +
            "AND pr.endDate >= :startDate")
    List<PricingRule> findActiveRulesForRoomType(
            @Param("hotelRoomTypeId") Integer hotelRoomTypeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT pr FROM PricingRule pr " +
            "JOIN FETCH pr.hotelRoomType hrt " +
            "JOIN FETCH hrt.roomType rt " +
            "JOIN FETCH pr.ruleType prt " +
            "WHERE pr.isDeleted = false " +
            "AND (:hotelRoomTypeId IS NULL OR pr.hotelRoomType.id = :hotelRoomTypeId) " +
            "AND (:ruleTypeCode IS NULL OR pr.ruleType.code = :ruleTypeCode) " +
            "AND (:activeOnDate IS NULL OR (pr.startDate <= :activeOnDate AND pr.endDate >= :activeOnDate)) " +
            "AND (:status IS NULL OR pr.status = :status)")
    Page<PricingRule> filterPricingRules(
            @Param("hotelRoomTypeId") Integer hotelRoomTypeId,
            @Param("ruleTypeCode") String ruleTypeCode,
            @Param("activeOnDate") LocalDate activeOnDate,
            @Param("status") String status,
            Pageable pageable
    );

    @Modifying
    @Query("UPDATE PricingRule pr SET pr.isDeleted = true WHERE pr.id IN :ids")
    int softDeleteByIds(@Param("ids") List<Integer> ids);
}
