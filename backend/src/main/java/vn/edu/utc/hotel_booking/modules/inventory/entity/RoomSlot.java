package vn.edu.utc.hotel_booking.modules.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "room_slots", uniqueConstraints = {
        @UniqueConstraint(name = "uk_room_slot", columnNames = {"room_instance_id", "slot_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_instance_id", nullable = false)
    RoomInstance roomInstance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maintenance_block_id")
    RoomMaintenanceBlock maintenanceBlock;

    @Column(name = "slot_date", nullable = false)
    LocalDate slotDate;

    @Column(name = "booking_room_id")
    Long bookingRoomId;

    @Column(nullable = false, length = 50)
    @Builder.Default
    String status = "READY"; // 'READY', 'BLOCKED', 'RESERVED', 'OCCUPIED', 'DIRTY', 'CLEANING', 'MAINTENANCE'

    @Column(name = "locked_at")
    OffsetDateTime lockedAt;

    @Column(name = "reserved_at")
    OffsetDateTime reservedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    OffsetDateTime updatedAt;
}
