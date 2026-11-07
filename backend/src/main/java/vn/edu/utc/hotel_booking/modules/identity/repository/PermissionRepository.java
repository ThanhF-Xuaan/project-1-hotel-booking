package vn.edu.utc.hotel_booking.modules.identity.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    @Query("SELECT p FROM Permission p WHERE p.isDeleted = false " +
            "AND (:status IS NULL OR p.status = :status) " +
            "AND (:resource IS NULL OR p.resource = :resource) " +
            "AND (:action IS NULL OR p.action = :action) " +
            "AND (:keyword IS NULL OR LOWER(p.resource) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "    OR LOWER(p.action) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Permission> searchPermissions(@Param("keyword") String keyword,
                                       @Param("resource") String resource,
                                       @Param("action") String action,
                                       @Param("status") String status,
                                       Pageable pageable);
}
