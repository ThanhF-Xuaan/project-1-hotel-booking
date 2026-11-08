package vn.edu.utc.hotel_booking.modules.inventory.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomBed;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomBedRepository extends JpaRepository<RoomBed, Short> {

    Optional<RoomBed> findByName(String name);

    Optional<RoomBed> findByIdAndIsDeletedFalse(Short id);

    boolean existsByNameAndIsDeletedFalse(String name);

    boolean existsByNameAndIdNotAndIsDeletedFalse(String name, Short id);

    @Query("SELECT rb FROM RoomBed rb WHERE rb.isDeleted = false " +
            "AND (:keyword IS NULL OR LOWER(rb.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(rb.size) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR rb.status = :status)")
    Page<RoomBed> filterRoomBeds(
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable
    );

    @Modifying
    @Query("UPDATE RoomBed rb SET rb.isDeleted = true WHERE rb.id IN :ids")
    int softDeleteByIds(@Param("ids") List<Short> ids);
}
