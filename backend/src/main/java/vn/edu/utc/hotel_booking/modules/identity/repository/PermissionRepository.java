package vn.edu.utc.hotel_booking.modules.identity.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.identity.entity.Permission;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Short> {

    Optional<Permission> findByIdAndIsDeletedFalse(Short id);

    List<Permission> findByIdInAndIsDeletedFalse(Collection<Short> ids);

    List<Permission> findAllByIsDeletedFalse();

    boolean existsByActionAndResourceAndIsDeletedFalse(String action, String resource);

    boolean existsByActionAndResourceAndIdNotAndIsDeletedFalse(String action, String resource, Short id);

    @Query("SELECT COUNT(rp) > 0 FROM Role r JOIN r.permissions rp WHERE rp.id IN :permissionIds AND r.isDeleted = false")
    boolean hasActiveRolesUsingPermissions(@Param("permissionIds") List<Short> permissionIds);

    @Modifying
    @Query("UPDATE Permission p SET p.isDeleted = true, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id IN :ids")
    int softDeleteBatch(@Param("ids") List<Short> ids);

    @Query("SELECT DISTINCT p.resource FROM Permission p WHERE p.isDeleted = false ORDER BY p.resource ASC")
    List<String> findDistinctResources();

    @Query("SELECT DISTINCT p.action FROM Permission p WHERE p.isDeleted = false ORDER BY p.action ASC")
    List<String> findDistinctActions();

    @Query("SELECT p FROM Permission p WHERE p.isDeleted = false " +
            "AND (:status IS NULL OR :status = '' OR p.status = :status) " +
            "AND (:resource IS NULL OR :resource = '' OR p.resource = :resource) " +
            "AND (:action IS NULL OR :action = '' OR p.action = :action) " +
            "AND (:keyword IS NULL OR :keyword = '' OR LOWER(p.resource) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "    OR LOWER(p.action) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Permission> searchPermissions(@Param("keyword") String keyword,
                                       @Param("resource") String resource,
                                       @Param("action") String action,
                                       @Param("status") String status,
                                       Pageable pageable);
}
