package vn.edu.utc.hotel_booking.modules.finance.service.impl;

import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
import vn.edu.utc.hotel_booking.modules.finance.mapper.FinanceMapper;
import vn.edu.utc.hotel_booking.modules.finance.repository.PaymentRepository;
import vn.edu.utc.hotel_booking.modules.finance.repository.TransactionRepository;
import vn.edu.utc.hotel_booking.modules.finance.service.PaymentService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    PaymentRepository paymentRepository;
    TransactionRepository transactionRepository;
    BookingRepository bookingRepository;
    FinanceMapper financeMapper;

    @Override
    @Transactional
    public PaymentResponse createPayment(PaymentCreateRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND, "Không tìm thấy đơn đặt phòng: " + request.getBookingId()));

        if (request.getTotalAmount() == null || request.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_AMOUNT, "Số tiền thanh toán phải lớn hơn 0");
        }

        boolean isImmediateSuccess = request.getPaymentMethod() == PaymentMethod.CASH || request.getPaymentMethod() == PaymentMethod.DEBIT_CARD;
        PaymentStatus initialStatus = isImmediateSuccess ? PaymentStatus.SUCCESS : PaymentStatus.PENDING;
        OffsetDateTime paidAt = isImmediateSuccess ? OffsetDateTime.now() : null;

        Payment payment = Payment.builder()
                .booking(booking)
                .totalAmount(request.getTotalAmount())
                .paymentPurpose(request.getPaymentPurpose() != null ? request.getPaymentPurpose() : PaymentPurpose.FULL_PAYMENT)
                .paymentMethod(request.getPaymentMethod())
                .paymentProvider(request.getPaymentProvider())
                .transactionReference(request.getTransactionReference())
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
                    .referenceCode("TX-" + System.currentTimeMillis())
                    .status("COMPLETED")
                    .issuedAt(OffsetDateTime.now())
                    .build();
            transactionRepository.save(transaction);
        }

        return financeMapper.toResponse(savedPayment);
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
                .referenceCode("TX-" + System.currentTimeMillis())
                .status("COMPLETED")
                .issuedAt(OffsetDateTime.now())
                .build();
        transactionRepository.save(transaction);

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
