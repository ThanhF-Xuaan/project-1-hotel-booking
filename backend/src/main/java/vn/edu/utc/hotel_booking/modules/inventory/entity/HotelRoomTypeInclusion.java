package vn.edu.utc.hotel_booking.modules.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;

@Entity
@Table(name = "hotel_room_type_inclusions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelRoomTypeInclusion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_room_type_id", nullable = false)
    HotelRoomType hotelRoomType;

    @Column(name = "reference_type", nullable = false, length = 50)
    String referenceType;

    @Column(name = "reference_id", nullable = false)
    Integer referenceId;

    @Column(name = "quantity")
    @Builder.Default
    Integer quantity = 1;
}
