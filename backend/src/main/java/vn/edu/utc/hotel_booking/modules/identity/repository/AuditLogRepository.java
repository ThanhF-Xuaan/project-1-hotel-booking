package vn.edu.utc.hotel_booking.modules.identity.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.identity.entity.AuditLog;

import java.time.OffsetDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query(value = "SELECT a FROM AuditLog a LEFT JOIN FETCH a.staff WHERE " +
            "(:staffId IS NULL OR a.staff.id = :staffId) " +
            "AND (:actionType IS NULL OR a.actionType = :actionType) " +
            "AND (:entityName IS NULL OR a.entityName = :entityName) " +
            "AND (:entityId IS NULL OR a.entityId = :entityId) " +
            "AND (CAST(:fromDate AS java.time.OffsetDateTime) IS NULL OR a.createdAt >= :fromDate) " +
            "AND (CAST(:toDate AS java.time.OffsetDateTime) IS NULL OR a.createdAt <= :toDate)",
            countQuery = "SELECT COUNT(a) FROM AuditLog a WHERE " +
            "(:staffId IS NULL OR a.staff.id = :staffId) " +
            "AND (:actionType IS NULL OR a.actionType = :actionType) " +
            "AND (:entityName IS NULL OR a.entityName = :entityName) " +
            "AND (:entityId IS NULL OR a.entityId = :entityId) " +
            "AND (CAST(:fromDate AS java.time.OffsetDateTime) IS NULL OR a.createdAt >= :fromDate) " +
            "AND (CAST(:toDate AS java.time.OffsetDateTime) IS NULL OR a.createdAt <= :toDate)")
    Page<AuditLog> searchAuditLogs(@Param("staffId") Integer staffId,
                                   @Param("actionType") String actionType,
                                   @Param("entityName") String entityName,
                                   @Param("entityId") String entityId,
                                   @Param("fromDate") OffsetDateTime fromDate,
                                   @Param("toDate") OffsetDateTime toDate,
                                   Pageable pageable);
}
