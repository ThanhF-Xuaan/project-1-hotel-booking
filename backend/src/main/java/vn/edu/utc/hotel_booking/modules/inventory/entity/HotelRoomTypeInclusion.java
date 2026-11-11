package vn.edu.utc.hotel_booking.modules.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;
import vn.edu.utc.hotel_booking.modules.operation.entity.MasterItem;
import vn.edu.utc.hotel_booking.modules.operation.entity.Menu;

@Entity
@Table(name = "hotel_room_type_inclusions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelRoomTypeInclusion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_room_type_id", nullable = false)
    HotelRoomType hotelRoomType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id")
    Menu menu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_item_id")
    MasterItem masterItem;

    @Column(name = "reference_type", length = 50)
    String referenceType;

    @Column(name = "reference_id")
    Integer referenceId;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    Integer quantity = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 30)
    @Builder.Default
    InclusionFrequency frequency = InclusionFrequency.PER_NIGHT;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    @Builder.Default
    InclusionTargetType targetType = InclusionTargetType.PER_ROOM;

    @Column(name = "custom_display_name", length = 255)
    String customDisplayName;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;
}
