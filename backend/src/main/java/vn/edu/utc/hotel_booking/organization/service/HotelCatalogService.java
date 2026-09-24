package vn.edu.utc.hotel_booking.organization.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.common.web.PageRequest;
import vn.edu.utc.hotel_booking.iam.service.StaffAccessService;
import vn.edu.utc.hotel_booking.iam.service.StaffAccessService.StaffActor;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class HotelCatalogService {
    private final JdbcTemplate jdbc;
    private final StaffAccessService access;

    public HotelCatalogService(JdbcTemplate jdbc, StaffAccessService access) {
        this.jdbc = jdbc;
        this.access = access;
    }

    public HotelInfo find(int hotelId) {
        var found = jdbc.query("""
                SELECT id, region_id, name, address, service_fee_percent
                FROM hotels WHERE id = ? AND status = 'ACTIVE' AND is_deleted = false
                """, (rs, row) -> new HotelInfo(rs.getInt("id"), rs.getInt("region_id"),
                rs.getString("name"), rs.getString("address"), rs.getBigDecimal("service_fee_percent")), hotelId);
        if (found.isEmpty()) {
            throw new AppException(ErrorCode.NOT_FOUND);
        }
        return found.getFirst();
    }

    public PageResponse<HotelSummary> list(PageRequest page, StaffActor actor) {
        String scopeClause = "";
        Object[] countArgs = {};
        Object[] pageArgs = {page.size(), page.offset()};
        if (actor != null) {
            access.require(actor, "VIEW:INVENTORY");
            switch (actor.scopeType()) {
                case "REGION" -> {
                    scopeClause = " AND region_id = ?";
                    countArgs = new Object[]{actor.scopeEntityId()};
                    pageArgs = new Object[]{actor.scopeEntityId(), page.size(), page.offset()};
                }
                case "PROPERTY" -> {
                    scopeClause = " AND id = ?";
                    countArgs = new Object[]{actor.scopeEntityId()};
                    pageArgs = new Object[]{actor.scopeEntityId(), page.size(), page.offset()};
                }
                case "CHAIN" -> { }
                default -> throw new AppException(ErrorCode.UNAUTHORIZED);
            }
        }
        String where = " WHERE status = 'ACTIVE' AND is_deleted = false" + scopeClause;
        Long total = jdbc.queryForObject("SELECT count(*) FROM hotels" + where, Long.class, countArgs);
        List<HotelSummary> items = jdbc.query("SELECT id, name, address FROM hotels" + where + " ORDER BY id LIMIT ? OFFSET ?",
                (rs, row) -> new HotelSummary(rs.getInt("id"), rs.getString("name"), rs.getString("address")), pageArgs);
        return PageResponse.of(items, page.page(), page.size(), total == null ? 0 : total);
    }

    public record HotelInfo(int id, int regionId, String name, String address, BigDecimal serviceFeePercent) {}
    public record HotelSummary(int id, String name, String address) {}
}
