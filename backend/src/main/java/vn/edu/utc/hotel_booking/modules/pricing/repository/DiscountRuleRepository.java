package vn.edu.utc.hotel_booking.modules.pricing.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.pricing.entity.DiscountRule;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiscountRuleRepository extends JpaRepository<DiscountRule, Integer> {

    @Query("SELECT dr FROM DiscountRule dr " +
            "JOIN FETCH dr.hotelRoomType hrt " +
            "JOIN FETCH hrt.roomType rt " +
            "JOIN FETCH dr.ruleType drt " +
            "LEFT JOIN FETCH dr.campaign c " +
            "WHERE dr.id = :id AND dr.isDeleted = false")
    Optional<DiscountRule> findByIdWithDetails(@Param("id") Integer id);

    Optional<DiscountRule> findByIdAndIsDeletedFalse(Integer id);

    boolean existsByRuleTypeCodeAndIsDeletedFalse(String ruleTypeCode);

    @Query("SELECT dr FROM DiscountRule dr " +
            "JOIN FETCH dr.ruleType drt " +
            "LEFT JOIN FETCH dr.campaign c " +
            "WHERE dr.hotelRoomType.id = :hotelRoomTypeId " +
            "AND dr.status = 'ACTIVE' " +
            "AND dr.isDeleted = false " +
            "AND dr.startDate <= :endDate " +
            "AND dr.endDate >= :startDate")
    List<DiscountRule> findActiveDiscountsForRoomType(
            @Param("hotelRoomTypeId") Integer hotelRoomTypeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT dr FROM DiscountRule dr " +
            "JOIN FETCH dr.hotelRoomType hrt " +
            "JOIN FETCH hrt.roomType rt " +
            "JOIN FETCH dr.ruleType drt " +
            "LEFT JOIN FETCH dr.campaign c " +
            "WHERE dr.isDeleted = false " +
            "AND (:hotelRoomTypeId IS NULL OR dr.hotelRoomType.id = :hotelRoomTypeId) " +
            "AND (:campaignId IS NULL OR dr.campaign.id = :campaignId) " +
            "AND (:ruleTypeCode IS NULL OR dr.ruleType.code = :ruleTypeCode) " +
            "AND (:status IS NULL OR dr.status = :status) " +
            "AND (:activeOnDate IS NULL OR (dr.startDate <= :activeOnDate AND dr.endDate >= :activeOnDate))")
    Page<DiscountRule> filterDiscountRules(
            @Param("hotelRoomTypeId") Integer hotelRoomTypeId,
            @Param("campaignId") Integer campaignId,
            @Param("ruleTypeCode") String ruleTypeCode,
            @Param("status") String status,
            @Param("activeOnDate") LocalDate activeOnDate,
            Pageable pageable
    );

    @Modifying
    @Query("UPDATE DiscountRule dr SET dr.isDeleted = true WHERE dr.id IN :ids")
    void softDeleteByIds(@Param("ids") List<Integer> ids);
}
