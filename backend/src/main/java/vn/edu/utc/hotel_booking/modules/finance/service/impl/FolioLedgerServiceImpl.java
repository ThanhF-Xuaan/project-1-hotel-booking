package vn.edu.utc.hotel_booking.modules.finance.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.CheckoutSettlementResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.SettlementLineItemDto;
import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingLookupService;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.PaymentResponse;
import vn.edu.utc.hotel_booking.modules.finance.entity.*;
import vn.edu.utc.hotel_booking.modules.finance.repository.FolioRepository;
import vn.edu.utc.hotel_booking.modules.finance.repository.FolioTransactionRepository;
import vn.edu.utc.hotel_booking.modules.finance.service.FolioLedgerService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class FolioLedgerServiceImpl implements FolioLedgerService {

    FolioRepository folioRepository;
    FolioTransactionRepository folioTransactionRepository;
    BookingLookupService bookingLookupService;

    @Override
    @Transactional
    public Folio getOrCreateFolio(Long bookingId, Long bookingRoomId) {
        if (bookingRoomId != null) {
            Optional<Folio> existing = folioRepository.findByBookingRoomId(bookingRoomId);
            if (existing.isPresent()) {
                return existing.get();
            }
        } else {
            Optional<Folio> existing = folioRepository.findByBookingIdAndBookingRoomIdIsNull(bookingId);
            if (existing.isPresent()) {
                return existing.get();
            }
            List<Folio> bookingFolios = folioRepository.findByBookingId(bookingId);
            if (bookingFolios != null && !bookingFolios.isEmpty()) {
                return bookingFolios.stream()
                        .filter(f -> f.getStatus() == FolioStatus.OPEN && f.getBalance() != null && f.getBalance().compareTo(BigDecimal.ZERO) > 0)
                        .findFirst()
                        .orElse(bookingFolios.get(0));
            }
        }

        Booking booking = bookingLookupService.getBookingEntityById(bookingId);
        BookingRoom bookingRoom = (bookingRoomId != null) ? bookingLookupService.getBookingRoomEntityById(bookingRoomId) : null;

        String roomSuffix = "MAIN";
        if (bookingRoom != null && bookingRoom.getRoomInstance() != null && bookingRoom.getRoomInstance().getRoomNumber() != null) {
            roomSuffix = bookingRoom.getRoomInstance().getRoomNumber();
        } else if (bookingRoomId != null) {
            roomSuffix = "ROOM-" + bookingRoomId;
        }

        String folioNumber = "FOL-" + booking.getBookingNumber() + "-" + roomSuffix;

        Folio folio = Folio.builder()
                .booking(booking)
                .bookingRoom(bookingRoom)
                .hotel(booking.getHotel())
                .folioNumber(folioNumber)
                .status(FolioStatus.OPEN)
                .totalDebits(BigDecimal.ZERO)
                .totalCredits(BigDecimal.ZERO)
                .balance(BigDecimal.ZERO)
                .build();

        return folioRepository.save(folio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Folio> findFolioByBookingRoomId(Long bookingRoomId) {
        return folioRepository.findByBookingRoomId(bookingRoomId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Folio> findFolioByBookingId(Long bookingId) {
        return folioRepository.findByBookingIdAndBookingRoomIdIsNull(bookingId);
    }

    @Override
    @Transactional
    public FolioTransaction postDebit(
            Long folioId,
            String category,
            String sourceType,
            Long sourceId,
            String description,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal,
            BigDecimal serviceFeeRate,
            BigDecimal serviceFeeAmount,
            BigDecimal vatRate,
            BigDecimal vatAmount,
            BigDecimal totalAmount
    ) {
        Folio folio = folioRepository.findById(folioId)
                .orElseThrow(() -> new IllegalArgumentException("Folio không tồn tại với ID: " + folioId));

        if (sourceType != null && sourceId != null) {
            Optional<FolioTransaction> existing = folioTransactionRepository.findBySourceTypeAndSourceId(sourceType, sourceId);
            if (existing.isPresent()) {
                FolioTransaction tx = existing.get();
                tx.setCategory(category);
                tx.setDescription(description);
                tx.setQuantity(quantity != null ? quantity : 1);
                tx.setUnitPrice(unitPrice != null ? unitPrice : BigDecimal.ZERO);
                tx.setSubtotal(subtotal != null ? subtotal : BigDecimal.ZERO);
                tx.setServiceFeeRate(serviceFeeRate != null ? serviceFeeRate : BigDecimal.ZERO);
                tx.setServiceFeeAmount(serviceFeeAmount != null ? serviceFeeAmount : BigDecimal.ZERO);
                tx.setVatRate(vatRate != null ? vatRate : BigDecimal.ZERO);
                tx.setVatAmount(vatAmount != null ? vatAmount : BigDecimal.ZERO);
                tx.setAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO);
                tx = folioTransactionRepository.save(tx);
                recalculateBalance(folioId);
                return tx;
            }
        }

        FolioTransaction tx = FolioTransaction.builder()
                .folio(folio)
                .transactionType("DEBIT")
                .category(category)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .description(description)
                .quantity(quantity != null ? quantity : 1)
                .unitPrice(unitPrice != null ? unitPrice : BigDecimal.ZERO)
                .subtotal(subtotal != null ? subtotal : BigDecimal.ZERO)
                .serviceFeeRate(serviceFeeRate != null ? serviceFeeRate : BigDecimal.ZERO)
                .serviceFeeAmount(serviceFeeAmount != null ? serviceFeeAmount : BigDecimal.ZERO)
                .vatRate(vatRate != null ? vatRate : BigDecimal.ZERO)
                .vatAmount(vatAmount != null ? vatAmount : BigDecimal.ZERO)
                .amount(totalAmount != null ? totalAmount : BigDecimal.ZERO)
                .postedAt(OffsetDateTime.now())
                .postedBy("SYSTEM")
                .build();

        tx = folioTransactionRepository.save(tx);
        recalculateBalance(folioId);
        return tx;
    }

    @Override
    @Transactional
    public FolioTransaction postCredit(
            Long folioId,
            String category,
            String sourceType,
            Long sourceId,
            String description,
            BigDecimal amount,
            String paymentMethod
    ) {
        Folio folio = folioRepository.findById(folioId)
                .orElseThrow(() -> new IllegalArgumentException("Folio không tồn tại với ID: " + folioId));

        if (sourceType != null && sourceId != null) {
            Optional<FolioTransaction> existing = folioTransactionRepository.findBySourceTypeAndSourceId(sourceType, sourceId);
            if (existing.isPresent()) {
                FolioTransaction tx = existing.get();
                tx.setCategory(category);
                tx.setDescription(description);
                tx.setUnitPrice(amount != null ? amount : BigDecimal.ZERO);
                tx.setSubtotal(amount != null ? amount : BigDecimal.ZERO);
                tx.setAmount(amount != null ? amount : BigDecimal.ZERO);
                tx.setPaymentMethod(paymentMethod);
                tx = folioTransactionRepository.save(tx);
                recalculateBalance(folioId);
                return tx;
            }
        }

        FolioTransaction tx = FolioTransaction.builder()
                .folio(folio)
                .transactionType("CREDIT")
                .category(category)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .description(description)
                .quantity(1)
                .unitPrice(amount != null ? amount : BigDecimal.ZERO)
                .subtotal(amount != null ? amount : BigDecimal.ZERO)
                .serviceFeeRate(BigDecimal.ZERO)
                .serviceFeeAmount(BigDecimal.ZERO)
                .vatRate(BigDecimal.ZERO)
                .vatAmount(BigDecimal.ZERO)
                .amount(amount != null ? amount : BigDecimal.ZERO)
                .paymentMethod(paymentMethod)
                .postedAt(OffsetDateTime.now())
                .postedBy("SYSTEM")
                .build();

        tx = folioTransactionRepository.save(tx);
        recalculateBalance(folioId);
        return tx;
    }

    @Override
    @Transactional
    public void recalculateBalance(Long folioId) {
        Folio folio = folioRepository.findById(folioId)
                .orElseThrow(() -> new IllegalArgumentException("Folio không tồn tại với ID: " + folioId));

        List<FolioTransaction> transactions = folioTransactionRepository.findByFolioIdOrderByPostedAtAsc(folioId);

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (FolioTransaction tx : transactions) {
            if ("DEBIT".equalsIgnoreCase(tx.getTransactionType())) {
                totalDebits = totalDebits.add(tx.getAmount());
            } else if ("CREDIT".equalsIgnoreCase(tx.getTransactionType())) {
                totalCredits = totalCredits.add(tx.getAmount());
            }
        }

        BigDecimal balance = totalDebits.subtract(totalCredits);

        folio.setTotalDebits(totalDebits);
        folio.setTotalCredits(totalCredits);
        folio.setBalance(balance);

        if (balance.compareTo(BigDecimal.ZERO) <= 0 && folio.getStatus() == FolioStatus.LOCKED_FOR_SETTLEMENT) {
            folio.setStatus(FolioStatus.SETTLED);
        }

        folioRepository.save(folio);
    }

    @Override
    @Transactional(readOnly = true)
    public CheckoutSettlementResponse getSettlementByBookingRoomId(Long bookingRoomId) {
        BookingRoom bookingRoom = bookingLookupService.getBookingRoomEntityById(bookingRoomId);
        Booking booking = bookingLookupService.getBookingByRoomId(bookingRoomId);

        Optional<Folio> folioOpt = folioRepository.findByBookingRoomId(bookingRoomId);
        if (folioOpt.isEmpty()) {
            folioOpt = folioRepository.findByBookingIdAndBookingRoomIdIsNull(booking.getId());
        }

        if (folioOpt.isEmpty()) {
            return null; // Chưa có folio trong DB, fallback sang logic truyền thống
        }

        Folio folio = folioOpt.get();
        List<FolioTransaction> transactions = folioTransactionRepository.findByFolioIdOrderByPostedAtAsc(folio.getId());

        List<SettlementLineItemDto> lineItems = new ArrayList<>();
        List<PaymentResponse> paymentHistory = new ArrayList<>();

        BigDecimal totalRoomCharges = BigDecimal.ZERO;
        BigDecimal totalSurcharges = BigDecimal.ZERO;
        BigDecimal totalServiceCharges = BigDecimal.ZERO;
        BigDecimal totalNoShowPenalty = BigDecimal.ZERO;

        for (FolioTransaction tx : transactions) {
            if ("DEBIT".equalsIgnoreCase(tx.getTransactionType())) {
                SettlementLineItemDto lineItem = SettlementLineItemDto.builder()
                        .lineType(tx.getCategory())
                        .referenceId(tx.getSourceId())
                        .description(tx.getDescription())
                        .quantity(tx.getQuantity())
                        .unitPrice(tx.getUnitPrice())
                        .subtotal(tx.getSubtotal())
                        .serviceFeeRate(tx.getServiceFeeRate())
                        .serviceFeeAmount(tx.getServiceFeeAmount())
                        .vatRate(tx.getVatRate())
                        .vatAmount(tx.getVatAmount())
                        .totalAmount(tx.getAmount())
                        .paymentMethod(tx.getPaymentMethod())
                        .build();

                lineItems.add(lineItem);

                if ("ROOM_RATE".equalsIgnoreCase(tx.getCategory())) {
                    totalRoomCharges = totalRoomCharges.add(tx.getAmount());
                } else if ("PENALTY".equalsIgnoreCase(tx.getCategory())) {
                    totalNoShowPenalty = totalNoShowPenalty.add(tx.getAmount());
                } else if ("SURCHARGE".equalsIgnoreCase(tx.getCategory()) || tx.getCategory().contains("CHECKIN") || tx.getCategory().contains("CHECKOUT")) {
                    totalSurcharges = totalSurcharges.add(tx.getAmount());
                } else {
                    totalServiceCharges = totalServiceCharges.add(tx.getAmount());
                }
            } else if ("CREDIT".equalsIgnoreCase(tx.getTransactionType())) {
                PaymentMethod pm = PaymentMethod.CASH;
                if (tx.getPaymentMethod() != null) {
                    try {
                        pm = PaymentMethod.valueOf(tx.getPaymentMethod());
                    } catch (IllegalArgumentException ignored) {}
                }

                PaymentResponse pResp = PaymentResponse.builder()
                        .paymentMethod(pm)
                        .paymentPurpose(PaymentPurpose.FULL_PAYMENT)
                        .transactionReference(tx.getDescription())
                        .totalAmount(tx.getAmount())
                        .status(PaymentStatus.SUCCESS)
                        .paidAt(tx.getPostedAt())
                        .build();

                paymentHistory.add(pResp);
            }
        }

        BigDecimal grandTotal = folio.getTotalDebits();
        BigDecimal totalPaid = folio.getTotalCredits();
        BigDecimal balanceDue = folio.getBalance().compareTo(BigDecimal.ZERO) > 0 ? folio.getBalance() : BigDecimal.ZERO;
        BigDecimal netOverpaymentAmount = folio.getBalance().compareTo(BigDecimal.ZERO) < 0 ? folio.getBalance().abs() : BigDecimal.ZERO;
        boolean canCheckOut = balanceDue.compareTo(BigDecimal.ZERO) <= 0;

        String roomNumber = bookingRoom.getRoomInstance() != null ? bookingRoom.getRoomInstance().getRoomNumber() : "N/A";
        String roomTypeName = bookingRoom.getBookingDetail() != null ? bookingRoom.getBookingDetail().getRoomTypeName() : "N/A";
        String guestName = booking.getBookingGuest() != null ? booking.getBookingGuest().getFullName() : "N/A";
        String guestPhone = booking.getBookingGuest() != null && booking.getBookingGuest().getPhone() != null ? booking.getBookingGuest().getPhone() : "";

        return CheckoutSettlementResponse.builder()
                .bookingId(booking.getId())
                .bookingRoomId(bookingRoom.getId())
                .bookingNumber(booking.getBookingNumber())
                .roomNumber(roomNumber)
                .roomTypeName(roomTypeName)
                .guestName(guestName)
                .guestPhone(guestPhone)
                .totalRoomCharges(totalRoomCharges)
                .totalSurcharges(totalSurcharges)
                .noShowPenaltyAmount(totalNoShowPenalty)
                .totalServiceCharges(totalServiceCharges)
                .grandTotal(grandTotal)
                .totalPaid(totalPaid)
                .balanceDue(balanceDue)
                .netOverpaymentAmount(netOverpaymentAmount)
                .canCheckOut(canCheckOut)
                .lineItems(lineItems)
                .paymentHistory(paymentHistory)
                .build();
    }

    @Override
    @Transactional
    public void updateFolioStatus(Long folioId, FolioStatus status) {
        folioRepository.findById(folioId).ifPresent(folio -> {
            folio.setStatus(status);
            folioRepository.save(folio);
        });
    }

    @Override
    @Transactional
    public void closeFolio(Long bookingId) {
        List<Folio> folios = folioRepository.findByBookingId(bookingId);
        for (Folio folio : folios) {
            folio.setStatus(FolioStatus.CLOSED);
            folio.setClosedAt(OffsetDateTime.now());
            folioRepository.save(folio);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalDebitsByBookingId(Long bookingId) {
        List<Folio> folios = folioRepository.findByBookingId(bookingId);
        if (folios == null || folios.isEmpty()) {
            return null;
        }
        return folios.stream()
                .map(Folio::getTotalDebits)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getRemainingBalanceByBookingId(Long bookingId) {
        List<Folio> folios = folioRepository.findByBookingId(bookingId);
        if (folios == null || folios.isEmpty()) {
            return null;
        }
        BigDecimal totalBalance = folios.stream()
                .map(Folio::getBalance)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalBalance.compareTo(BigDecimal.ZERO) > 0 ? totalBalance : BigDecimal.ZERO;
    }
}

