package vn.edu.utc.hotel_booking.modules.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "booking_rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_detail_id", nullable = false)
    BookingDetail bookingDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_instance_id")
    RoomInstance roomInstance;

    @Column(name = "adult_count", nullable = false)
    @Builder.Default
    Short adultCount = 1;

    @Column(name = "child_count", nullable = false)
    @Builder.Default
    Short childCount = 0;

    @Column(name = "infant_count", nullable = false)
    @Builder.Default
    Short infantCount = 0;

    @Column(name = "guest_count", nullable = false)
    @Builder.Default
    Short guestCount = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    BookingRoomStatus status = BookingRoomStatus.EXPECTED;

    @Column(name = "actual_check_in_at")
    OffsetDateTime actualCheckInAt;

    @Column(name = "actual_check_out_at")
    OffsetDateTime actualCheckOutAt;

    @Column(name = "assigned_at")
    @Builder.Default
    OffsetDateTime assignedAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "bookingRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<StayGuest> stayGuests = new ArrayList<>();

    public List<StayGuest> getBookingGuests() {
        return stayGuests;
    }

    public void setBookingGuests(List<StayGuest> guests) {
        this.stayGuests = guests;
    }

    @OneToMany(mappedBy = "bookingRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<BookingDailyRate> dailyRates = new ArrayList<>();

    @OneToMany(mappedBy = "bookingRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<BookingCharge> charges = new ArrayList<>();
}
