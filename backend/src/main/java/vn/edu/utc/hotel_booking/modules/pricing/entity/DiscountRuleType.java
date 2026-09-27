package vn.edu.utc.hotel_booking.modules.pricing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;

@Entity
@Table(name = "discount_rule_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiscountRuleType extends BaseEntity {

    @Id
    @Column(length = 50)
    String code;

    @Column(name = "display_name", nullable = false, length = 150)
    String displayName;

    @Column(nullable = false)
    @Builder.Default
    Short priority = 0;

    @Column(length = 50)
    @Builder.Default
    String status = "ACTIVE";
}
