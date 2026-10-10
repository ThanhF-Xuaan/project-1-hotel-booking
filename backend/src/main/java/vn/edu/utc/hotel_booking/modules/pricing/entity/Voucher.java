package vn.edu.utc.hotel_booking.modules.pricing.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Voucher extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(name = "hotel_id")
    Short hotelId;

    @Column(name = "voucher_code", nullable = false, unique = true, length = 50)
    String voucherCode;

    @Column(name = "title", nullable = false, length = 150)
    String title;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Column(name = "discount_type", nullable = false, length = 20)
    String discountType; // 'PERCENT', 'FIXED'

    @Column(name = "discount_value", nullable = false, precision = 15, scale = 2)
    BigDecimal discountValue;

    @Column(name = "apply_scope", nullable = false, length = 20)
    @Builder.Default
    String applyScope = "ROOM_ONLY"; // 'ROOM_ONLY', 'TOTAL_BOOKING', 'SERVICE_ONLY'

    @Column(name = "min_order_amount", precision = 15, scale = 2)
    @Builder.Default
    BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(name = "max_discount_amount", precision = 15, scale = 2)
    BigDecimal maxDiscountAmount;

    @Column(name = "total_quantity", nullable = false)
    @Builder.Default
    Integer totalQuantity = 100;

    @Column(name = "used_quantity", nullable = false)
    @Builder.Default
    Integer usedQuantity = 0;

    @Column(name = "max_usage_per_user", nullable = false)
    @Builder.Default
    Short maxUsagePerUser = 1;

    @Column(name = "valid_from", nullable = false)
    OffsetDateTime validFrom;

    @Column(name = "valid_to", nullable = false)
    OffsetDateTime validTo;

    @Column(name = "status", length = 50)
    @Builder.Default
    String status = "ACTIVE";
}
