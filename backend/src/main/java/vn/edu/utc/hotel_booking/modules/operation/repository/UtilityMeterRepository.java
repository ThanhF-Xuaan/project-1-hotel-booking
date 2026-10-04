package vn.edu.utc.hotel_booking.modules.operation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.operation.entity.UtilityMeter;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.MeterType;

@Repository
public interface UtilityMeterRepository extends JpaRepository<UtilityMeter, Integer> {

    @Query("SELECT m FROM UtilityMeter m WHERE m.isDeleted = false " +
           "AND (:hotelId IS NULL OR m.hotel.id = :hotelId) " +
           "AND (:meterType IS NULL OR m.meterType = :meterType) " +
           "AND (:keyword IS NULL OR LOWER(m.meterCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<UtilityMeter> search(@Param("hotelId") Short hotelId, 
                              @Param("meterType") MeterType meterType, 
                              @Param("keyword") String keyword, 
                              Pageable pageable);
    
    boolean existsByHotelIdAndMeterCodeAndIsDeletedFalse(Short hotelId, String meterCode);
}
