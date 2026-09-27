package vn.edu.utc.hotel_booking.modules.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "hotel_room_type_beds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelRoomTypeBed {

    @EmbeddedId
    HotelRoomTypeBedId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("hotelRoomTypeId")
    @JoinColumn(name = "hotel_room_type_id")
    HotelRoomType hotelRoomType;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roomBedId")
    @JoinColumn(name = "room_bed_id")
    RoomBed roomBed;

    @Column(name = "base_quantity", nullable = false)
    @Builder.Default
    Short baseQuantity = 1;
}
