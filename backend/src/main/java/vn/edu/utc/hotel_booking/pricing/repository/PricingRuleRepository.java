package vn.edu.utc.hotel_booking.pricing.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Repository
public class PricingRuleRepository {
    private final JdbcTemplate jdbc;

    public PricingRuleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<DatedRule<BigDecimal>> vatRules(int taxCategoryId, LocalDate checkIn, LocalDate checkOut) {
        return jdbc.query("""
                SELECT vat_percent, start_date, end_date FROM vat_rules
                WHERE tax_category_id = ? AND status = 'ACTIVE' AND is_deleted = false
                  AND start_date < ? AND (end_date IS NULL OR end_date >= ?)
                """, (rs, row) -> new DatedRule<>(rs.getDate("start_date").toLocalDate(),
                rs.getDate("end_date") == null ? null : rs.getDate("end_date").toLocalDate(),
                rs.getBigDecimal("vat_percent")), taxCategoryId, Date.valueOf(checkOut), Date.valueOf(checkIn));
    }

    public List<DatedRule<PricingRule>> pricingRules(int hotelRoomTypeId, LocalDate checkIn, LocalDate checkOut) {
        return jdbc.query("""
                SELECT pr.rule_type, pr.adjustment_type, pr.adjustment_value, hc.date AS holiday_date,
                       pr.start_date, pr.end_date
                FROM pricing_rules pr JOIN pricing_rule_types prt ON prt.code = pr.rule_type
                LEFT JOIN holiday_calendars hc ON hc.id = pr.holiday_calendar_id
                WHERE pr.hotel_room_type_id = ? AND pr.status = 'ACTIVE' AND pr.is_deleted = false
                  AND prt.status = 'ACTIVE' AND prt.is_deleted = false
                  AND (pr.holiday_calendar_id IS NULL OR (hc.status = 'ACTIVE' AND hc.is_deleted = false))
                  AND pr.start_date < ? AND pr.end_date >= ?
                """, (rs, row) -> new DatedRule<>(rs.getDate("start_date").toLocalDate(),
                rs.getDate("end_date").toLocalDate(), new PricingRule(rs.getString("rule_type"),
                rs.getString("adjustment_type"), rs.getBigDecimal("adjustment_value"),
                rs.getDate("holiday_date") == null ? null : rs.getDate("holiday_date").toLocalDate())),
                hotelRoomTypeId, Date.valueOf(checkOut), Date.valueOf(checkIn));
    }

    public List<DatedRule<DiscountRule>> discountRules(int hotelRoomTypeId, LocalDate checkIn, LocalDate checkOut) {
        return jdbc.query("""
                SELECT dr.rule_type, dr.discount_type, dr.discount_value, dr.conditions::text AS conditions,
                       COALESCE(drt.priority, 0) AS priority,
                       GREATEST(dr.start_date, c.start_date) AS start_date,
                       LEAST(dr.end_date, c.end_date) AS end_date
                FROM discount_rules dr JOIN discount_rule_types drt ON drt.code = dr.rule_type
                LEFT JOIN campaigns c ON c.id = dr.campaign_id
                WHERE dr.hotel_room_type_id = ? AND dr.status = 'ACTIVE' AND dr.is_deleted = false
                  AND drt.status = 'ACTIVE' AND drt.is_deleted = false
                  AND dr.start_date < ? AND dr.end_date >= ?
                  AND (dr.campaign_id IS NULL OR (c.status = 'ACTIVE' AND c.is_deleted = false
                       AND c.start_date < ? AND c.end_date >= ?))
                ORDER BY drt.priority DESC, dr.id
                """, (rs, row) -> new DatedRule<>(rs.getDate("start_date").toLocalDate(),
                rs.getDate("end_date").toLocalDate(), new DiscountRule(rs.getString("rule_type"),
                rs.getString("discount_type"), rs.getBigDecimal("discount_value"),
                rs.getString("conditions"), rs.getInt("priority"))),
                hotelRoomTypeId, Date.valueOf(checkOut), Date.valueOf(checkIn), Date.valueOf(checkOut), Date.valueOf(checkIn));
    }

    public List<DatedRule<SurchargeRule>> surchargeRules(int hotelRoomTypeId, LocalDate checkIn, LocalDate checkOut) {
        return jdbc.query("""
                SELECT sr.rule_type, sr.pricing_type, sr.adjustment_type, sr.adjustment_value,
                       sr.conditions::text AS conditions, hap.guest_type, sr.start_date, sr.end_date
                FROM surcharge_rules sr LEFT JOIN hotel_age_policies hap ON hap.id = sr.age_policy_id
                WHERE sr.hotel_room_type_id = ? AND sr.status = 'ACTIVE' AND sr.is_deleted = false
                  AND sr.start_date < ? AND sr.end_date >= ?
                """, (rs, row) -> new DatedRule<>(rs.getDate("start_date").toLocalDate(),
                rs.getDate("end_date").toLocalDate(), new SurchargeRule(rs.getString("rule_type"), rs.getString("pricing_type"),
                rs.getString("adjustment_type"), rs.getBigDecimal("adjustment_value"),
                rs.getString("conditions"), rs.getString("guest_type"))),
                hotelRoomTypeId, Date.valueOf(checkOut), Date.valueOf(checkIn));
    }

    /** Configuration dates are inclusive; a null end date means no expiry (VAT). */
    public record DatedRule<T>(LocalDate startDate, LocalDate endDate, T rule) {
        public boolean activeOn(LocalDate day) {
            return !day.isBefore(startDate) && (endDate == null || !day.isAfter(endDate));
        }
    }

    public record PricingRule(String ruleType, String adjustmentType, BigDecimal value, LocalDate holidayDate) {}
    public record DiscountRule(String ruleType, String adjustmentType, BigDecimal value, String conditions, int priority) {}
    public record SurchargeRule(String ruleType, String pricingType, String adjustmentType,
                                BigDecimal value, String conditions, String guestType) {}
}
