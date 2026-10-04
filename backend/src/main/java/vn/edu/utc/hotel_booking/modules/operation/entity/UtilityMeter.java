package vn.edu.utc.hotel_booking.modules.operation.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.MeterType;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

@Entity
@Table(name = "utility_meters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@FieldDefaults(level = AccessLevel.PRIVATE)
public class UtilityMeter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    Hotel hotel;

    @Column(name = "meter_code", nullable = false, length = 50)
    String meterCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "meter_type", nullable = false, length = 20)
    MeterType meterType;

    @Column(name = "location_label", length = 100)
    String locationLabel;
}
