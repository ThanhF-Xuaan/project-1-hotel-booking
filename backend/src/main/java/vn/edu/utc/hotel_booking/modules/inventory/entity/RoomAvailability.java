package vn.edu.utc.hotel_booking.modules.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "room_availability", uniqueConstraints = {
        @UniqueConstraint(name = "uk_room_availability", columnNames = {"hotel_room_type_id", "date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_room_type_id", nullable = false)
    HotelRoomType hotelRoomType;

    @Column(nullable = false)
    LocalDate date;

    @Column(name = "total_rooms", nullable = false)
    Integer totalRooms;

    @Column(name = "booked_rooms", nullable = false)
    @Builder.Default
    Integer bookedRooms = 0;

    @Column(name = "locked_rooms", nullable = false)
    @Builder.Default
    Integer lockedRooms = 0;

    @Column(name = "ooo_rooms", nullable = false)
    @Builder.Default
    Integer oooRooms = 0;

    // Generated column in PostgreSQL: (total_rooms - booked_rooms - locked_rooms - ooo_rooms)
    @Column(name = "available_count", insertable = false, updatable = false)
    Integer availableCount;

    @Version
    @Column(nullable = false)
    @Builder.Default
    Long version = 0L;

    @Column(name = "locked_until")
    OffsetDateTime lockedUntil;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    OffsetDateTime updatedAt;
}
