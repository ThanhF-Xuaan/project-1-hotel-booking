package vn.edu.utc.hotel_booking.modules.organization.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "hotels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Hotel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Short id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    Region region;

    @Column(nullable = false, length = 255)
    String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    String address;

    @Column(length = 20)
    String phone;

    @Column(name = "check_in_time", nullable = false)
    @Builder.Default
    LocalTime checkInTime = LocalTime.of(14, 0);

    @Column(name = "check_out_time", nullable = false)
    @Builder.Default
    LocalTime checkOutTime = LocalTime.of(12, 0);

    @Column(name = "service_fee_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    BigDecimal serviceFeePercent = BigDecimal.ZERO;

    @Column(length = 50)
    @Builder.Default
    String status = "ACTIVE";
}
