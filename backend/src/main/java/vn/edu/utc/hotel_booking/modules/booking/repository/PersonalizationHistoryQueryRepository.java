package vn.edu.utc.hotel_booking.modules.booking.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.booking.repository.projection.PersonalizationHistoryRow;

import java.time.OffsetDateTime;
import java.util.List;

public interface PersonalizationHistoryQueryRepository extends Repository<BookingRoom, Long> {

    @Query(value = """
            SELECT DISTINCT
                br.id AS "bookingRoomId",
                CAST(b.hotel_id AS INTEGER) AS "hotelId",
                bd.hotel_room_type_id AS "hotelRoomTypeId",
                br.actual_check_in_at AS "actualCheckInAt",
                br.actual_check_out_at AS "actualCheckOutAt"
            FROM booking_rooms br
            JOIN booking_details bd ON bd.id = br.booking_detail_id
            JOIN bookings b ON b.id = bd.booking_id
            JOIN booking_guests guest ON guest.id = b.guest_id
            JOIN hotel_room_types historical_hrt
              ON historical_hrt.id = bd.hotel_room_type_id
             AND historical_hrt.hotel_id = b.hotel_id
            WHERE guest.id = :guestId
              AND guest.is_deleted = FALSE
              AND b.hotel_id = :hotelId
              AND b.booking_type = 'FIT'
              AND b.status = 'CONFIRMED'
              AND br.status = 'CHECKED_OUT'
              AND bd.quantity = 1
              AND br.actual_check_in_at IS NOT NULL
              AND br.actual_check_out_at IS NOT NULL
              AND br.actual_check_out_at > br.actual_check_in_at
              AND br.actual_check_out_at >= :fromInclusive
              AND br.actual_check_out_at <= :toInclusive
              AND (br.actual_check_in_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
                    < (br.actual_check_out_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
              AND (SELECT COUNT(*) FROM booking_details booking_detail_count
                   WHERE booking_detail_count.booking_id = b.id) = 1
              AND (SELECT COUNT(*) FROM booking_rooms booking_room_count
                   WHERE booking_room_count.booking_detail_id = bd.id) = 1
            ORDER BY br.actual_check_out_at DESC, br.id DESC
            LIMIT 100
            """, nativeQuery = true)
    List<PersonalizationHistoryRow> findEligibleStays(
            @Param("guestId") Long guestId,
            @Param("hotelId") Integer hotelId,
            @Param("fromInclusive") OffsetDateTime fromInclusive,
            @Param("toInclusive") OffsetDateTime toInclusive
    );
}
