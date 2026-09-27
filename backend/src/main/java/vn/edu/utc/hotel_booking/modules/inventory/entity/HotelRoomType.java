package vn.edu.utc.hotel_booking.modules.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "hotel_room_types", uniqueConstraints = {
        @UniqueConstraint(name = "uk_hotel_room_type", columnNames = {"hotel_id", "room_type_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelRoomType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    Hotel hotel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    RoomType roomType;

    @Column(name = "tax_category_id", nullable = false)
    Integer taxCategoryId;

    @Column(name = "standard_adults", nullable = false)
    @Builder.Default
    Short standardAdults = 2;

    @Column(name = "standard_children", nullable = false)
    @Builder.Default
    Short standardChildren = 0;

    @Column(name = "max_adults", nullable = false)
    @Builder.Default
    Short maxAdults = 2;

    @Column(name = "max_children", nullable = false)
    @Builder.Default
    Short maxChildren = 1;

    @Column(name = "max_infants", nullable = false)
    @Builder.Default
    Short maxInfants = 1;

    @Column(name = "max_total_guests", nullable = false)
    @Builder.Default
    Short maxTotalGuests = 3;

    @Column(name = "max_beds", nullable = false)
    @Builder.Default
    Short maxBeds = 1;

    @Column(name = "extra_beds", nullable = false)
    @Builder.Default
    Short extraBeds = 0;

    @Column(name = "base_price", nullable = false, precision = 15, scale = 2)
    BigDecimal basePrice;

    @Column(name = "total_quantity", nullable = false)
    @Builder.Default
    Integer totalQuantity = 0;

    @Column(length = 50)
    @Builder.Default
    String status = "ACTIVE";

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "hotel_room_type_features",
            joinColumns = @JoinColumn(name = "hotel_room_type_id"),
            inverseJoinColumns = @JoinColumn(name = "room_feature_id")
    )
    @Builder.Default
    Set<RoomFeature> features = new HashSet<>();
}
