package vn.edu.utc.hotel_booking.modules.finance.service.impl;

import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRepository;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.PaymentCreateRequest;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.PaymentSearchDto;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.ProcessRefundRequest;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.PaymentResponse;
import vn.edu.utc.hotel_booking.modules.finance.entity.*;
import vn.edu.utc.hotel_booking.modules.finance.gateway.PaymentGateway;
import vn.edu.utc.hotel_booking.modules.finance.gateway.dto.CallbackResult;
import vn.edu.utc.hotel_booking.modules.finance.gateway.dto.CreatePaymentRequest;
import vn.edu.utc.hotel_booking.modules.finance.gateway.dto.PaymentUrlResponse;
import vn.edu.utc.hotel_booking.modules.finance.mapper.FinanceMapper;
import vn.edu.utc.hotel_booking.modules.finance.repository.InvoiceRepository;
import vn.edu.utc.hotel_booking.modules.finance.repository.PaymentRepository;
import vn.edu.utc.hotel_booking.modules.finance.repository.TransactionRepository;
import vn.edu.utc.hotel_booking.modules.finance.service.PaymentService;
import java.time.LocalDate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import vn.edu.utc.hotel_booking.modules.notification.event.BookingConfirmationEmailEvent;
import vn.edu.utc.hotel_booking.modules.notification.event.PaymentReceiptEmailEvent;
import vn.edu.utc.hotel_booking.modules.notification.dto.InvoiceItemEmailDto;
import vn.edu.utc.hotel_booking.modules.notification.dto.ServiceItemEmailDto;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderStatus;
import java.math.RoundingMode;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingDetail;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.finance.service.FolioLedgerService;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrder;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderPaymentStatus;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderReferenceType;
import vn.edu.utc.hotel_booking.modules.operation.repository.ServiceOrderRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService, ApplicationEventPublisherAware {

    PaymentRepository paymentRepository;
    TransactionRepository transactionRepository;
    InvoiceRepository invoiceRepository;
    BookingRepository bookingRepository;
    ServiceOrderRepository serviceOrderRepository;
    FinanceMapper financeMapper;
    FolioLedgerService folioLedgerService;
    StringRedisTemplate redisTemplate;
    @NonFinal
    ApplicationEventPublisher eventPublisher;

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    /** Tất cả bean PaymentGateway (VnPayGateway, MoMoGateway) — Strategy Pattern */
    List<PaymentGateway> paymentGateways;

    /** TTL lock phiên thanh toán = 10 phút (Rules concurrency — FE đồng bộ countdown) */
    private static final Duration GATEWAY_LOCK_TTL = Duration.ofMinutes(10);
    private static final String GATEWAY_LOCK_PREFIX = "payment:lock:booking:";

    @Override
    @Transactional
    public PaymentResponse createPayment(PaymentCreateRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND, "Không tìm thấy đơn đặt phòng: " + request.getBookingId()));

        if (request.getTotalAmount() == null || request.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_AMOUNT, "Số tiền thanh toán phải lớn hơn 0");
        }

        BigDecimal grandTotal = getGrandTotal(booking);
        BigDecimal remainingBalance = getRemainingBalance(booking, grandTotal);

        if (remainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_COMPLETED,
                    "Đơn đặt phòng đã thanh toán hoàn tất (Số dư nợ = 0)");
        }

        if (request.getTotalAmount().compareTo(remainingBalance) > 0) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_AMOUNT,
                    "Số tiền thanh toán (" + request.getTotalAmount() + ") vượt quá số dư còn nợ (" + remainingBalance + ")");
        }

        // B1: Validate & chuẩn hóa mã tham chiếu theo từng phương thức (mục 1)
        PaymentMethod paymentMethod = request.getPaymentMethod();
        String reference = request.getTransactionReference() != null
                ? request.getTransactionReference().trim() : null;

        if (paymentMethod == PaymentMethod.VNPAY || paymentMethod == PaymentMethod.MOMO) {
            // Cấm FE nhập tay: để trống cho webhook tự ghi khi gateway callback
            reference = null;
        } else if ((paymentMethod == PaymentMethod.BANK_TRANSFER
                || paymentMethod == PaymentMethod.CREDIT_CARD
                || paymentMethod == PaymentMethod.DEBIT_CARD)
                && (reference == null || reference.isEmpty())) {
            throw new AppException(ErrorCode.PAYMENT_REFERENCE_REQUIRED,
                    "Phương thức " + paymentMethod.name()
                            + " bắt buộc nhập mã tham chiếu (mã giao dịch ngân hàng / approval code POS)");
        }

        // B2: CREDIT_CARD đã nhập approval code → tiền đã quẹt thành công trên POS → SUCCESS ngay (như CASH/DEBIT)
        boolean isImmediateSuccess = paymentMethod == PaymentMethod.CASH
                || paymentMethod == PaymentMethod.DEBIT_CARD
                || paymentMethod == PaymentMethod.CREDIT_CARD;
        PaymentStatus initialStatus = isImmediateSuccess ? PaymentStatus.SUCCESS : PaymentStatus.PENDING;
        OffsetDateTime paidAt = isImmediateSuccess ? OffsetDateTime.now() : null;

        Payment payment = Payment.builder()
                .booking(booking)
                .totalAmount(request.getTotalAmount())
                .paymentPurpose(request.getPaymentPurpose() != null ? request.getPaymentPurpose() : PaymentPurpose.FULL_PAYMENT)
                .paymentMethod(paymentMethod)
                .paymentProvider(request.getPaymentProvider())
                .transactionReference(reference)
                .status(initialStatus)
                .paidAt(paidAt)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        if (isImmediateSuccess) {
            Transaction transaction = Transaction.builder()
                    .payment(savedPayment)
                    .booking(booking)
                    .transactionType(TransactionType.PAYMENT)
                    .amount(request.getTotalAmount())
                    // B3: dùng mã tham chiếu thật (phiếu thu / approval code), chỉ fallback TX-<ts> khi không có
                    .referenceCode(buildReferenceCode(reference))
                    .status("COMPLETED")
                    .issuedAt(OffsetDateTime.now())
                    .build();
            transactionRepository.save(transaction);
            updateInvoiceStatusIfSettled(booking);
            syncPaymentToFolio(savedPayment);
        }

        return financeMapper.toResponse(savedPayment);
    }

    // B3: mã tham chiếu thật nếu có, ngược lại giữ mã hệ thống TX-<timestamp>
    private String buildReferenceCode(String transactionReference) {
        return (transactionReference != null && !transactionReference.isBlank())
                ? transactionReference
                : "TX-" + System.currentTimeMillis();
    }

    // ==================== GIAI ĐOẠN C — Payment Gateway (VNPay/MoMo) ====================

    @Override
    @Transactional
    public PaymentUrlResponse createGatewayPayment(CreatePaymentRequest request, String ipAddress) {
        // Chỉ nhận 2 phương thức online
        if (request.getMethod() != PaymentMethod.VNPAY && request.getMethod() != PaymentMethod.MOMO) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA,
                    "Phương thức " + request.getMethod() + " không phải thanh toán online qua gateway");
        }

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND,
                        "Không tìm thấy đơn đặt phòng: " + request.getBookingId()));

        PaymentGateway gateway = findGateway(request.getMethod());

        // C2: Redis lock TTL 10' — chặn tạo trùng phiên khi khách bấm nhiều lần / đang redirect
        String lockKey = GATEWAY_LOCK_PREFIX + booking.getId();
        String txnRef = generateTxnRef(request.getMethod());
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, txnRef, GATEWAY_LOCK_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new AppException(ErrorCode.PAYMENT_CALLBACK_DUPLICATED,
                    "Đã có phiên thanh toán đang diễn ra cho booking " + booking.getId()
                            + " — thử lại sau " + GATEWAY_LOCK_TTL.toMinutes() + " phút");
        }

        try {
            Payment payment;
            String orderInfo;
            if (request.getPaymentId() != null) {
                // Tái sử dụng Payment đã tạo trước đó (qua POST /payments/create)
                payment = paymentRepository.findById(request.getPaymentId())
                        .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND,
                                "Không tìm thấy thanh toán ID: " + request.getPaymentId()));
                if (payment.getStatus() != PaymentStatus.PENDING) {
                    throw new AppException(ErrorCode.PAYMENT_NOT_PENDING,
                            "Chỉ tái sử dụng được thanh toán đang chờ (PENDING)");
                }
                if (payment.getGatewayTxnId() != null) {
                    throw new AppException(ErrorCode.PAYMENT_CALLBACK_DUPLICATED,
                            "Thanh toán đã có phiên gateway, không tạo phiên mới");
                }
                payment.setGatewayTxnId(txnRef);
                orderInfo = payment.getServiceOrder() != null
                        ? "Thanh toan don POS " + payment.getServiceOrder().getOrderNumber()
                        : "Thanh toan dat phong " + booking.getBookingNumber();
            } else if (request.getServiceOrderId() != null) {
                // Thanh toán riêng cho đơn dịch vụ phòng POS
                ServiceOrder serviceOrder = serviceOrderRepository.findById(request.getServiceOrderId())
                        .orElseThrow(() -> new AppException(ErrorCode.SERVICE_ORDER_NOT_FOUND,
                                "Không tìm thấy đơn dịch vụ ID: " + request.getServiceOrderId()));

                if (serviceOrder.getPaymentStatus() == ServiceOrderPaymentStatus.PAID) {
                    throw new AppException(ErrorCode.PAYMENT_ALREADY_COMPLETED,
                            "Đơn dịch vụ #" + serviceOrder.getOrderNumber() + " đã được thanh toán hoàn tất");
                }

                BigDecimal paymentAmount = serviceOrder.getTotalAmount();
                payment = Payment.builder()
                        .booking(booking)
                        .serviceOrder(serviceOrder)
                        .totalAmount(paymentAmount)
                        .paymentPurpose(PaymentPurpose.FULL_PAYMENT)
                        .paymentMethod(request.getMethod())
                        .gatewayTxnId(txnRef)
                        .status(PaymentStatus.PENDING)
                        .build();

                orderInfo = "Thanh toan don POS " + serviceOrder.getOrderNumber();
            } else {
                BigDecimal grandTotal = getGrandTotal(booking);
                BigDecimal remainingBalance = getRemainingBalance(booking, grandTotal);

                if (remainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new AppException(ErrorCode.PAYMENT_ALREADY_COMPLETED,
                            "Đơn đặt phòng đã thanh toán hoàn tất (Số dư nợ = 0)");
                }

                BigDecimal paymentAmount = request.getAmount();
                if (paymentAmount == null) {
                    paymentAmount = remainingBalance;
                } else {
                    if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new AppException(ErrorCode.INVALID_PAYMENT_AMOUNT,
                                "Số tiền thanh toán phải lớn hơn 0");
                    }
                    if (paymentAmount.compareTo(remainingBalance) > 0) {
                        throw new AppException(ErrorCode.INVALID_PAYMENT_AMOUNT,
                                "Số tiền thanh toán (" + paymentAmount + ") vượt quá số dư còn nợ (" + remainingBalance + ")");
                    }
                }

                payment = Payment.builder()
                        .booking(booking)
                        .totalAmount(paymentAmount)
                        .paymentPurpose(PaymentPurpose.FULL_PAYMENT)
                        .paymentMethod(request.getMethod())
                        .gatewayTxnId(txnRef)
                        .status(PaymentStatus.PENDING)
                        .build();

                orderInfo = "Thanh toan dat phong " + booking.getBookingNumber();
            }
            Payment saved = paymentRepository.save(payment);

            String effectiveBankCode = request.getBankCode();
            if ((effectiveBankCode == null || effectiveBankCode.isBlank()) && request.getMethod() == PaymentMethod.VNPAY) {
                effectiveBankCode = "VNBANK";
            }
            String paymentUrl = gateway.createPaymentUrl(txnRef, saved.getTotalAmount(), orderInfo, ipAddress, effectiveBankCode);

            return PaymentUrlResponse.builder()
                    .paymentUrl(paymentUrl)
                    .paymentId(saved.getId())
                    .txnRef(txnRef)
                    .expiresAt(OffsetDateTime.now().plus(GATEWAY_LOCK_TTL))
                    .build();
        } catch (RuntimeException e) {
            // Thất bại giữa chừng → giải phóng lock để khách thử lại được ngay
            redisTemplate.delete(lockKey);
            throw e;
        }
    }

    @Override
    @Transactional
    public PaymentResponse handleGatewayCallback(PaymentMethod method, Map<String, String> params) {
        PaymentGateway gateway = findGateway(method);

        // C5: verify chữ ký TRƯỚC — sai → 8004 (không đụng DB)
        CallbackResult result = gateway.verifyCallback(params);
        if (!result.isValid()) {
            log.warn("Callback chữ ký sai method={} payload={}", method, maskPayload(result.getRawPayload()));
            throw new AppException(ErrorCode.PAYMENT_SIGNATURE_INVALID,
                    "Chữ ký callback không hợp lệ (" + method + ")");
        }

        Payment payment = paymentRepository.findByGatewayTxnIdWithLock(result.getTxnRef())
                .or(() -> paymentRepository.findByGatewayTxnId(result.getTxnRef()))
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND,
                        "Không tìm thấy giao dịch với mã đơn: " + result.getTxnRef()));

        // Idempotency: IPN gọi lại lần 2 → không update lần 2 (guard theo status)
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new AppException(ErrorCode.PAYMENT_CALLBACK_DUPLICATED,
                    "Giao dịch đã được xử lý trước đó (payment ID: " + payment.getId() + ")");
        }

        // Chống lệch số tiền (gateway báo về ≠ số tiền thanh toán đã tạo)
        if (result.getAmount() != null
                && payment.getTotalAmount().compareTo(result.getAmount()) != 0) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_AMOUNT,
                    "Số tiền callback (" + result.getAmount() + ") không khớp thanh toán ("
                            + payment.getTotalAmount() + ")");
        }

        payment.setRawCallbackPayload(result.getRawPayload());
        payment.setGatewayResponseCode(result.getResponseCode());
        if (result.getBankCode() != null && !result.getBankCode().isBlank()) {
            payment.setPaymentProvider(result.getBankCode());
        }
        Payment saved;

        if (result.isSuccess()) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(OffsetDateTime.now());
            // C3: TỰ GHI reference từ webhook (vnp_TransactionNo / MoMo orderId) — FE không nhập tay
            payment.setTransactionReference(result.getTransactionReference());
            saved = paymentRepository.save(payment);

            Transaction transaction = Transaction.builder()
                    .payment(saved)
                    .booking(saved.getBooking())
                    .transactionType(TransactionType.PAYMENT)
                    .amount(saved.getTotalAmount())
                    .referenceCode(buildReferenceCode(result.getTransactionReference()))
                    .status("COMPLETED")
                    .issuedAt(OffsetDateTime.now())
                    .build();
            transactionRepository.save(transaction);

            // Cập nhật đơn dịch vụ POS nếu giao dịch thanh toán cho ServiceOrder
            if (saved.getServiceOrder() != null) {
                ServiceOrder so = saved.getServiceOrder();
                so.setPaymentStatus(ServiceOrderPaymentStatus.PAID);
                serviceOrderRepository.save(so);
                log.info("Cập nhật đơn dịch vụ POS #{} sang PAID qua gateway {}", so.getOrderNumber(), method);
            } else {
                updateInvoiceStatusIfSettled(saved.getBooking());
                publishPaymentReceiptEmail(saved);
            }
            syncPaymentToFolio(saved);
        } else {
            // Gateway báo thất bại (hủy/đủ hạn mức...) → FAILED để FE hiển thị thử lại
            payment.setStatus(PaymentStatus.FAILED);
            saved = paymentRepository.save(payment);
        }

        // Giải phóng lock — khách được phép tạo phiên mới (SUCCESS lẫn FAILED)
        releaseLock(saved.getBooking().getId());
        return financeMapper.toResponse(saved);
    }

    private PaymentGateway findGateway(PaymentMethod method) {
        return paymentGateways.stream()
                .filter(g -> g.supportedMethod() == method)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.GATEWAY_ERROR,
                        "Chưa cấu hình cổng thanh toán cho phương thức: " + method));
    }

    /** Mã đơn của mình: VNP-/MOMO- + timestamp + random (≤34 ký tự theo quy định VNPay) */
    private String generateTxnRef(PaymentMethod method) {
        String prefix = method == PaymentMethod.VNPAY ? "VNP" : "MOMO";
        String timestamp = OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return prefix + timestamp + "-" + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    private void releaseLock(Long bookingId) {
        try {
            redisTemplate.delete(GATEWAY_LOCK_PREFIX + bookingId);
        } catch (Exception e) {
            // Redis trục trặc không được làm fail webhook — lock tự hết hạn sau TTL 10'
            log.warn("Không giải phóng được lock thanh toán booking {}: {}", bookingId, e.getMessage());
        }
    }

    @Override
    public PageResponse<PaymentResponse> filter(PaymentSearchDto searchDto) {
        int page = searchDto.getPage() != null && searchDto.getPage() > 0 ? searchDto.getPage() - 1 : 0;
        int size = searchDto.getPageSize() != null && searchDto.getPageSize() > 0 ? searchDto.getPageSize() : 10;

        Specification<Payment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (searchDto.getBookingId() != null) {
                predicates.add(cb.equal(root.get("booking").get("id"), searchDto.getBookingId()));
            }
            if (searchDto.getPaymentMethod() != null) {
                predicates.add(cb.equal(root.get("paymentMethod"), searchDto.getPaymentMethod()));
            }
            if (searchDto.getPaymentPurpose() != null) {
                predicates.add(cb.equal(root.get("paymentPurpose"), searchDto.getPaymentPurpose()));
            }
            if (searchDto.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), searchDto.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Payment> pageResult = paymentRepository.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.from(pageResult.map(financeMapper::toResponse));
    }

    @Override
    public PaymentResponse getById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND, "Không tìm thấy thông tin thanh toán: " + id));
        return financeMapper.toResponse(payment);
    }

    @Override
    public List<PaymentResponse> getByBookingId(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId).stream()
                .map(financeMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public PaymentResponse completePayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND, "Không tìm thấy thanh toán ID: " + id));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_COMPLETED, "Thanh toán này đã hoàn tất");
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new AppException(ErrorCode.PAYMENT_NOT_PENDING,
                    "Chỉ xác nhận hoàn tất được giao dịch đang chờ (PENDING) — trạng thái hiện tại: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(OffsetDateTime.now());
        Payment saved = paymentRepository.save(payment);

        Transaction transaction = Transaction.builder()
                .payment(saved)
                .booking(saved.getBooking())
                .transactionType(TransactionType.PAYMENT)
                .amount(saved.getTotalAmount())
                // B3: dùng mã tham chiếu thật của payment (VD: FT26... khi lễ tân xác nhận bank transfer)
                .referenceCode(buildReferenceCode(saved.getTransactionReference()))
                .status("COMPLETED")
                .issuedAt(OffsetDateTime.now())
                .build();
        transactionRepository.save(transaction);
        if (saved.getBooking() != null) {
            updateInvoiceStatusIfSettled(saved.getBooking());
            syncPaymentToFolio(saved);
            releaseLock(saved.getBooking().getId());
            publishPaymentReceiptEmail(saved);
        }

        return financeMapper.toResponse(saved);
    }

    private void publishPaymentReceiptEmail(Payment payment) {
        if (eventPublisher == null || payment == null || payment.getBooking() == null) {
            return;
        }
        Booking booking = payment.getBooking();
        if (booking.getGuest() == null || booking.getGuest().getEmail() == null || booking.getGuest().getEmail().isBlank()) {
            return;
        }

        try {
            BigDecimal grandTotal = getGrandTotal(booking);
            BigDecimal remainingBalance = getRemainingBalance(booking, grandTotal);
            if (remainingBalance.compareTo(BigDecimal.ZERO) < 0) {
                remainingBalance = BigDecimal.ZERO;
            }

            // Trích xuất số phòng
            String roomNumber = null;
            if (booking.getBookingDetails() != null) {
                for (BookingDetail bd : booking.getBookingDetails()) {
                    if (bd.getBookingRooms() != null) {
                        for (BookingRoom br : bd.getBookingRooms()) {
                            if (br.getRoomInstance() != null && br.getRoomInstance().getRoomNumber() != null) {
                                roomNumber = br.getRoomInstance().getRoomNumber();
                                break;
                            }
                        }
                    }
                    if (roomNumber != null) break;
                }
            }

            // Trích xuất hóa đơn GTGT / Folio mới nhất nếu có
            Invoice invoice = null;
            try {
                if (invoiceRepository != null && booking.getId() != null) {
                    List<Invoice> invoices = invoiceRepository.findByBookingId(booking.getId());
                    if (invoices != null && !invoices.isEmpty()) {
                        invoice = invoices.get(invoices.size() - 1);
                    }
                }
            } catch (Exception ex) {
                log.debug("Chưa tìm thấy hóa đơn cho đơn {}: {}", booking.getId(), ex.getMessage());
            }

            String invoiceNumber = (invoice != null && invoice.getInvoiceNumber() != null)
                    ? invoice.getInvoiceNumber()
                    : null;
            OffsetDateTime invoiceDate = (invoice != null && invoice.getIssuedAt() != null)
                    ? invoice.getIssuedAt()
                    : (invoice != null && invoice.getCreatedAt() != null ? invoice.getCreatedAt() : payment.getPaidAt());
            BigDecimal subtotalAmount = invoice != null ? invoice.getSubTotal() : booking.getSubtotalAmount();
            BigDecimal serviceFeeRate = invoice != null ? invoice.getServiceFeeRate() : booking.getServiceFeeRate();
            BigDecimal serviceFeeAmount = invoice != null ? invoice.getServiceFeeAmount() : booking.getServiceFeeAmount();
            BigDecimal vatAmount = invoice != null ? invoice.getVatAmount() : booking.getTotalVatAmount();
            BigDecimal invoiceGrandTotal = invoice != null ? invoice.getGrandTotal() : grandTotal;

            List<InvoiceItemEmailDto> invoiceDetails = new ArrayList<>();
            if (invoice != null && invoice.getDetails() != null && !invoice.getDetails().isEmpty()) {
                for (InvoiceDetail d : invoice.getDetails()) {
                    invoiceDetails.add(InvoiceItemEmailDto.builder()
                            .description(d.getDescription())
                            .quantity(d.getQuantity() != null ? d.getQuantity() : 1)
                            .unitPrice(d.getUnitPrice())
                            .vatRate(d.getVatRate())
                            .vatAmount(d.getVatAmount())
                            .subtotal(d.getSubtotal() != null ? d.getSubtotal() : d.getTotalAmount())
                            .build());
                }
            }

            LocalDate checkIn = null;
            LocalDate checkOut = null;
            String roomTypeName = null;
            int totalRooms = 0;
            if (booking.getBookingDetails() != null && !booking.getBookingDetails().isEmpty()) {
                BookingDetail first = booking.getBookingDetails().get(0);
                checkIn = first.getCheckInDate();
                checkOut = first.getCheckOutDate();
                roomTypeName = first.getRoomTypeName();
                for (BookingDetail d : booking.getBookingDetails()) {
                    totalRooms += (d.getQuantity() != null ? d.getQuantity() : 1);
                }
            }

            List<ServiceItemEmailDto> emailServices = extractEmailServices(booking);
            List<ServiceItemEmailDto> emailSurcharges = extractEmailSurcharges(booking);

            BookingConfirmationEmailEvent event = BookingConfirmationEmailEvent.builder()
                    .bookingId(booking.getId())
                    .bookingCode(booking.getBookingNumber())
                    .recipientEmail(booking.getGuest().getEmail().trim())
                    .customerName(booking.getGuest().getFullName())
                    .hotelName(booking.getHotel() != null ? booking.getHotel().getName() : "SmartStay Hotel")
                    .hotelAddress(booking.getHotel() != null ? booking.getHotel().getAddress() : "")
                    .hotelPhone(booking.getHotel() != null ? booking.getHotel().getPhone() : "")
                    .checkInDate(checkIn)
                    .checkOutDate(checkOut)
                    .roomTypeName(roomTypeName != null ? roomTypeName : "Phòng tiêu chuẩn")
                    .totalRooms(totalRooms > 0 ? totalRooms : 1)
                    .totalGuests(1)
                    .subtotalAmount(subtotalAmount != null ? subtotalAmount : booking.getSubtotalAmount())
                    .serviceFeeAmount(serviceFeeAmount != null ? serviceFeeAmount : booking.getServiceFeeAmount())
                    .vatAmount(vatAmount != null ? vatAmount : booking.getTotalVatAmount())
                    .totalAmount(invoiceGrandTotal != null ? invoiceGrandTotal : grandTotal)
                    .currency("VND")
                    .isPaid(true)
                    .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : "VNPAY")
                    .transactionReference(payment.getTransactionReference())
                    .invoiceNumber(invoiceNumber)
                    .paidAt(payment.getPaidAt() != null ? payment.getPaidAt() : OffsetDateTime.now())
                    .services(emailServices)
                    .surcharges(emailSurcharges)
                    .build();
            eventPublisher.publishEvent(event);
            log.info("[EMAIL] Published unified All-in-One BookingConfirmationEmailEvent for booking {} and payment {}",
                    booking.getBookingNumber(), payment.getId());
        } catch (Exception ex) {
            log.error("Failed to publish BookingConfirmationEmailEvent for payment {}: {}", payment.getId(), ex.getMessage());
        }
    }

    private List<ServiceItemEmailDto> extractEmailServices(Booking booking) {
        List<ServiceItemEmailDto> emailServices = new ArrayList<>();
        if (booking == null || booking.getId() == null || serviceOrderRepository == null) {
            return emailServices;
        }
        try {
            List<Long> roomIds = new ArrayList<>();
            if (booking.getBookingDetails() != null) {
                for (BookingDetail bd : booking.getBookingDetails()) {
                    if (bd.getBookingRooms() != null) {
                        for (BookingRoom br : bd.getBookingRooms()) {
                            if (br.getId() != null) roomIds.add(br.getId());
                        }
                    }
                }
            }
            List<ServiceOrder> orders = roomIds.isEmpty()
                    ? serviceOrderRepository.findByBookingIdAndIsDeletedFalse(booking.getId())
                    : serviceOrderRepository.findAllOrdersForBookingAndRooms(booking.getId(), roomIds);
            if (orders != null) {
                for (ServiceOrder o : orders) {
                    if (o.getStatus() != ServiceOrderStatus.CANCELLED && o.getDetails() != null) {
                        for (var d : o.getDetails()) {
                            int qty = d.getQuantity() != null && d.getQuantity() > 0 ? d.getQuantity() : 1;
                            BigDecimal total = d.getTotalAmount() != null ? d.getTotalAmount() : BigDecimal.ZERO;
                            BigDecimal unitGross = total.divide(BigDecimal.valueOf(qty), 0, RoundingMode.HALF_UP);
                            String itemName = d.getMenu() != null && d.getMenu().getName() != null
                                    ? d.getMenu().getName()
                                    : (d.getMasterItem() != null ? d.getMasterItem().getName() : "Dịch vụ");
                            emailServices.add(ServiceItemEmailDto.builder()
                                    .name(itemName)
                                    .quantity(qty)
                                    .unitPrice(unitGross)
                                    .totalPrice(total)
                                    .build());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Lỗi trích xuất dịch vụ gửi email: {}", ex.getMessage());
        }
        return emailServices;
    }

    private List<ServiceItemEmailDto> extractEmailSurcharges(Booking booking) {
        List<ServiceItemEmailDto> emailSurcharges = new ArrayList<>();
        if (booking == null || booking.getBookingDetails() == null) {
            return emailSurcharges;
        }
        try {
            for (BookingDetail bd : booking.getBookingDetails()) {
                if (bd.getBookingRooms() != null) {
                    for (BookingRoom br : bd.getBookingRooms()) {
                        if (br.getCharges() != null) {
                            for (var c : br.getCharges()) {
                                if (c.getTotalAmount() != null && c.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
                                    emailSurcharges.add(ServiceItemEmailDto.builder()
                                            .name(c.getItemName() != null ? c.getItemName() : (c.getChargeType() != null ? c.getChargeType().name() : "Phụ thu"))
                                            .quantity(c.getQuantity() != null ? c.getQuantity() : 1)
                                            .unitPrice(c.getUnitPrice() != null ? c.getUnitPrice() : BigDecimal.ZERO)
                                            .totalPrice(c.getTotalAmount())
                                            .build());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Lỗi trích xuất phụ thu gửi email: {}", ex.getMessage());
        }
        return emailSurcharges;
    }

    @Override
    @Transactional
    public PaymentResponse cancelPayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND, "Không tìm thấy thanh toán ID: " + id));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new AppException(ErrorCode.PAYMENT_NOT_PENDING,
                    "Chỉ hủy được thanh toán đang chờ thanh toán (PENDING) — trạng thái hiện tại: "
                            + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        Payment saved = paymentRepository.save(payment);

        // Giải phóng lock phiên gateway (nếu là payment online) để khách tạo phiên mới ngay
        if (saved.getBooking() != null) {
            releaseLock(saved.getBooking().getId());
        }
        log.info("Đã hủy thanh toán PENDING → CANCELLED: payment id={}, booking={}",
                saved.getId(), saved.getBooking() != null ? saved.getBooking().getId() : null);
        return financeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentResponse refundPayment(Long id, BigDecimal refundAmount, String reason) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND, "Không tìm thấy thanh toán ID: " + id));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_AMOUNT, "Chỉ có thể hoàn tiền cho giao dịch đã thanh toán thành công");
        }

        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0 || refundAmount.compareTo(payment.getTotalAmount()) > 0) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_AMOUNT, "Số tiền hoàn không hợp lệ (phải từ 0 đến " + payment.getTotalAmount() + ")");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        Payment saved = paymentRepository.save(payment);

        Transaction transaction = Transaction.builder()
                .payment(saved)
                .booking(saved.getBooking())
                .transactionType(TransactionType.REFUND)
                .amount(refundAmount)
                .referenceCode(reason != null ? reason : "REFUND-" + System.currentTimeMillis())
                .status("COMPLETED")
                .issuedAt(OffsetDateTime.now())
                .build();
        transactionRepository.save(transaction);
        if (saved.getBooking() != null) {
            syncInvoiceAfterRefund(saved.getBooking());
        }

        return financeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentResponse processRefund(Long id, ProcessRefundRequest request) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND, "Không tìm thấy yêu cầu hoàn tiền ID: " + id));

        if (payment.getPaymentPurpose() != PaymentPurpose.REFUND) {
            throw new AppException(ErrorCode.PAYMENT_NOT_PENDING,
                    "Giao dịch ID " + id + " không phải là yêu cầu hoàn tiền (Mục đích hiện tại: " + payment.getPaymentPurpose() + ")");
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new AppException(ErrorCode.PAYMENT_NOT_PENDING,
                    "Chỉ có thể xử lý yêu cầu hoàn tiền đang ở trạng thái chờ duyệt (PENDING) — trạng thái hiện tại: " + payment.getStatus());
        }

        if (request.getAction() == ProcessRefundRequest.RefundAction.APPROVE) {
            PaymentMethod method = request.getRefundMethod() != null ? request.getRefundMethod() : PaymentMethod.BANK_TRANSFER;
            String reference = request.getTransactionReference() != null && !request.getTransactionReference().isBlank()
                    ? request.getTransactionReference().trim()
                    : buildReferenceCode(payment.getTransactionReference());

            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setPaymentMethod(method);
            payment.setTransactionReference(reference);
            payment.setPaidAt(OffsetDateTime.now());
            payment.setGatewayTxnId("INTERNAL_REFUND_SETTLED");

            String note = request.getNote() != null ? request.getNote().replace("\"", "\\\"") : "";
            payment.setRawCallbackPayload(String.format(
                    "{\"action\": \"APPROVE\", \"method\": \"%s\", \"reference\": \"%s\", \"note\": \"%s\", \"approvedAt\": \"%s\"}",
                    method.name(), reference, note, OffsetDateTime.now()
            ));

            Payment saved = paymentRepository.save(payment);

            Transaction transaction = Transaction.builder()
                    .payment(saved)
                    .booking(saved.getBooking())
                    .transactionType(TransactionType.REFUND)
                    .amount(saved.getTotalAmount())
                    .referenceCode(reference)
                    .status("COMPLETED")
                    .issuedAt(OffsetDateTime.now())
                    .build();
            transactionRepository.save(transaction);

            if (saved.getBooking() != null) {
                syncInvoiceAfterRefund(saved.getBooking());
                syncPaymentToFolio(saved);
                releaseLock(saved.getBooking().getId());
            }

            log.info("Kế toán đã PHÊ DUYỆT hoàn tiền ID {}: số tiền {} đ, phương thức {}, mã tham chiếu {}",
                    saved.getId(), saved.getTotalAmount(), method, reference);
            return financeMapper.toResponse(saved);
        } else {
            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setGatewayTxnId("INTERNAL_REFUND_REJECTED");
            String note = request.getNote() != null ? request.getNote().replace("\"", "\\\"") : "Kế toán từ chối yêu cầu hoàn tiền";
            payment.setRawCallbackPayload(String.format(
                    "{\"action\": \"REJECT\", \"reason\": \"%s\", \"rejectedAt\": \"%s\"}",
                    note, OffsetDateTime.now()
            ));

            Payment saved = paymentRepository.save(payment);

            if (saved.getBooking() != null) {
                releaseLock(saved.getBooking().getId());
            }

            log.info("Kế toán đã TỪ CHỐI yêu cầu hoàn tiền ID {}: lý do '{}'", saved.getId(), note);
            return financeMapper.toResponse(saved);
        }
    }

    private BigDecimal getGrandTotal(Booking booking) {
        if (folioLedgerService != null) {
            BigDecimal folioDebits = folioLedgerService.getTotalDebitsByBookingId(booking.getId());
            if (folioDebits != null && folioDebits.compareTo(BigDecimal.ZERO) > 0) {
                return folioDebits;
            }
        }
        List<Invoice> invoices = invoiceRepository.findByBookingId(booking.getId());
        if (invoices != null && !invoices.isEmpty()) {
            return invoices.get(0).getGrandTotal();
        }
        return booking.getTotalAmount() != null ? booking.getTotalAmount() : BigDecimal.ZERO;
    }

    private BigDecimal getRemainingBalance(Booking booking, BigDecimal grandTotal) {
        if (folioLedgerService != null) {
            BigDecimal folioBalance = folioLedgerService.getRemainingBalanceByBookingId(booking.getId());
            if (folioBalance != null) {
                return folioBalance;
            }
        }
        List<Payment> existingPayments = paymentRepository.findByBookingId(booking.getId());
        BigDecimal totalPaid = existingPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .map(Payment::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return grandTotal.subtract(totalPaid);
    }

    private void updateInvoiceStatusIfSettled(Booking booking) {
        try {
            List<Invoice> invoices = invoiceRepository.findByBookingId(booking.getId());
            if (invoices != null && !invoices.isEmpty()) {
                Invoice inv = invoices.get(0);
                if (inv.getStatus() != InvoiceStatus.PAID && inv.getStatus() != InvoiceStatus.CANCELLED) {
                    List<Payment> existingPayments = paymentRepository.findByBookingId(booking.getId());
                    BigDecimal totalPaid = existingPayments.stream()
                            .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                            .map(Payment::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    if (totalPaid.compareTo(inv.getGrandTotal()) >= 0) {
                        inv.setStatus(InvoiceStatus.PAID);
                        if (inv.getIssuedAt() == null) {
                            inv.setIssuedAt(OffsetDateTime.now());
                        }
                        invoiceRepository.save(inv);
                        log.info("Hóa đơn {} của booking {} đã được tự động chuyển sang PAID", inv.getInvoiceNumber(), booking.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Không thể cập nhật trạng thái hóa đơn cho booking {}: {}", booking.getId(), e.getMessage());
        }
    }

    private void syncInvoiceAfterRefund(Booking booking) {
        try {
            List<Invoice> invoices = invoiceRepository.findByBookingId(booking.getId());
            if (invoices != null && !invoices.isEmpty()) {
                Invoice inv = invoices.get(0);
                if (inv.getStatus() == InvoiceStatus.PAID) {
                    List<Payment> existingPayments = paymentRepository.findByBookingId(booking.getId());
                    BigDecimal totalPaid = existingPayments.stream()
                            .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                            .map(Payment::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    if (totalPaid.compareTo(inv.getGrandTotal()) < 0) {
                        inv.setStatus(inv.getIssuedAt() != null ? InvoiceStatus.ISSUED : InvoiceStatus.DRAFT);
                        invoiceRepository.save(inv);
                        log.info("Hóa đơn {} của booking {} chuyển lại trạng thái {} sau hoàn tiền",
                                inv.getInvoiceNumber(), booking.getId(), inv.getStatus());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Không thể đồng bộ trạng thái hóa đơn sau hoàn tiền cho booking {}: {}", booking.getId(), e.getMessage());
        }
    }

    private String maskPayload(String payload) {
        if (payload == null) return null;
        return payload.replaceAll("(\"vnp_SecureHash\"\\s*:\\s*\")[^\"]+(\")", "$1****$2");
    }

    private void syncPaymentToFolio(Payment payment) {
        if (folioLedgerService == null || payment == null || payment.getBooking() == null) {
            return;
        }
        try {
            Long bookingId = payment.getBooking().getId();
            Long bookingRoomId = (payment.getServiceOrder() != null && payment.getServiceOrder().getReferenceType() == ServiceOrderReferenceType.BOOKING_ROOM)
                    ? payment.getServiceOrder().getReferenceId()
                    : null;

            Folio folio = folioLedgerService.getOrCreateFolio(bookingId, bookingRoomId);
            if (folio != null) {
                boolean isRefund = payment.getPaymentPurpose() == PaymentPurpose.REFUND;
                String category = isRefund ? "REFUND" : "PAYMENT";
                String desc = (isRefund ? "Hoàn tiền " : "Thanh toán ") + payment.getPaymentMethod() + " (" +
                        (payment.getTransactionReference() != null ? payment.getTransactionReference() : payment.getPaymentPurpose()) + ")";

                BigDecimal creditAmount = isRefund ? payment.getTotalAmount().negate() : payment.getTotalAmount();

                folioLedgerService.postCredit(
                        folio.getId(),
                        category,
                        "PAYMENT",
                        payment.getId(),
                        desc,
                        creditAmount,
                        payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : "CASH"
                );
            }
        } catch (Exception e) {
            log.warn("Không thể đồng bộ Payment ID {} vào Folio Ledger: {}", payment.getId(), e.getMessage());
        }
    }
}
