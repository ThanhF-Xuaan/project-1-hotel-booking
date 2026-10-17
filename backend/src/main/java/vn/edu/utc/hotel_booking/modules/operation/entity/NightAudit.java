package vn.edu.utc.hotel_booking.modules.operation.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "night_audits", uniqueConstraints = {
        @UniqueConstraint(name = "chk_unique_hotel_night_audit", columnNames = {"hotel_id", "audit_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NightAudit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    Hotel hotel;

    @Column(name = "audit_date", nullable = false)
    LocalDate auditDate;

    @Column(name = "occupied_rooms_count", nullable = false)
    @Builder.Default
    Integer occupiedRoomsCount = 0;

    @Column(name = "no_show_bookings_count", nullable = false)
    @Builder.Default
    Integer noShowBookingsCount = 0;

    @Column(name = "total_daily_room_revenue", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal totalDailyRoomRevenue = BigDecimal.ZERO;

    @Column(name = "total_daily_service_revenue", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal totalDailyServiceRevenue = BigDecimal.ZERO;

    @Column(name = "total_daily_revenue", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal totalDailyRevenue = BigDecimal.ZERO;

    @Column(nullable = false, length = 30)
    @Builder.Default
    String status = "COMPLETED";

    @Column(name = "executed_by", length = 100)
    String executedBy;

    @Column(name = "executed_at", nullable = false)
    @Builder.Default
    OffsetDateTime executedAt = OffsetDateTime.now();

    @Column(columnDefinition = "TEXT")
    String notes;
}
