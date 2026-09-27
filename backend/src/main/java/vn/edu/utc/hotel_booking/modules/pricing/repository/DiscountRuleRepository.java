package vn.edu.utc.hotel_booking.modules.pricing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.pricing.entity.DiscountRule;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DiscountRuleRepository extends JpaRepository<DiscountRule, Integer> {

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
}
