package vn.edu.utc.hotel_booking.modules.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomTypeInclusion;

import java.util.List;

@Repository
public interface HotelRoomTypeInclusionRepository extends JpaRepository<HotelRoomTypeInclusion, Long> {

    List<HotelRoomTypeInclusion> findByHotelRoomTypeIdAndIsDeletedFalse(Integer hotelRoomTypeId);

    List<HotelRoomTypeInclusion> findByHotelRoomTypeId(Integer hotelRoomTypeId);

    @Query("SELECT i FROM HotelRoomTypeInclusion i " +
           "LEFT JOIN FETCH i.menu m " +
           "LEFT JOIN FETCH i.masterItem mi " +
           "WHERE i.hotelRoomType.id = :hotelRoomTypeId AND i.isDeleted = false")
    List<HotelRoomTypeInclusion> findActiveWithDetailsByHotelRoomTypeId(@Param("hotelRoomTypeId") Integer hotelRoomTypeId);
}
