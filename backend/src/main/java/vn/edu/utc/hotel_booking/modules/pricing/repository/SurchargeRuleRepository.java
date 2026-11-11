package vn.edu.utc.hotel_booking.modules.pricing.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.pricing.entity.SurchargeRule;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SurchargeRuleRepository extends JpaRepository<SurchargeRule, Integer> {

    @Query("SELECT sr FROM SurchargeRule sr " +
            "JOIN FETCH sr.hotelRoomType hrt " +
            "JOIN FETCH hrt.roomType rt " +
            "JOIN FETCH hrt.hotel h " +
            "LEFT JOIN FETCH sr.agePolicy ap " +
            "WHERE sr.id = :id AND sr.isDeleted = false")
    Optional<SurchargeRule> findByIdWithDetails(@Param("id") Integer id);

    Optional<SurchargeRule> findByIdAndIsDeletedFalse(Integer id);

    @Query(value = "SELECT sr FROM SurchargeRule sr " +
            "JOIN FETCH sr.hotelRoomType hrt " +
            "JOIN FETCH hrt.roomType rt " +
            "JOIN FETCH hrt.hotel h " +
            "LEFT JOIN FETCH sr.agePolicy ap " +
            "WHERE sr.isDeleted = false " +
            "AND (:hotelId IS NULL OR h.id = :hotelId) " +
            "AND (:hotelRoomTypeId IS NULL OR hrt.id = :hotelRoomTypeId) " +
            "AND (:agePolicyId IS NULL OR (ap IS NOT NULL AND ap.id = :agePolicyId)) " +
            "AND (:ruleType IS NULL OR sr.ruleType = :ruleType) " +
            "AND (:pricingType IS NULL OR sr.pricingType = :pricingType) " +
            "AND (:status IS NULL OR sr.status = :status) " +
            "AND (cast(:activeOnDate as date) IS NULL OR (sr.startDate <= :activeOnDate AND sr.endDate >= :activeOnDate))",
          countQuery = "SELECT COUNT(sr) FROM SurchargeRule sr " +
            "JOIN sr.hotelRoomType hrt " +
            "JOIN hrt.hotel h " +
            "LEFT JOIN sr.agePolicy ap " +
            "WHERE sr.isDeleted = false " +
            "AND (:hotelId IS NULL OR h.id = :hotelId) " +
            "AND (:hotelRoomTypeId IS NULL OR hrt.id = :hotelRoomTypeId) " +
            "AND (:agePolicyId IS NULL OR (ap IS NOT NULL AND ap.id = :agePolicyId)) " +
            "AND (:ruleType IS NULL OR sr.ruleType = :ruleType) " +
            "AND (:pricingType IS NULL OR sr.pricingType = :pricingType) " +
            "AND (:status IS NULL OR sr.status = :status) " +
            "AND (cast(:activeOnDate as date) IS NULL OR (sr.startDate <= :activeOnDate AND sr.endDate >= :activeOnDate))")
    Page<SurchargeRule> filterSurchargeRules(
            @Param("hotelId") Short hotelId,
            @Param("hotelRoomTypeId") Integer hotelRoomTypeId,
            @Param("agePolicyId") Short agePolicyId,
            @Param("ruleType") String ruleType,
            @Param("pricingType") String pricingType,
            @Param("status") String status,
            @Param("activeOnDate") LocalDate activeOnDate,
            Pageable pageable
    );

    @Query("SELECT COUNT(sr) > 0 FROM SurchargeRule sr " +
            "WHERE sr.isDeleted = false " +
            "AND (:excludeId IS NULL OR sr.id != :excludeId) " +
            "AND sr.hotelRoomType.id = :hotelRoomTypeId " +
            "AND sr.ruleType = :ruleType " +
            "AND ((sr.agePolicy IS NULL AND :agePolicyId IS NULL) OR (sr.agePolicy.id = :agePolicyId)) " +
            "AND sr.startDate <= :endDate AND sr.endDate >= :startDate")
    boolean existsOverlappingRule(
            @Param("excludeId") Integer excludeId,
            @Param("hotelRoomTypeId") Integer hotelRoomTypeId,
            @Param("ruleType") String ruleType,
            @Param("agePolicyId") Short agePolicyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Modifying
    @Query("UPDATE SurchargeRule sr SET sr.isDeleted = true WHERE sr.id IN :ids")
    void softDeleteByIds(@Param("ids") List<Integer> ids);

    @Query("SELECT sr FROM SurchargeRule sr " +
            "LEFT JOIN FETCH sr.agePolicy ap " +
            "WHERE sr.hotelRoomType.id = :hotelRoomTypeId " +
            "AND sr.status = 'ACTIVE' " +
            "AND sr.isDeleted = false " +
            "AND sr.startDate <= :endDate " +
            "AND sr.endDate >= :startDate")
    List<SurchargeRule> findActiveSurchargesForRoomType(
            @Param("hotelRoomTypeId") Integer hotelRoomTypeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT sr FROM SurchargeRule sr " +
            "WHERE sr.hotelRoomType.id = :hotelRoomTypeId " +
            "AND sr.ruleType = :ruleType " +
            "AND sr.status = 'ACTIVE' " +
            "AND sr.isDeleted = false " +
            "AND sr.startDate <= :date " +
            "AND sr.endDate >= :date")
    Optional<SurchargeRule> findActiveRuleForRoomTypeAndDate(
            @Param("hotelRoomTypeId") Integer hotelRoomTypeId,
            @Param("ruleType") String ruleType,
            @Param("date") LocalDate date
    );
}
