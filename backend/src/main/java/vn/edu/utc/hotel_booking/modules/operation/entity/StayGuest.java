package vn.edu.utc.hotel_booking.modules.operation.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;
import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;
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
    @JoinColumn(name = "hotel_id", nullable = false)
    Hotel hotel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    RoomInstance room;

    @Column(name = "room_number", nullable = false, length = 20)
    String roomNumber;

    @Column(name = "full_name", nullable = false, length = 250)
    String fullName;

    @Column(name = "date_of_birth")
    LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    Gender gender;

    @Column(nullable = false, length = 100)
    @Builder.Default
    String nationality = "Việt Nam";

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    @Builder.Default
    DocumentType documentType = DocumentType.CCCD;

    @Column(name = "document_number", nullable = false, length = 50)
    String documentNumber;

    @Column(name = "permanent_address", columnDefinition = "TEXT")
    String permanentAddress;

    @Column(name = "current_address", columnDefinition = "TEXT")
    String currentAddress;

    @Column(name = "check_in_time", nullable = false)
    OffsetDateTime checkInTime;

    @Column(name = "expected_check_out_time", nullable = false)
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
}
