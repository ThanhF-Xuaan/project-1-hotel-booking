package vn.edu.utc.hotel_booking.modules.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "room_swap_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomSwapRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_room_id", nullable = false)
    BookingRoom bookingRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_room_instance_id", nullable = false)
    RoomInstance fromRoomInstance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_room_instance_id", nullable = false)
    RoomInstance toRoomInstance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_hotel_room_type_id", nullable = false)
    HotelRoomType fromHotelRoomType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_hotel_room_type_id", nullable = false)
    HotelRoomType toHotelRoomType;

    @Column(name = "swap_date", nullable = false)
    LocalDate swapDate;

    @Column(name = "effective_date", nullable = false)
    LocalDate effectiveDate;

    @Column(columnDefinition = "TEXT", nullable = false)
    String reason;

    @Column(name = "is_upgrade", nullable = false)
    @Builder.Default
    Boolean isUpgrade = false;

    @Column(name = "is_complimentary", nullable = false)
    @Builder.Default
    Boolean isComplimentary = false;

    @Column(name = "price_difference", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal priceDifference = BigDecimal.ZERO;

    @Column(name = "performed_by", nullable = false, length = 100)
    String performedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    OffsetDateTime createdAt;
}
