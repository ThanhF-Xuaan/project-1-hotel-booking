package vn.edu.utc.hotel_booking.modules.operation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.operation.entity.LodgingQueue;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.LodgingQueueStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LodgingQueueRepository extends JpaRepository<LodgingQueue, Long> {

    Optional<LodgingQueue> findByStayGuestIdAndIsDeletedFalse(Long stayGuestId);

    @Query("SELECT q FROM LodgingQueue q WHERE q.isDeleted = false " +
           "AND (:hotelId IS NULL OR q.hotel.id = :hotelId) " +
           "AND (:status IS NULL OR q.status = :status) " +
           "AND (cast(:fromDate as timestamp with time zone) IS NULL OR q.createdAt >= :fromDate) " +
           "AND (cast(:toDate as timestamp with time zone) IS NULL OR q.createdAt <= :toDate)")
    Page<LodgingQueue> search(@Param("hotelId") Short hotelId,
                              @Param("status") LodgingQueueStatus status,
                              @Param("fromDate") OffsetDateTime fromDate,
                              @Param("toDate") OffsetDateTime toDate,
                              Pageable pageable);

    @Query("SELECT q FROM LodgingQueue q " +
           "JOIN FETCH q.stayGuest g " +
           "WHERE q.isDeleted = false " +
           "AND q.hotel.id = :hotelId " +
           "AND q.status = vn.edu.utc.hotel_booking.modules.operation.entity.enums.LodgingQueueStatus.PENDING " +
           "AND g.checkInTime <= :cutoffTime " +
           "AND (:isVietnamese = true AND (LOWER(g.nationality) = 'việt nam' OR LOWER(g.nationality) = 'viet nam' OR LOWER(g.nationality) = 'vietnam') " +
           "     OR :isVietnamese = false AND LOWER(g.nationality) != 'việt nam' AND LOWER(g.nationality) != 'viet nam' AND LOWER(g.nationality) != 'vietnam') " +
           "ORDER BY g.checkInTime ASC")
    List<LodgingQueue> findPendingForExport(@Param("hotelId") Short hotelId,
                                            @Param("cutoffTime") OffsetDateTime cutoffTime,
                                            @Param("isVietnamese") boolean isVietnamese);
}
