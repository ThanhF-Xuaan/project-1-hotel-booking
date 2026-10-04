package vn.edu.utc.hotel_booking.modules.operation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.operation.entity.UtilityReading;

import java.time.LocalDate;
import java.util.List;
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
    
    Optional<UtilityReading> findFirstByMeterIdAndReadingDateLessThanAndIsDeletedFalseOrderByReadingDateDesc(Integer meterId, LocalDate readingDate);
    
    default Optional<UtilityReading> findPreviousReading(Integer meterId, LocalDate readingDate) {
        return findFirstByMeterIdAndReadingDateLessThanAndIsDeletedFalseOrderByReadingDateDesc(meterId, readingDate);
    }

    Optional<UtilityReading> findFirstByMeterIdAndReadingDateGreaterThanAndIsDeletedFalseOrderByReadingDateAsc(Integer meterId, LocalDate readingDate);

    default Optional<UtilityReading> findNextReading(Integer meterId, LocalDate readingDate) {
        return findFirstByMeterIdAndReadingDateGreaterThanAndIsDeletedFalseOrderByReadingDateAsc(meterId, readingDate);
    }

    @Query(value = "SELECT r.id AS reading_id, prev.reading_value AS previous_value " +
           "FROM utility_readings r " +
           "LEFT JOIN LATERAL (" +
           "  SELECT p.reading_value FROM utility_readings p " +
           "  WHERE p.meter_id = r.meter_id AND p.reading_date < r.reading_date AND p.is_deleted = false " +
           "  ORDER BY p.reading_date DESC LIMIT 1" +
           ") prev ON true " +
           "WHERE r.id IN :readingIds", nativeQuery = true)
    List<Object[]> findPreviousValues(@Param("readingIds") List<Long> readingIds);
}
