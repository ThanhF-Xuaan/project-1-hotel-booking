package vn.edu.utc.hotel_booking.modules.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelRoomTypeBedId implements Serializable {

    @Column(name = "hotel_room_type_id")
    Integer hotelRoomTypeId;

    @Column(name = "room_bed_id")
    Short roomBedId;
}
