package vn.edu.utc.hotel_booking.modules.pricing.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "surcharge_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SurchargeRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_room_type_id", nullable = false)
    HotelRoomType hotelRoomType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "age_policy_id")
    HotelAgePolicy agePolicy;

    @Column(name = "rule_type", nullable = false, length = 50)
    String ruleType; // 'EXTRA_PERSON', 'EXTRA_BED', 'EARLY_CHECKIN', 'LATE_CHECKOUT'

    @Column(name = "pricing_type", nullable = false, length = 20)
    @Builder.Default
    String pricingType = "PER_NIGHT"; // 'PER_NIGHT', 'PER_STAY'

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conditions", columnDefinition = "jsonb")
    @Builder.Default
    String conditions = "{}";

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
