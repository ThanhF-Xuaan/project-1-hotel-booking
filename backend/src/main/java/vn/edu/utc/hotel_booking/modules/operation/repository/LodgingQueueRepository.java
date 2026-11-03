package vn.edu.utc.hotel_booking.modules.operation.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LodgingQueue> findByStayGuestIdAndIsDeletedFalse(Long stayGuestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM LodgingQueue q WHERE q.id = :id")
    Optional<LodgingQueue> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT q FROM LodgingQueue q WHERE q.isDeleted = false " +
           "AND (:hotelId IS NULL OR q.hotel.id = :hotelId) " +
           "AND (:status IS NULL OR q.status = :status) " +
           "AND (CAST(:fromDate AS java.time.OffsetDateTime) IS NULL OR q.createdAt >= :fromDate) " +
           "AND (CAST(:toDate AS java.time.OffsetDateTime) IS NULL OR q.createdAt <= :toDate)")
    Page<LodgingQueue> search(@Param("hotelId") Short hotelId,
                              @Param("status") LodgingQueueStatus status,
                              @Param("fromDate") OffsetDateTime fromDate,
                              @Param("toDate") OffsetDateTime toDate,
                              Pageable pageable);

    @Query(value = "SELECT q.* FROM lodging_queues q WHERE q.is_deleted = false AND q.hotel_id = :hotelId " +
           "AND q.status = 'PENDING' AND EXISTS (SELECT 1 FROM stay_guests g WHERE g.id=q.stay_guest_id " +
           "AND g.check_in_time < :cutoffTime AND ((:isVietnamese = true AND LOWER(g.nationality) IN ('việt nam','viet nam','vietnam')) " +
           "OR (:isVietnamese = false AND LOWER(g.nationality) NOT IN ('việt nam','viet nam','vietnam')))) " +
           "ORDER BY q.id FOR UPDATE OF q", nativeQuery = true)
    List<LodgingQueue> findPendingForExport(@Param("hotelId") Short hotelId,
                                            @Param("cutoffTime") OffsetDateTime cutoffTime,
                                            @Param("isVietnamese") boolean isVietnamese);
}
