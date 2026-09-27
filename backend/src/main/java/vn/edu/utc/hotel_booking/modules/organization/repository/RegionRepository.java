package vn.edu.utc.hotel_booking.modules.organization.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.organization.entity.Region;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegionRepository extends JpaRepository<Region, Short> {

    Optional<Region> findByIdAndIsDeletedFalse(Short id);

    boolean existsByCodeAndIsDeletedFalse(String code);

    boolean existsByCodeAndIdNotAndIsDeletedFalse(String code, Short id);

    @Query("SELECT r FROM Region r WHERE r.isDeleted = false " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:keyword IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "    OR LOWER(r.code) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Region> searchRegions(@Param("keyword") String keyword,
                               @Param("status") String status,
                               Pageable pageable);

    @Modifying
    @Query("UPDATE Region r SET r.isDeleted = true, r.updatedAt = CURRENT_TIMESTAMP WHERE r.id IN :ids")
    int softDeleteBatch(@Param("ids") List<Short> ids);
}
