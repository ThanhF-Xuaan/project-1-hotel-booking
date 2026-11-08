package vn.edu.utc.hotel_booking.modules.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomTypeBed;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomTypeBedId;

import java.util.List;

@Repository
public interface HotelRoomTypeBedRepository extends JpaRepository<HotelRoomTypeBed, HotelRoomTypeBedId> {

    List<HotelRoomTypeBed> findByHotelRoomTypeId(Integer hotelRoomTypeId);

    @Modifying
    @Query("DELETE FROM HotelRoomTypeBed hrtb WHERE hrtb.id.hotelRoomTypeId = :hotelRoomTypeId")
    void deleteByHotelRoomTypeId(@Param("hotelRoomTypeId") Integer hotelRoomTypeId);
}

