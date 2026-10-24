package vn.edu.utc.hotel_booking.modules.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;
import vn.edu.utc.hotel_booking.modules.finance.entity.MasterFolio;
import vn.edu.utc.hotel_booking.modules.identity.entity.Company;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "group_contracts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupContract extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "contract_number", nullable = false, unique = true, length = 50)
    String contractNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    Hotel hotel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_staff_id", nullable = false)
    Staff salesStaff;

    @Column(name = "group_name", nullable = false, length = 255)
    String groupName;

    @Column(name = "company_tax_code", nullable = false, length = 50)
    String companyTaxCode;

    @Column(name = "company_legal_representative", length = 150)
    String companyLegalRepresentative;

    @Column(name = "signer_name", length = 150)
    String signerName;

    @Column(name = "signer_title", length = 100)
    String signerTitle;

    @Column(name = "check_in_date", nullable = false)
    LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    LocalDate checkOutDate;

    @Column(name = "total_rooms", nullable = false)
    Integer totalRooms;

    @Column(name = "total_guests", nullable = false)
    @Builder.Default
    Integer totalGuests = 1;

    @Column(name = "subtotal_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(name = "service_fee_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    BigDecimal serviceFeeRate = BigDecimal.valueOf(5.00);

    @Column(name = "service_fee_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal serviceFeeAmount = BigDecimal.ZERO;

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    BigDecimal vatRate = BigDecimal.valueOf(10.00);

    @Column(name = "vat_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(name = "total_contract_value", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal totalContractValue = BigDecimal.ZERO;

    @Column(name = "deposit_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    BigDecimal depositRate = BigDecimal.valueOf(50.00);

    @Column(name = "deposit_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(name = "deposit_paid_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal depositPaidAmount = BigDecimal.ZERO;

    @Column(name = "deposit_due_date", nullable = false)
    OffsetDateTime depositDueDate;

    @Column(name = "deposit_paid_at")
    OffsetDateTime depositPaidAt;

    @Column(name = "cutoff_date", nullable = false)
    OffsetDateTime cutoffDate;

    @Column(name = "final_payment_due_date", nullable = false)
    OffsetDateTime finalPaymentDueDate;

    @Column(name = "lock_checkin", nullable = false)
    @Builder.Default
    Boolean lockCheckin = false;

    @Column(name = "governing_law", nullable = false, length = 100)
    @Builder.Default
    String governingLaw = "VIETNAMESE_LAW";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    GroupContractStatus status = GroupContractStatus.DRAFT;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    String cancellationReason;

    @Column(name = "special_requests", columnDefinition = "TEXT")
    String specialRequests;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<GroupRoomBlock> roomBlocks = new ArrayList<>();

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<ContractAttachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "contract")
    @Builder.Default
    List<MasterFolio> masterFolios = new ArrayList<>();
}
