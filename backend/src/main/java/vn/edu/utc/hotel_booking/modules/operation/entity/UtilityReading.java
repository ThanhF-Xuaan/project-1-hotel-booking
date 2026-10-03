package vn.edu.utc.hotel_booking.modules.operation.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "utility_readings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UtilityReading extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meter_id", nullable = false)
    UtilityMeter meter;

    @Column(name = "reading_date", nullable = false)
    LocalDate readingDate;

    @Column(name = "reading_value", nullable = false, precision = 12, scale = 3)
    BigDecimal readingValue;

    @Column(name = "is_meter_reset", nullable = false)
    Boolean isMeterReset = false;

    @Column(name = "recorded_by", nullable = false)
    Integer recordedBy;

    @Column(name = "updated_by")
    Integer updatedBy;
}
