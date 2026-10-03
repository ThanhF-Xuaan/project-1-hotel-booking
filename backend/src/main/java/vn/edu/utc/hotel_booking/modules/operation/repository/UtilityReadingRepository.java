package vn.edu.utc.hotel_booking.modules.operation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.operation.entity.UtilityReading;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface UtilityReadingRepository extends JpaRepository<UtilityReading, Long> {

    @Query("SELECT r FROM UtilityReading r WHERE r.isDeleted = false " +
           "AND (:meterId IS NULL OR r.meter.id = :meterId) " +
           "AND (cast(:fromDate as date) IS NULL OR r.readingDate >= :fromDate) " +
           "AND (cast(:toDate as date) IS NULL OR r.readingDate <= :toDate)")
    Page<UtilityReading> search(@Param("meterId") Integer meterId, 
                                @Param("fromDate") LocalDate fromDate, 
                                @Param("toDate") LocalDate toDate, 
                                Pageable pageable);
                                
    boolean existsByMeterIdAndReadingDateAndIsDeletedFalse(Integer meterId, LocalDate readingDate);
    
    @Query("SELECT r FROM UtilityReading r WHERE r.meter.id = :meterId AND r.readingDate < :readingDate AND r.isDeleted = false ORDER BY r.readingDate DESC LIMIT 1")
    Optional<UtilityReading> findPreviousReading(@Param("meterId") Integer meterId, @Param("readingDate") LocalDate readingDate);
}
