package vn.edu.utc.hotel_booking.modules.organization.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.util.List;
import java.util.Optional;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Short> {

    @Query("SELECT h FROM Hotel h JOIN FETCH h.region WHERE h.id = :id AND h.isDeleted = false")
    Optional<Hotel> findByIdWithRegion(@Param("id") Short id);

    Optional<Hotel> findByIdAndIsDeletedFalse(Short id);

    boolean existsByNameAndRegionIdAndIsDeletedFalse(String name, Short regionId);

    boolean existsByNameAndRegionIdAndIdNotAndIsDeletedFalse(String name, Short regionId, Short id);

    @Query(value = "SELECT h FROM Hotel h JOIN FETCH h.region r WHERE h.isDeleted = false " +
            "AND (:regionId IS NULL OR r.id = :regionId) " +
            "AND (:status IS NULL OR h.status = :status) " +
            "AND (:keyword IS NULL OR LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "    OR LOWER(h.address) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "    OR h.phone LIKE CONCAT('%', :keyword, '%'))",
            countQuery = "SELECT COUNT(h) FROM Hotel h WHERE h.isDeleted = false " +
            "AND (:regionId IS NULL OR h.region.id = :regionId) " +
            "AND (:status IS NULL OR h.status = :status) " +
            "AND (:keyword IS NULL OR LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "    OR LOWER(h.address) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "    OR h.phone LIKE CONCAT('%', :keyword, '%'))")
    Page<Hotel> searchHotels(@Param("regionId") Short regionId,
                             @Param("keyword") String keyword,
                             @Param("status") String status,
                             Pageable pageable);

    @Modifying
    @Query("UPDATE Hotel h SET h.isDeleted = true, h.updatedAt = CURRENT_TIMESTAMP WHERE h.id IN :ids")
    int softDeleteBatch(@Param("ids") List<Short> ids);
}
