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

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "DELETE FROM audit_logs WHERE id IN (" +
            "SELECT id FROM audit_logs ORDER BY created_at DESC, id DESC OFFSET 50" +
            ")", nativeQuery = true)
    int purgeOldAuditLogsExceedingLimit();

    @Query(value = "SELECT a FROM AuditLog a LEFT JOIN FETCH a.staff s WHERE " +
            "(:staffId IS NULL OR s.id = :staffId) " +
            "AND (:staffKeyword IS NULL " +
            "     OR LOWER(s.username) LIKE :staffKeyword " +
            "     OR LOWER(s.fullName) LIKE :staffKeyword) " +
            "AND a.actionType != 'LOGIN' AND a.actionType NOT LIKE '%LOGIN%' " +
            "AND (:actionType IS NULL " +
            "     OR a.actionType = :actionType " +
            "     OR (:actionType = 'STATUS_CHANGED' AND (a.actionType = 'STATUS_CHANGED' OR a.actionType = 'STATUS_UPDATE' OR a.actionType = 'STAFF_STATUS_UPDATE' OR a.actionType LIKE '%STATUS%')) " +
            "     OR (:actionType = 'CREATE' AND (a.actionType = 'CREATE' OR a.actionType LIKE '%CREATE%')) " +
            "     OR (:actionType = 'UPDATE' AND (a.actionType = 'UPDATE' OR (a.actionType LIKE '%UPDATE%' AND a.actionType NOT LIKE '%STATUS%'))) " +
            "     OR (:actionType = 'DELETE' AND (a.actionType = 'DELETE' OR a.actionType LIKE '%DELETE%')) " +
            "     OR (:actionType = 'PASSWORD_RESET' AND (a.actionType = 'PASSWORD_RESET' OR a.actionType LIKE '%PASSWORD_RESET%'))) " +
            "AND (:entityName IS NULL OR LOWER(a.entityName) LIKE :entityName) " +
            "AND (:entityId IS NULL OR a.entityId = :entityId) " +
            "AND (CAST(:fromDate AS java.time.OffsetDateTime) IS NULL OR a.createdAt >= :fromDate) " +
            "AND (CAST(:toDate AS java.time.OffsetDateTime) IS NULL OR a.createdAt <= :toDate)",
            countQuery = "SELECT COUNT(a) FROM AuditLog a LEFT JOIN a.staff s WHERE " +
            "(:staffId IS NULL OR s.id = :staffId) " +
            "AND (:staffKeyword IS NULL " +
            "     OR LOWER(s.username) LIKE :staffKeyword " +
            "     OR LOWER(s.fullName) LIKE :staffKeyword) " +
            "AND a.actionType != 'LOGIN' AND a.actionType NOT LIKE '%LOGIN%' " +
            "AND (:actionType IS NULL " +
            "     OR a.actionType = :actionType " +
            "     OR (:actionType = 'STATUS_CHANGED' AND (a.actionType = 'STATUS_CHANGED' OR a.actionType = 'STATUS_UPDATE' OR a.actionType = 'STAFF_STATUS_UPDATE' OR a.actionType LIKE '%STATUS%')) " +
            "     OR (:actionType = 'CREATE' AND (a.actionType = 'CREATE' OR a.actionType LIKE '%CREATE%')) " +
            "     OR (:actionType = 'UPDATE' AND (a.actionType = 'UPDATE' OR (a.actionType LIKE '%UPDATE%' AND a.actionType NOT LIKE '%STATUS%'))) " +
            "     OR (:actionType = 'DELETE' AND (a.actionType = 'DELETE' OR a.actionType LIKE '%DELETE%')) " +
            "     OR (:actionType = 'PASSWORD_RESET' AND (a.actionType = 'PASSWORD_RESET' OR a.actionType LIKE '%PASSWORD_RESET%'))) " +
            "AND (:entityName IS NULL OR LOWER(a.entityName) LIKE :entityName) " +
            "AND (:entityId IS NULL OR a.entityId = :entityId) " +
            "AND (CAST(:fromDate AS java.time.OffsetDateTime) IS NULL OR a.createdAt >= :fromDate) " +
            "AND (CAST(:toDate AS java.time.OffsetDateTime) IS NULL OR a.createdAt <= :toDate)")
    Page<AuditLog> searchAuditLogs(@Param("staffId") Integer staffId,
                                   @Param("staffKeyword") String staffKeyword,
                                   @Param("actionType") String actionType,
                                   @Param("entityName") String entityName,
                                   @Param("entityId") String entityId,
                                   @Param("fromDate") OffsetDateTime fromDate,
                                   @Param("toDate") OffsetDateTime toDate,
                                   Pageable pageable);
}
