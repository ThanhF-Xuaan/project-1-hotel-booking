package vn.edu.utc.hotel_booking.modules.operation.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    Hotel hotel;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 50)
    @Builder.Default
    ServiceOrderReferenceType referenceType = ServiceOrderReferenceType.BOOKING;

    @Column(name = "reference_id")
    Long referenceId;

    @Column(name = "sub_total", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal subTotal = BigDecimal.ZERO;

    @Column(name = "service_fee_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    BigDecimal serviceFeeRate = BigDecimal.ZERO;

    @Column(name = "service_fee_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal serviceFeeAmount = BigDecimal.ZERO;

    @Column(name = "vat_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    ServiceOrderStatus status = ServiceOrderStatus.PENDING;

    @Column(name = "issued_at")
    @Builder.Default
    OffsetDateTime issuedAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "serviceOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<ServiceOrderDetail> details = new ArrayList<>();

    public Long getBookingId() {
        return referenceType == ServiceOrderReferenceType.BOOKING ? referenceId : null;
    }
}
