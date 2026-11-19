package vn.edu.utc.hotel_booking.modules.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;
import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "folios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Folio extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_room_id")
    BookingRoom bookingRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    Hotel hotel;

    @Column(name = "folio_number", nullable = false, unique = true, length = 100)
    String folioNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    FolioStatus status = FolioStatus.OPEN;

    @Column(name = "total_debits", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal totalDebits = BigDecimal.ZERO;

    @Column(name = "total_credits", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal totalCredits = BigDecimal.ZERO;

    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "closed_at")
    OffsetDateTime closedAt;

    @OneToMany(mappedBy = "folio", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    List<FolioTransaction> transactions = new ArrayList<>();
}

