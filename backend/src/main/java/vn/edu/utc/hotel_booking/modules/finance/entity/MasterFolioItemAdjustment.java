package vn.edu.utc.hotel_booking.modules.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "master_folio_item_adjustments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MasterFolioItemAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_folio_id", nullable = false)
    MasterFolio masterFolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_room_id")
    BookingRoom bookingRoom;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_category", nullable = false, length = 50)
    ChargeCategory chargeCategory;

    @Column(name = "item_type", nullable = false, length = 50)
    String itemType;

    @Column(name = "reference_id", nullable = false)
    Long referenceId;

    @Column(name = "item_description", nullable = false, length = 255)
    String itemDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjusted_target", nullable = false, length = 30)
    RoutingTarget adjustedTarget;

    @Column(nullable = false, precision = 15, scale = 2)
    BigDecimal amount;

    @Column(name = "adjustment_reason", nullable = false, columnDefinition = "TEXT")
    String adjustmentReason;

    @Column(name = "adjusted_by", nullable = false, length = 100)
    String adjustedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    OffsetDateTime createdAt;
}
