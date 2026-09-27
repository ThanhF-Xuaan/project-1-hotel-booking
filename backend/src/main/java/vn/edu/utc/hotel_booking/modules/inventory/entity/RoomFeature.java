package vn.edu.utc.hotel_booking.modules.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;

@Entity
@Table(name = "room_features")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomFeature extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Short id;

    @Column(nullable = false, length = 150)
    String name;

    @Column(nullable = false, unique = true, length = 50)
    String code;

    @Column(length = 50)
    String category; // VIEW, BATHROOM, BEDROOM, MEDIA, ENTERTAINMENT, AMENITY, COMFORT, INTERNET, OTHER

    @Column(nullable = false, length = 50)
    @Builder.Default
    String status = "ACTIVE";
}
