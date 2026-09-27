package vn.edu.utc.hotel_booking.modules.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "room_maintenance_blocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomMaintenanceBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_instance_id", nullable = false)
    RoomInstance roomInstance;

    @Column(name = "block_type", nullable = false, length = 20)
    @Builder.Default
    String blockType = "OOO"; // 'OOO', 'OOS'

    @Column(name = "start_date", nullable = false)
    LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    LocalDate endDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    String reason;

    @Column(nullable = false, length = 50)
    @Builder.Default
    String status = "ACTIVE"; // 'ACTIVE', 'COMPLETED', 'CANCELLED'

    @Column(name = "maintenance_ticket_id")
    Long maintenanceTicketId;

    @Column(name = "created_by_staff_id")
    Integer createdByStaffId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    OffsetDateTime updatedAt;
}
