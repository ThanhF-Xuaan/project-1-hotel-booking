package vn.edu.utc.hotel_booking.modules.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "booking_guests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingGuest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "public_id", nullable = false, unique = true)
    @Builder.Default
    UUID publicId = UUID.randomUUID();

    @Column(name = "birth_date")
    LocalDate birthDate;

    @Column(name = "identity_type", length = 20)
    String identityType;

    @Column(name = "identity_number", length = 50)
    String identityNumber;

    @Column(length = 100)
    String nationality;

    @Column(length = 150)
    String email;

    @Column(nullable = false, length = 20)
    String phone;

    @Column(length = 50)
    @Builder.Default
    String status = "ACTIVE";
}
