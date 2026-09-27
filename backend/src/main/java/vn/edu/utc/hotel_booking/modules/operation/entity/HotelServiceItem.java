package vn.edu.utc.hotel_booking.modules.operation.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelServiceItem {

    @Id
    Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    Menu menu;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_type", nullable = false, length = 50)
    @Builder.Default
    ServicePricingType pricingType = ServicePricingType.PER_STAY;
}
