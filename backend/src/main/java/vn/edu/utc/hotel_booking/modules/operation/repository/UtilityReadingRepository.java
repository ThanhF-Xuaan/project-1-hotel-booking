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

    @Query(value = "SELECT r FROM UtilityReading r " +
                   "JOIN FETCH r.meter m " +
                   "JOIN FETCH m.hotel h " +
                   "WHERE r.isDeleted = false " +
                   "AND (:meterId IS NULL OR m.id = :meterId) " +
                   "AND (:hotelId IS NULL OR h.id = :hotelId) " +
                   "AND (:regionId IS NULL OR h.region.id = :regionId) " +
                   "AND (cast(:fromDate as date) IS NULL OR r.readingDate >= :fromDate) " +
                   "AND (cast(:toDate as date) IS NULL OR r.readingDate <= :toDate)",
           countQuery = "SELECT count(r) FROM UtilityReading r " +
                        "WHERE r.isDeleted = false " +
                        "AND (:meterId IS NULL OR r.meter.id = :meterId) " +
                        "AND (:hotelId IS NULL OR r.meter.hotel.id = :hotelId) " +
                        "AND (:regionId IS NULL OR r.meter.hotel.region.id = :regionId) " +
                        "AND (cast(:fromDate as date) IS NULL OR r.readingDate >= :fromDate) " +
                        "AND (cast(:toDate as date) IS NULL OR r.readingDate <= :toDate)")
    Page<UtilityReading> search(@Param("meterId") Integer meterId, 
                                @Param("hotelId") Short hotelId,
                                @Param("regionId") Short regionId,
                                @Param("fromDate") LocalDate fromDate, 
                                @Param("toDate") LocalDate toDate, 
                                Pageable pageable);

    @Query("SELECT r FROM UtilityReading r JOIN FETCH r.meter m JOIN FETCH m.hotel h JOIN FETCH h.region WHERE r.id = :id AND r.isDeleted = false")
    Optional<UtilityReading> findByIdWithMeterAndHotelAndRegion(@Param("id") Long id);

    @Query("SELECT r FROM UtilityReading r JOIN FETCH r.meter m JOIN FETCH m.hotel h WHERE r.id IN :ids")
    List<UtilityReading> findAllByIdInWithMeterAndHotel(@Param("ids") List<Long> ids);
                                
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
