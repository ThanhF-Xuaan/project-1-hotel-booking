package vn.edu.utc.hotel_booking.modules.inventory.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface HotelRoomTypeRepository extends JpaRepository<HotelRoomType, Integer> {

    @Query("SELECT hrt FROM HotelRoomType hrt " +
            "JOIN FETCH hrt.hotel h " +
            "JOIN FETCH hrt.roomType rt " +
            "LEFT JOIN FETCH hrt.features f " +
            "WHERE hrt.id = :id AND hrt.isDeleted = false")
    Optional<HotelRoomType> findByIdWithDetails(@Param("id") Integer id);

    Optional<HotelRoomType> findByIdAndIsDeletedFalse(Integer id);

    boolean existsByHotelIdAndRoomTypeIdAndIsDeletedFalse(Short hotelId, Short roomTypeId);

    boolean existsByHotelIdAndRoomTypeIdAndIdNotAndIsDeletedFalse(Short hotelId, Short roomTypeId, Integer id);

    @Query("SELECT hrt FROM HotelRoomType hrt " +
            "JOIN FETCH hrt.hotel h " +
            "JOIN FETCH hrt.roomType rt " +
            "WHERE hrt.isDeleted = false " +
            "AND (:hotelId IS NULL OR hrt.hotel.id = :hotelId) " +
            "AND (:roomTypeId IS NULL OR hrt.roomType.id = :roomTypeId) " +
            "AND (:adults IS NULL OR hrt.maxAdults >= :adults) " +
            "AND (:children IS NULL OR hrt.maxChildren >= :children) " +
            "AND (:minPrice IS NULL OR hrt.basePrice >= :minPrice) " +
            "AND (:maxPrice IS NULL OR hrt.basePrice <= :maxPrice) " +
            "AND (:status IS NULL OR hrt.status = :status)")
    Page<HotelRoomType> filterHotelRoomTypes(
            @Param("hotelId") Short hotelId,
            @Param("roomTypeId") Short roomTypeId,
            @Param("adults") Short adults,
            @Param("children") Short children,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("status") String status,
            Pageable pageable
    );

    @Modifying
    @Query("UPDATE HotelRoomType hrt SET hrt.isDeleted = true WHERE hrt.id IN :ids")
    int softDeleteByIds(@Param("ids") List<Integer> ids);
}
