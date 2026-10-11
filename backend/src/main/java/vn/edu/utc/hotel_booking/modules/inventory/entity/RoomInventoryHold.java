package vn.edu.utc.hotel_booking.modules.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "room_inventory_holds", indexes = {
        @Index(name = "idx_room_inventory_holds_active_session", columnList = "session_id, status, is_deleted"),
        @Index(name = "idx_room_inventory_holds_active_expires", columnList = "expires_at, status, is_deleted"),
        @Index(name = "idx_room_inventory_holds_room_date", columnList = "hotel_room_type_id, hold_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomInventoryHold extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "session_id", nullable = false, length = 100)
    String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_room_type_id", nullable = false)
    HotelRoomType hotelRoomType;

    @Column(name = "hold_date", nullable = false)
    LocalDate holdDate;

    @Column(nullable = false)
    @Builder.Default
    Integer quantity = 1;

    @Column(name = "expires_at", nullable = false)
    OffsetDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    RoomInventoryHoldStatus status = RoomInventoryHoldStatus.HELD;

    @Enumerated(EnumType.STRING)
    @Column(name = "release_reason", length = 50)
    HoldReleaseReason releaseReason;
}
