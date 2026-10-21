package vn.edu.utc.hotel_booking.modules.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "no_show_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoShowPolicy extends BaseEntity {

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
    BookingType bookingType = BookingType.ALL;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "release_room_time", nullable = false)
    @Builder.Default
    LocalTime releaseRoomTime = LocalTime.of(23, 59, 0);

    @Enumerated(EnumType.STRING)
    @Column(name = "penalty_type", nullable = false, length = 30)
    @Builder.Default
    NoShowPenaltyType penaltyType = NoShowPenaltyType.PERCENT_FIRST_NIGHT_RATE;

    @Column(name = "penalty_value", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal penaltyValue = new BigDecimal("100.00");

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    Boolean isActive = true;

    @Column(name = "created_by", length = 100)
    String createdBy;

    @Column(name = "updated_by", length = 100)
    String updatedBy;
}
