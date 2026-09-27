package vn.edu.utc.hotel_booking.modules.pricing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.pricing.entity.SurchargeRule;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SurchargeRuleRepository extends JpaRepository<SurchargeRule, Integer> {

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
}
