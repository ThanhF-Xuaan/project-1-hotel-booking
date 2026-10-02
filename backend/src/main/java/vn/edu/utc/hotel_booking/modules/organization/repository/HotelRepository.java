package vn.edu.utc.hotel_booking.modules.organization.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.util.List;
import java.util.Optional;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Short>, JpaSpecificationExecutor<Hotel> {

    @Query("SELECT h FROM Hotel h JOIN FETCH h.region WHERE h.id = :id AND h.isDeleted = false")
    Optional<Hotel> findByIdWithRegion(@Param("id") Short id);

    Optional<Hotel> findByIdAndIsDeletedFalse(Short id);

    boolean existsByNameAndRegionIdAndIsDeletedFalse(String name, Short regionId);

    boolean existsByNameAndRegionIdAndIdNotAndIsDeletedFalse(String name, Short regionId, Short id);

    @Modifying
    @Query("UPDATE Hotel h SET h.isDeleted = true, h.updatedAt = CURRENT_TIMESTAMP WHERE h.id IN :ids")
    int softDeleteBatch(@Param("ids") List<Short> ids);
}
