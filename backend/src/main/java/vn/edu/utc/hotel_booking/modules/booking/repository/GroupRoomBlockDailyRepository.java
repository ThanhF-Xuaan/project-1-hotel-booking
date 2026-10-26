package vn.edu.utc.hotel_booking.modules.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
        SELECT COALESCE(SUM(d.blockedQuantity - d.pickedUpQuantity), 0)
        FROM GroupRoomBlockDaily d
        JOIN d.roomBlock b
        JOIN b.contract c
        WHERE d.hotelRoomType.id = :hotelRoomTypeId
          AND d.stayDate = :stayDate
          AND d.status = vn.edu.utc.hotel_booking.modules.booking.entity.GroupRoomBlockDailyStatus.ACTIVE
          AND b.status = vn.edu.utc.hotel_booking.modules.booking.entity.GroupRoomBlockStatus.DEFINITE
          AND c.status IN (
              vn.edu.utc.hotel_booking.modules.booking.entity.GroupContractStatus.DEPOSIT_PAID,
              vn.edu.utc.hotel_booking.modules.booking.entity.GroupContractStatus.ROOMING_LIST_ATTACHED
          )
    """)
    Integer sumActiveBlockedRooms(
            @Param("hotelRoomTypeId") Integer hotelRoomTypeId,
            @Param("stayDate") LocalDate stayDate
    );
}
