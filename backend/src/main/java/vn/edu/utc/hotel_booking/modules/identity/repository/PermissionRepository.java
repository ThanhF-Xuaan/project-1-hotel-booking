package vn.edu.utc.hotel_booking.modules.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
