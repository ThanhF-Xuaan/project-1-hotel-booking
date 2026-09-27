package vn.edu.utc.hotel_booking.modules.pricing.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "vat_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VatRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_category_id", nullable = false)
    TaxCategory taxCategory;

    @Column(name = "vat_code", nullable = false, unique = true, length = 50)
    String vatCode;

    @Column(name = "vat_name", nullable = false, length = 150)
    String vatName;

    @Column(name = "vat_percent", nullable = false, precision = 5, scale = 2)
    BigDecimal vatPercent;

    @Column(name = "start_date", nullable = false)
    LocalDate startDate;

    @Column(name = "end_date")
    LocalDate endDate;

    @Column(nullable = false, length = 20)
    @Builder.Default
    String status = "ACTIVE";
}
