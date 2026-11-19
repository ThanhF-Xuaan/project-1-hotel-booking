package vn.edu.utc.hotel_booking.modules.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "folio_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FolioTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folio_id", nullable = false)
    Folio folio;

    /**
     * Loại bút toán kế toán kép: DEBIT (Chi phí/Ghi Nợ) hoặc CREDIT (Thanh toán/Ghi Có)
     */
    @Column(name = "transaction_type", nullable = false, length = 20)
    String transactionType;

    /**
     * Danh mục nghiệp vụ: ROOM_RATE, SURCHARGE, PENALTY, SERVICE, PRODUCT, PAYMENT, REFUND
     */
    @Column(name = "category", nullable = false, length = 50)
    String category;

    /**
     * Bảng chứng từ nguồn: DAILY_RATE, BOOKING_CHARGE, SERVICE_ORDER, PAYMENT
     */
    @Column(name = "source_type", nullable = false, length = 50)
    String sourceType;

    /**
     * ID khóa chính của bản ghi nguồn (booking_daily_rate_id, booking_charge_id, service_order_id, payment_id)
     */
    @Column(name = "source_id")
    Long sourceId;

    @Column(name = "description", nullable = false, length = 255)
    String description;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    Integer quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "service_fee_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    BigDecimal serviceFeeRate = BigDecimal.ZERO;

    @Column(name = "service_fee_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal serviceFeeAmount = BigDecimal.ZERO;

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    BigDecimal vatRate = BigDecimal.ZERO;

    @Column(name = "vat_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    BigDecimal amount;

    @Column(name = "payment_method", length = 50)
    String paymentMethod;

    @CreationTimestamp
    @Column(name = "posted_at", nullable = false)
    OffsetDateTime postedAt;

    @Column(name = "posted_by", nullable = false, length = 100)
    @Builder.Default
    String postedBy = "SYSTEM";
}

