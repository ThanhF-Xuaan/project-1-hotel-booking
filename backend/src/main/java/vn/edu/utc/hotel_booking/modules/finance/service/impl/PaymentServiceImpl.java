package vn.edu.utc.hotel_booking.modules.finance.service.impl;

import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
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
import vn.edu.utc.hotel_booking.modules.finance.dto.response.PaymentResponse;
import vn.edu.utc.hotel_booking.modules.finance.entity.*;
import vn.edu.utc.hotel_booking.modules.finance.gateway.PaymentGateway;
import vn.edu.utc.hotel_booking.modules.finance.gateway.dto.CallbackResult;
import vn.edu.utc.hotel_booking.modules.finance.gateway.dto.CreatePaymentRequest;
import vn.edu.utc.hotel_booking.modules.finance.gateway.dto.PaymentUrlResponse;
import vn.edu.utc.hotel_booking.modules.finance.mapper.FinanceMapper;
import vn.edu.utc.hotel_booking.modules.finance.repository.PaymentRepository;
import vn.edu.utc.hotel_booking.modules.finance.repository.TransactionRepository;
import vn.edu.utc.hotel_booking.modules.finance.service.PaymentService;

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
public class PaymentServiceImpl implements PaymentService {

    PaymentRepository paymentRepository;
    TransactionRepository transactionRepository;
    BookingRepository bookingRepository;
    FinanceMapper financeMapper;
    StringRedisTemplate redisTemplate;
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
            } else {
                // Contract { bookingId, method } — số tiền lấy từ đơn đặt phòng
                if (booking.getTotalAmount() == null || booking.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new AppException(ErrorCode.INVALID_PAYMENT_AMOUNT,
                            "Đơn đặt phòng chưa có số tiền thanh toán");
                }
                payment = Payment.builder()
                        .booking(booking)
                        .totalAmount(booking.getTotalAmount())
                        .paymentPurpose(PaymentPurpose.FULL_PAYMENT)
                        .paymentMethod(request.getMethod())
                        .gatewayTxnId(txnRef)
                        .status(PaymentStatus.PENDING)
                        .build();
            }
            Payment saved = paymentRepository.save(payment);

            String orderInfo = "Thanh toan dat phong " + booking.getBookingNumber();
            String paymentUrl = gateway.createPaymentUrl(txnRef, saved.getTotalAmount(), orderInfo, ipAddress);

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
            log.warn("Callback chữ ký sai method={} payload={}", method, result.getRawPayload());
            throw new AppException(ErrorCode.PAYMENT_SIGNATURE_INVALID,
                    "Chữ ký callback không hợp lệ (" + method + ")");
        }

        Payment payment = paymentRepository.findByGatewayTxnId(result.getTxnRef())
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

        return financeMapper.toResponse(saved);
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

        return financeMapper.toResponse(saved);
    }
}
