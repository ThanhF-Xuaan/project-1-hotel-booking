package vn.edu.utc.hotel_booking.modules.inventory.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.repository.projection.PersonalizationCatalogRow;

import java.util.List;

public interface PersonalizationCatalogQueryRepository extends Repository<HotelRoomType, Integer> {

    @Query(value = """
            SELECT
                CAST(hotel.id AS INTEGER) AS "hotelId",
                eligible.hotel_room_type_id AS "hotelRoomTypeId",
                eligible.room_type_code AS "roomTypeCode",
                eligible.room_type_name AS "roomTypeName"
            FROM hotels hotel
            LEFT JOIN LATERAL (
                SELECT hrt.id AS hotel_room_type_id, rt.code AS room_type_code, rt.name AS room_type_name
                FROM hotel_room_types hrt
                JOIN room_types rt ON rt.id = hrt.room_type_id
                WHERE hrt.hotel_id = hotel.id
                  AND hrt.status = 'ACTIVE'
                  AND hrt.is_deleted = FALSE
                  AND rt.status = 'ACTIVE'
                  AND rt.is_deleted = FALSE
                ORDER BY hrt.id
            ) eligible ON TRUE
            WHERE hotel.id = :hotelId
              AND hotel.status = 'ACTIVE'
              AND hotel.is_deleted = FALSE
            ORDER BY eligible.hotel_room_type_id
            """, nativeQuery = true)
    List<PersonalizationCatalogRow> findActiveCatalog(@Param("hotelId") Integer hotelId);
}
