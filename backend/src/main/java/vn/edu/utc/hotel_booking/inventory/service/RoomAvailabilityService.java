package vn.edu.utc.hotel_booking.inventory.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.inventory.api.StayCriteria;
import vn.edu.utc.hotel_booking.property.service.RoomTypeCatalogService;

import java.sql.Date;

@Service
@Transactional(readOnly = true)
public class RoomAvailabilityService {
    private final JdbcTemplate jdbc;
    private final RoomTypeCatalogService roomTypes;

    public RoomAvailabilityService(JdbcTemplate jdbc, RoomTypeCatalogService roomTypes) {
        this.jdbc = jdbc;
        this.roomTypes = roomTypes;
    }

    public Availability availability(int hotelRoomTypeId, StayCriteria stay) {
        var type = roomTypes.find(hotelRoomTypeId);
        stay.validateCapacity(type);
        Integer physical = jdbc.queryForObject("""
                SELECT count(*) FROM room_instances r
                WHERE r.hotel_room_type_id = ? AND r.hotel_id = ? AND r.is_deleted = false
                  AND r.current_status <> 'MAINTENANCE'
                  AND NOT EXISTS (
                      SELECT 1 FROM room_slots s WHERE s.room_instance_id = r.id
                        AND s.slot_date >= ? AND s.slot_date < ? AND s.status <> 'READY'
                  )
                  AND NOT EXISTS (
                      SELECT 1 FROM room_maintenance_blocks b WHERE b.room_instance_id = r.id
                        AND b.status = 'ACTIVE' AND b.start_date < ? AND b.end_date >= ?
                  )
                """, Integer.class, type.id(), type.hotelId(), Date.valueOf(stay.checkIn()),
                Date.valueOf(stay.checkOut()), Date.valueOf(stay.checkOut()), Date.valueOf(stay.checkIn()));
        Integer counter = jdbc.queryForObject("""
                SELECT min(available_count) FROM room_availability
                WHERE hotel_room_type_id = ? AND date >= ? AND date < ?
                """, Integer.class, type.id(), Date.valueOf(stay.checkIn()), Date.valueOf(stay.checkOut()));
        int count = Math.max(0, physical == null ? 0 : physical);
        if (counter != null) {
            count = Math.min(count, Math.max(0, counter));
        }
        return new Availability(type.id(), stay.checkIn(), stay.checkOut(), count);
    }

    public record Availability(int hotelRoomTypeId, java.time.LocalDate checkIn,
                               java.time.LocalDate checkOut, int availableRooms) {}
}
