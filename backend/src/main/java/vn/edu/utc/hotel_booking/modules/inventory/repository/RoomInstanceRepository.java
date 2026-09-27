package vn.edu.utc.hotel_booking.modules.inventory.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomInstanceRepository extends JpaRepository<RoomInstance, Integer> {

    @Query("SELECT r FROM RoomInstance r " +
            "JOIN FETCH r.hotel h " +
            "JOIN FETCH r.hotelRoomType hrt " +
            "JOIN FETCH hrt.roomType rt " +
            "WHERE r.id = :id AND r.isDeleted = false")
    Optional<RoomInstance> findByIdWithDetails(@Param("id") Integer id);

    Optional<RoomInstance> findByIdAndIsDeletedFalse(Integer id);

    boolean existsByHotelIdAndRoomNumberAndIsDeletedFalse(Short hotelId, String roomNumber);

    boolean existsByHotelIdAndRoomNumberAndIdNotAndIsDeletedFalse(Short hotelId, String roomNumber, Integer id);

    @Query("SELECT r FROM RoomInstance r " +
            "JOIN FETCH r.hotel h " +
            "JOIN FETCH r.hotelRoomType hrt " +
            "JOIN FETCH hrt.roomType rt " +
            "WHERE r.isDeleted = false " +
            "AND (:hotelId IS NULL OR r.hotel.id = :hotelId) " +
            "AND (:hotelRoomTypeId IS NULL OR r.hotelRoomType.id = :hotelRoomTypeId) " +
            "AND (:roomNumber IS NULL OR LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :roomNumber, '%'))) " +
            "AND (:currentStatus IS NULL OR r.currentStatus = :currentStatus)")
    Page<RoomInstance> filterRooms(
            @Param("hotelId") Short hotelId,
            @Param("hotelRoomTypeId") Integer hotelRoomTypeId,
            @Param("roomNumber") String roomNumber,
            @Param("currentStatus") String currentStatus,
            Pageable pageable
    );

    List<RoomInstance> findByHotelRoomTypeIdAndCurrentStatusAndIsDeletedFalse(Integer hotelRoomTypeId, String status);

    @Modifying
    @Query("UPDATE RoomInstance r SET r.isDeleted = true WHERE r.id IN :ids")
    int softDeleteByIds(@Param("ids") List<Integer> ids);
}
