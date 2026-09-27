package vn.edu.utc.hotel_booking.modules.identity.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Integer> {

    @Query("SELECT s FROM Staff s LEFT JOIN FETCH s.role LEFT JOIN FETCH s.department WHERE s.id = :id AND s.isDeleted = false")
    Optional<Staff> findByIdWithDetails(@Param("id") Integer id);

    Optional<Staff> findByIdAndIsDeletedFalse(Integer id);

    Optional<Staff> findByKeycloakIdAndIsDeletedFalse(UUID keycloakId);

    Optional<Staff> findByUsernameAndIsDeletedFalse(String username);

    boolean existsByUsernameAndIsDeletedFalse(String username);

    boolean existsByEmailAndIsDeletedFalse(String email);

    boolean existsByPhoneAndIsDeletedFalse(String phone);

    boolean existsByEmailAndIdNotAndIsDeletedFalse(String email, Integer id);

    boolean existsByPhoneAndIdNotAndIsDeletedFalse(String phone, Integer id);

    @Query(value = "SELECT s FROM Staff s LEFT JOIN FETCH s.role r LEFT JOIN FETCH s.department d WHERE s.isDeleted = false " +
            "AND (:roleId IS NULL OR r.id = :roleId) " +
            "AND (:departmentId IS NULL OR d.id = :departmentId) " +
            "AND (:scopeType IS NULL OR s.scopeType = :scopeType) " +
            "AND (:scopeEntityId IS NULL OR s.scopeEntityId = :scopeEntityId) " +
            "AND (:status IS NULL OR s.status = :status) " +
            "AND (:keyword IS NULL OR LOWER(s.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "    OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "    OR LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "    OR s.phone LIKE CONCAT('%', :keyword, '%'))",
            countQuery = "SELECT COUNT(s) FROM Staff s WHERE s.isDeleted = false " +
            "AND (:roleId IS NULL OR s.role.id = :roleId) " +
            "AND (:departmentId IS NULL OR s.department.id = :departmentId) " +
            "AND (:scopeType IS NULL OR s.scopeType = :scopeType) " +
            "AND (:scopeEntityId IS NULL OR s.scopeEntityId = :scopeEntityId) " +
            "AND (:status IS NULL OR s.status = :status) " +
            "AND (:keyword IS NULL OR LOWER(s.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "    OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "    OR LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "    OR s.phone LIKE CONCAT('%', :keyword, '%'))")
    Page<Staff> searchStaffs(@Param("keyword") String keyword,
                             @Param("roleId") Short roleId,
                             @Param("departmentId") Short departmentId,
                             @Param("scopeType") String scopeType,
                             @Param("scopeEntityId") Integer scopeEntityId,
                             @Param("status") String status,
                             Pageable pageable);

    @Modifying
    @Query("UPDATE Staff s SET s.isDeleted = true, s.updatedAt = CURRENT_TIMESTAMP WHERE s.id IN :ids")
    int softDeleteBatch(@Param("ids") List<Integer> ids);
}
