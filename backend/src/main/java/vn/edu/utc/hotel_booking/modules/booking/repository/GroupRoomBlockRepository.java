package vn.edu.utc.hotel_booking.modules.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupRoomBlock;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupRoomBlockStatus;

import java.util.List;

@Repository
public interface GroupRoomBlockRepository extends JpaRepository<GroupRoomBlock, Long> {

    List<GroupRoomBlock> findByContractId(Long contractId);

    List<GroupRoomBlock> findByHotelRoomTypeIdAndStatus(Integer hotelRoomTypeId, GroupRoomBlockStatus status);
}
