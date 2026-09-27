package vn.edu.utc.hotel_booking.modules.identity.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.identity.entity.Role;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Short> {

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.id = :id AND r.isDeleted = false")
    Optional<Role> findByIdWithPermissions(@Param("id") Short id);

    Optional<Role> findByIdAndIsDeletedFalse(Short id);

    Optional<Role> findByCodeAndIsDeletedFalse(String code);

    boolean existsByCodeAndIsDeletedFalse(String code);

    boolean existsByCodeAndIdNotAndIsDeletedFalse(String code, Short id);

    @Query("SELECT r FROM Role r WHERE r.isDeleted = false " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:keyword IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "    OR LOWER(r.code) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Role> searchRoles(@Param("keyword") String keyword,
                           @Param("status") String status,
                           Pageable pageable);

    @Modifying
    @Query("UPDATE Role r SET r.isDeleted = true, r.updatedAt = CURRENT_TIMESTAMP WHERE r.id IN :ids")
    int softDeleteBatch(@Param("ids") List<Short> ids);
}
