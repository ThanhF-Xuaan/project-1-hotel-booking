package vn.edu.utc.hotel_booking.modules.pricing.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "pricing_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PricingRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_room_type_id", nullable = false)
    HotelRoomType hotelRoomType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "holiday_calendar_id")
    HolidayCalendar holidayCalendar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_type", nullable = false)
    PricingRuleType ruleType;

    @Column(name = "adjustment_type", nullable = false, length = 20)
    String adjustmentType; // 'PERCENT', 'FIXED'

    @Column(name = "adjustment_value", nullable = false, precision = 15, scale = 2)
    BigDecimal adjustmentValue;

    @Column(name = "start_date", nullable = false)
    LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    LocalDate endDate;

    @Column(length = 50)
    @Builder.Default
    String status = "ACTIVE";
}
