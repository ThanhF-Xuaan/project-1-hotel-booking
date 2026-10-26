package vn.edu.utc.hotel_booking.modules.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomMaintenanceBlock;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RoomMaintenanceBlockRepository extends JpaRepository<RoomMaintenanceBlock, Long> {

    List<RoomMaintenanceBlock> findByRoomInstanceId(Integer roomInstanceId);

    @Query("SELECT COUNT(DISTINCT rmb.roomInstance.id) FROM RoomMaintenanceBlock rmb " +
           "WHERE rmb.roomInstance.hotelRoomType.id = :hotelRoomTypeId " +
           "AND rmb.status = 'ACTIVE' AND rmb.blockType = 'OOO' " +
           "AND :date >= rmb.startDate AND :date <= rmb.endDate")
    int countActiveOooByHotelRoomTypeIdAndDate(
            @Param("hotelRoomTypeId") Integer hotelRoomTypeId,
            @Param("date") LocalDate date
    );
}
