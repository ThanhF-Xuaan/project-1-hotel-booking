package vn.edu.utc.hotel_booking.modules.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupRoomBlockDaily;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupRoomBlockDailyStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRoomBlockDailyRepository extends JpaRepository<GroupRoomBlockDaily, Long> {

    List<GroupRoomBlockDaily> findByRoomBlockId(Long roomBlockId);

    Optional<GroupRoomBlockDaily> findByRoomBlockIdAndStayDate(Long roomBlockId, LocalDate stayDate);

    List<GroupRoomBlockDaily> findByHotelRoomTypeIdAndStayDateAndStatus(
            Integer hotelRoomTypeId,
            LocalDate stayDate,
            GroupRoomBlockDailyStatus status
    );
}
