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

    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM HotelRoomTypeInclusion i " +
           "WHERE i.hotelRoomType.id = :hotelRoomTypeId AND i.menu.id = :menuId AND i.isDeleted = false " +
           "AND (:excludeId IS NULL OR i.id <> :excludeId)")
    boolean existsByRoomTypeAndMenu(@Param("hotelRoomTypeId") Integer hotelRoomTypeId, @Param("menuId") Integer menuId, @Param("excludeId") Long excludeId);

    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM HotelRoomTypeInclusion i " +
           "WHERE i.hotelRoomType.id = :hotelRoomTypeId AND i.masterItem.id = :masterItemId AND i.isDeleted = false " +
           "AND (:excludeId IS NULL OR i.id <> :excludeId)")
    boolean existsByRoomTypeAndMasterItem(@Param("hotelRoomTypeId") Integer hotelRoomTypeId, @Param("masterItemId") Integer masterItemId, @Param("excludeId") Long excludeId);
}
