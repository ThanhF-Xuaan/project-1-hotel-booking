package vn.edu.utc.hotel_booking.modules.inventory.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomType;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, Short> {

    Optional<RoomType> findByIdAndIsDeletedFalse(Short id);

    Optional<RoomType> findByCodeAndIsDeletedFalse(String code);

    boolean existsByCodeAndIsDeletedFalse(String code);

    boolean existsByCodeAndIdNotAndIsDeletedFalse(String code, Short id);

    @Query("SELECT rt FROM RoomType rt WHERE rt.isDeleted = false " +
            "AND (:keyword IS NULL OR LOWER(rt.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(rt.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR rt.status = :status)")
    Page<RoomType> filterRoomTypes(
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable
    );

    @Modifying
    @Query("UPDATE RoomType rt SET rt.isDeleted = true WHERE rt.id IN :ids")
    int softDeleteByIds(@Param("ids") List<Short> ids);
}
