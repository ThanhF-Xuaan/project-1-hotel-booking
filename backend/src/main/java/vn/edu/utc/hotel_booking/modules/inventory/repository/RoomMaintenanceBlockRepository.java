package vn.edu.utc.hotel_booking.modules.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomMaintenanceBlock;

import java.util.List;

@Repository
public interface RoomMaintenanceBlockRepository extends JpaRepository<RoomMaintenanceBlock, Long> {

    List<RoomMaintenanceBlock> findByRoomInstanceId(Integer roomInstanceId);
}
