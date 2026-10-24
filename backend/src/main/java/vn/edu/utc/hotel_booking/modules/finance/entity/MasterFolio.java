package vn.edu.utc.hotel_booking.modules.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupContract;
import vn.edu.utc.hotel_booking.modules.identity.entity.Company;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "master_folios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MasterFolio extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "folio_number", nullable = false, unique = true, length = 50)
    String folioNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    GroupContract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    Hotel hotel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    Company company;

    @Column(name = "buyer_legal_name", nullable = false, length = 255)
    String buyerLegalName;

    @Column(name = "buyer_tax_code", nullable = false, length = 50)
    String buyerTaxCode;

    @Column(name = "buyer_address", nullable = false, columnDefinition = "TEXT")
    String buyerAddress;

    @Column(name = "buyer_email_for_invoice", length = 150)
    String buyerEmailForInvoice;

    @Column(name = "subtotal_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(name = "service_fee_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal serviceFeeAmount = BigDecimal.ZERO;

    @Column(name = "total_vat_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal totalVatAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "deposit_credited", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal depositCredited = BigDecimal.ZERO;

    @Column(name = "total_paid", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal totalPaid = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    MasterFolioStatus status = MasterFolioStatus.OPEN;

    @Column(name = "closed_at")
    OffsetDateTime closedAt;

    @OneToMany(mappedBy = "masterFolio", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<BillingRoutingRule> routingRules = new ArrayList<>();
}
