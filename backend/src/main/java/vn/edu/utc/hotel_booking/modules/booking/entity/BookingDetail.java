package vn.edu.utc.hotel_booking.modules.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "booking_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_room_type_id", nullable = false)
    HotelRoomType hotelRoomType;

    @Column(name = "room_type_name", nullable = false, length = 150)
    String roomTypeName;

    @Column(nullable = false)
    @Builder.Default
    Short quantity = 1;

    @Column(name = "check_in_date", nullable = false)
    LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    LocalDate checkOutDate;

    @Column(name = "selection_deadline")
    OffsetDateTime selectionDeadline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancellation_policy_id")
    CancellationPolicy cancellationPolicy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "no_show_policy_id")
    NoShowPolicy noShowPolicy;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "cancellation_policy_snapshot", columnDefinition = "jsonb")
    String cancellationPolicySnapshot;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "no_show_policy_snapshot", columnDefinition = "jsonb")
    String noShowPolicySnapshot;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "bookingDetail", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<BookingRoom> bookingRooms = new ArrayList<>();
}
