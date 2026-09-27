package vn.edu.utc.hotel_booking.modules.pricing.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

@Entity
@Table(name = "hotel_age_policies", uniqueConstraints = {
        @UniqueConstraint(name = "uk_hotel_guest_type", columnNames = {"hotel_id", "guest_type"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelAgePolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Short id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    Hotel hotel;

    @Column(name = "guest_type", nullable = false, length = 20)
    String guestType; // 'ADULT', 'CHILD', 'INFANT'

    @Column(name = "min_age", nullable = false)
    Short minAge;

    @Column(name = "max_age", nullable = false)
    Short maxAge;

    @Column(length = 50)
    @Builder.Default
    String status = "ACTIVE";
}
