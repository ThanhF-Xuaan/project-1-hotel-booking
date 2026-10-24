package vn.edu.utc.hotel_booking.modules.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "group_room_blocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupRoomBlock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    GroupContract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_room_type_id", nullable = false)
    HotelRoomType hotelRoomType;

    @Column(name = "agreed_rate", nullable = false, precision = 15, scale = 2)
    BigDecimal agreedRate;

    @Column(name = "total_blocked_rooms", nullable = false)
    Integer totalBlockedRooms;

    @Column(name = "allocated_rooms", nullable = false)
    @Builder.Default
    Integer allocatedRooms = 0;

    @Column(name = "released_rooms", nullable = false)
    @Builder.Default
    Integer releasedRooms = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    GroupRoomBlockStatus status = GroupRoomBlockStatus.TENTATIVE;

    @OneToMany(mappedBy = "roomBlock", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<GroupRoomBlockDaily> dailyBlocks = new ArrayList<>();
}
