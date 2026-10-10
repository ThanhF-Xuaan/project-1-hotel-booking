package vn.edu.utc.hotel_booking.modules.pricing.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import vn.edu.utc.hotel_booking.modules.identity.entity.BookingGuest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "voucher_usages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoucherUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    Voucher voucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_guest_id", nullable = false)
    BookingGuest bookingGuest;

    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 2)
    BigDecimal discountAmount;

    @CreationTimestamp
    @Column(name = "used_at", updatable = false)
    OffsetDateTime usedAt;
}
