package vn.edu.utc.hotel_booking.modules.operation.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;
import vn.edu.utc.hotel_booking.modules.booking.entity.StayGuest;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.LodgingQueueStatus;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.time.OffsetDateTime;

@Entity
@Table(name = "lodging_queues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LodgingQueue extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stay_guest_id", nullable = false)
    StayGuest stayGuest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    Hotel hotel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    LodgingQueueStatus status = LodgingQueueStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    String errorMessage;

    @Column(name = "exported_at")
    OffsetDateTime exportedAt;

    @Column(name = "batch_reference", length = 100)
    String batchReference;
}
