package vn.edu.utc.hotel_booking.modules.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.DocumentType;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.Gender;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "stay_guests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StayGuest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    Hotel hotel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_room_id")
    BookingRoom bookingRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    RoomInstance room;

    @Column(name = "room_number", length = 20)
    String roomNumber;

    @Column(name = "first_name", length = 100)
    String firstName;

    @Column(name = "last_name", length = 100)
    String lastName;

    @Column(name = "full_name", nullable = false, length = 250)
    String fullName;

    @Column(name = "date_of_birth")
    LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "guest_type", length = 20)
    @Builder.Default
    BookingGuestType guestType = BookingGuestType.ADULT;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    Gender gender;

    @Column(nullable = false, length = 100)
    @Builder.Default
    String nationality = "Việt Nam";

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 20)
    @Builder.Default
    DocumentType documentType = DocumentType.CCCD;

    @Column(name = "document_number", length = 50)
    String documentNumber;

    @Column(name = "permanent_address", columnDefinition = "TEXT")
    String permanentAddress;

    @Column(name = "current_address", columnDefinition = "TEXT")
    String currentAddress;

    @Column(name = "check_in_time")
    OffsetDateTime checkInTime;

    @Column(name = "expected_check_out_time")
    OffsetDateTime expectedCheckOutTime;

    @Column(name = "actual_check_out_time")
    OffsetDateTime actualCheckOutTime;

    @Column(name = "reason_for_stay", length = 150)
    @Builder.Default
    String reasonForStay = "Du lịch";

    @Column(name = "document_image_url", columnDefinition = "TEXT")
    String documentImageUrl;

    @Column(name = "image_purged_at")
    OffsetDateTime imagePurgedAt;

    public LocalDate getBirthDate() {
        return dateOfBirth;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.dateOfBirth = birthDate;
    }

    public String getIdentityNumber() {
        return documentNumber;
    }

    public void setIdentityNumber(String identityNumber) {
        this.documentNumber = identityNumber;
    }

    public String getIdentityType() {
        return documentType != null ? documentType.name() : null;
    }

    public void setIdentityType(String identityType) {
        if (identityType != null) {
            try {
                this.documentType = DocumentType.valueOf(identityType);
            } catch (IllegalArgumentException e) {
                this.documentType = DocumentType.OTHER;
            }
        }
    }
}
