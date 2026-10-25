package vn.edu.utc.hotel_booking.modules.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "group_inquiries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupInquiry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "inquiry_number", nullable = false, unique = true, length = 50)
    String inquiryNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    Hotel hotel;

    @Column(name = "company_name", nullable = false, length = 255)
    String companyName;

    @Column(name = "tax_code", length = 50)
    String taxCode;

    @Column(name = "contact_name", nullable = false, length = 150)
    String contactName;

    @Column(name = "contact_email", nullable = false, length = 150)
    String contactEmail;

    @Column(name = "contact_phone", nullable = false, length = 50)
    String contactPhone;

    @Column(name = "check_in_date", nullable = false)
    LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    LocalDate checkOutDate;

    @Column(name = "total_rooms", nullable = false)
    Integer totalRooms;

    @Column(name = "total_guests", nullable = false)
    @Builder.Default
    Integer totalGuests = 1;

    @Column(name = "estimated_budget", precision = 15, scale = 2)
    BigDecimal estimatedBudget;

    @Column(name = "special_requests", columnDefinition = "TEXT")
    String specialRequests;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    GroupInquiryStatus status = GroupInquiryStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_staff_id")
    Staff salesStaff;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_contract_id")
    GroupContract convertedContract;

    @Column(name = "rejection_reason", length = 500)
    String rejectionReason;

    @OneToMany(mappedBy = "inquiry", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<GroupInquiryRoomType> requestedRoomTypes = new ArrayList<>();
}
