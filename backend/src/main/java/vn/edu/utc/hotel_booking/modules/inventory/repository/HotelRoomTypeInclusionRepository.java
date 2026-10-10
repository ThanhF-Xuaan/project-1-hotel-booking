package vn.edu.utc.hotel_booking.modules.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomTypeInclusion;

import java.util.List;

@Repository
public interface HotelRoomTypeInclusionRepository extends JpaRepository<HotelRoomTypeInclusion, Long> {

    List<HotelRoomTypeInclusion> findByHotelRoomTypeIdAndIsDeletedFalse(Integer hotelRoomTypeId);

    List<HotelRoomTypeInclusion> findByHotelRoomTypeId(Integer hotelRoomTypeId);
}
