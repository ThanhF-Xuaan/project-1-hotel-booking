package vn.edu.utc.hotel_booking.modules.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;

@Entity
@Table(name = "room_beds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomBed extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Short id;

    @Column(nullable = false, unique = true, length = 100)
    String name;

    @Column(length = 50)
    String size;

    @Column(length = 50)
    @Builder.Default
    String status = "ACTIVE";
}
