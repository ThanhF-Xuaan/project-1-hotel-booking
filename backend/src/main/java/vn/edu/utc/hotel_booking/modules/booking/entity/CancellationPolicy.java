package vn.edu.utc.hotel_booking.modules.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;

import java.math.BigDecimal;

@Entity
@Table(name = "cancellation_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CancellationPolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(name = "policy_code", nullable = false, length = 50)
    String policyCode;

    @Column(nullable = false, length = 150)
    String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_type", nullable = false, length = 20)
    @Builder.Default
    BookingType bookingType = BookingType.FIT;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "free_cancellation_before_days", nullable = false)
    @Builder.Default
    Integer freeCancellationBeforeDays = 7;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_fee_type", nullable = false, length = 20)
    @Builder.Default
    CancellationFeeType cancellationFeeType = CancellationFeeType.TIERED;

    @Column(name = "default_penalty_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    BigDecimal defaultPenaltyPercent = new BigDecimal("100.00");

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tiers", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    String tiers = "[]";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    Boolean isActive = true;

    @Column(name = "created_by", length = 100)
    String createdBy;

    @Column(name = "updated_by", length = 100)
    String updatedBy;
}
