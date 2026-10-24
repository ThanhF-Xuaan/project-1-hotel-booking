package vn.edu.utc.hotel_booking.modules.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "group_room_block_daily")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupRoomBlockDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_block_id", nullable = false)
    GroupRoomBlock roomBlock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_room_type_id", nullable = false)
    HotelRoomType hotelRoomType;

    @Column(name = "stay_date", nullable = false)
    LocalDate stayDate;

    @Column(name = "blocked_quantity", nullable = false)
    Integer blockedQuantity;

    @Column(name = "picked_up_quantity", nullable = false)
    @Builder.Default
    Integer pickedUpQuantity = 0;

    @Column(name = "released_quantity", nullable = false)
    @Builder.Default
    Integer releasedQuantity = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    GroupRoomBlockDailyStatus status = GroupRoomBlockDailyStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    OffsetDateTime updatedAt;
}
