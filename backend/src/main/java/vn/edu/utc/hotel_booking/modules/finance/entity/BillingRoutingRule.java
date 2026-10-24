package vn.edu.utc.hotel_booking.modules.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "billing_routing_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BillingRoutingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_folio_id", nullable = false)
    MasterFolio masterFolio;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_category", nullable = false, length = 50)
    ChargeCategory chargeCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "route_target", nullable = false, length = 30)
    @Builder.Default
    RoutingTarget routeTarget = RoutingTarget.MASTER;

    @Column(name = "max_covered_amount", precision = 15, scale = 2)
    BigDecimal maxCoveredAmount;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    OffsetDateTime updatedAt;
}
