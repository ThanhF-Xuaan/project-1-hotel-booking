package vn.edu.utc.hotel_booking.modules.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;

import java.math.BigDecimal;

@Entity
@Table(name = "payroll_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PayrollSummary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "staff_id", nullable = false)
    Integer staffId;

    @Column(name = "period_month", nullable = false)
    Integer periodMonth;

    @Column(name = "period_year", nullable = false)
    Integer periodYear;

    @Column(name = "base_salary", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal baseSalary = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal bonus = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal deductions = BigDecimal.ZERO;

    @Column(name = "net_salary", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal netSalary = BigDecimal.ZERO;
}
