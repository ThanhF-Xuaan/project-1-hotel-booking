package vn.edu.utc.hotel_booking.modules.pricing.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;

@Entity
@Table(name = "tax_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TaxCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(name = "category_code", nullable = false, unique = true, length = 50)
    String categoryCode;

    @Column(name = "category_name", nullable = false, length = 150)
    String categoryName;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(nullable = false, length = 20)
    @Builder.Default
    String status = "ACTIVE";
}
