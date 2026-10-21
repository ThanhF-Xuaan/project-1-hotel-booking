package vn.edu.utc.hotel_booking.modules.operation.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;
import vn.edu.utc.hotel_booking.modules.pricing.entity.TaxCategory;

import java.math.BigDecimal;

@Entity
@Table(name = "master_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MasterItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(nullable = false, unique = true, length = 100)
    String code;

    @Column(nullable = false, length = 255)
    String name;

    @Column(columnDefinition = "TEXT")
    String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 50)
    MenuType itemType;

    @Column(name = "default_base_price", nullable = false, precision = 15, scale = 2)
    BigDecimal defaultBasePrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_category_id")
    TaxCategory taxCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_type", length = 50)
    ServicePricingType pricingType;

    @Column(length = 50)
    String unit;

    @Column(nullable = false, length = 50)
    @Builder.Default
    String status = "ACTIVE";
}
