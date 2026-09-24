package vn.edu.utc.hotel_booking.property.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.common.web.PageRequest;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RoomTypeCatalogService {
    private final JdbcTemplate jdbc;

    public RoomTypeCatalogService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public RoomTypeInfo find(int id) {
        List<RoomTypeInfo> found = jdbc.query("""
                SELECT hrt.id, hrt.hotel_id, rt.name, hrt.tax_category_id, hrt.base_price,
                       hrt.standard_adults, hrt.standard_children, hrt.max_adults,
                       hrt.max_children, hrt.max_infants, hrt.max_total_guests
                FROM hotel_room_types hrt JOIN room_types rt ON rt.id = hrt.room_type_id
                JOIN hotels h ON h.id = hrt.hotel_id
                WHERE hrt.id = ? AND hrt.status = 'ACTIVE' AND hrt.is_deleted = false
                  AND rt.status = 'ACTIVE' AND rt.is_deleted = false
                  AND h.status = 'ACTIVE' AND h.is_deleted = false
                """, (rs, row) -> new RoomTypeInfo(rs.getInt("id"), rs.getInt("hotel_id"),
                rs.getString("name"), rs.getInt("tax_category_id"), rs.getBigDecimal("base_price"),
                rs.getInt("standard_adults"), rs.getInt("standard_children"), rs.getInt("max_adults"),
                rs.getInt("max_children"), rs.getInt("max_infants"), rs.getInt("max_total_guests")), id);
        if (found.isEmpty()) {
            throw new AppException(ErrorCode.NOT_FOUND);
        }
        return found.getFirst();
    }

    public PageResponse<RoomTypeSummary> list(int hotelId, PageRequest page,
                                               int adults, int children, int infants) {
        String where = """
                FROM hotel_room_types hrt JOIN room_types rt ON rt.id = hrt.room_type_id
                WHERE hrt.hotel_id = ? AND hrt.status = 'ACTIVE' AND hrt.is_deleted = false
                  AND rt.status = 'ACTIVE' AND rt.is_deleted = false
                  AND hrt.max_adults >= ? AND hrt.max_children >= ? AND hrt.max_infants >= ?
                  AND hrt.max_total_guests >= ?
                """;
        Long total = jdbc.queryForObject("SELECT count(*) " + where, Long.class,
                hotelId, adults, children, infants, adults + children + infants);
        List<RoomTypeSummary> items = jdbc.query("""
                SELECT hrt.id, rt.name, hrt.max_adults, hrt.max_children,
                       hrt.max_infants, hrt.max_total_guests
                """ + where + " ORDER BY hrt.id LIMIT ? OFFSET ?",
                (rs, row) -> new RoomTypeSummary(rs.getInt("id"), rs.getString("name"),
                        rs.getInt("max_adults"), rs.getInt("max_children"),
                        rs.getInt("max_infants"), rs.getInt("max_total_guests")),
                hotelId, adults, children, infants, adults + children + infants, page.size(), page.offset());
        return PageResponse.of(items, page.page(), page.size(), total == null ? 0 : total);
    }

    public record RoomTypeInfo(int id, int hotelId, String name, int taxCategoryId,
                               BigDecimal basePrice, int standardAdults, int standardChildren,
                               int maxAdults, int maxChildren, int maxInfants, int maxTotalGuests) {}
    public record RoomTypeSummary(int id, String name, int maxAdults, int maxChildren,
                                  int maxInfants, int maxTotalGuests) {}
}
