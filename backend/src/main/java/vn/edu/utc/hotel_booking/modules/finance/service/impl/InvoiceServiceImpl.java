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
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingCharge;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingDailyRate;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingDetail;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRepository;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.InvoiceCreateRequest;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.InvoiceSearchDto;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.InvoiceResponse;
import vn.edu.utc.hotel_booking.modules.finance.entity.Invoice;
import vn.edu.utc.hotel_booking.modules.finance.entity.InvoiceDetail;
import vn.edu.utc.hotel_booking.modules.finance.entity.InvoiceLineType;
import vn.edu.utc.hotel_booking.modules.finance.entity.InvoiceStatus;
import vn.edu.utc.hotel_booking.modules.finance.mapper.FinanceMapper;
import vn.edu.utc.hotel_booking.modules.finance.repository.InvoiceRepository;
import vn.edu.utc.hotel_booking.modules.finance.service.InvoiceService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class InvoiceServiceImpl implements InvoiceService {

    InvoiceRepository invoiceRepository;
    BookingRepository bookingRepository;
    FinanceMapper financeMapper;

    @Override
    @Transactional
    public InvoiceResponse createInvoice(InvoiceCreateRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND, "Không tìm thấy đơn đặt phòng: " + request.getBookingId()));

        String invoiceNumber = generateInvoiceNumber();

        BigDecimal serviceFeeRate = request.getServiceFeeRate() != null ? request.getServiceFeeRate() : booking.getServiceFeeRate();

        Invoice invoice = Invoice.builder()
                .booking(booking)
                .invoiceNumber(invoiceNumber)
                .serviceFeeRate(serviceFeeRate)
                .status(InvoiceStatus.DRAFT)
                .build();

        List<InvoiceDetail> details = new ArrayList<>();
        BigDecimal subTotal = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;

        // Snapshot bất biến từng dòng tiền theo quy chuẩn tài chính (§3 hotel-concurrency-and-pricing.md)
        for (BookingDetail detail : booking.getBookingDetails()) {
            for (BookingRoom br : detail.getBookingRooms()) {
                for (BookingDailyRate dr : br.getDailyRates()) {
                    BigDecimal lineSubtotal = dr.getNetPrice();
                    BigDecimal lineServiceFee = lineSubtotal.multiply(serviceFeeRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    BigDecimal lineTaxable = lineSubtotal.add(lineServiceFee);
                    BigDecimal lineVat = lineTaxable.multiply(dr.getVatPercent()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    BigDecimal lineTotal = lineTaxable.add(lineVat);

                    String roomPrefix = (br.getRoomInstance() != null && br.getRoomInstance().getRoomNumber() != null)
                            ? "Phòng " + br.getRoomInstance().getRoomNumber() + " - "
                            : "";

                    InvoiceDetail invDetail = InvoiceDetail.builder()
                            .invoice(invoice)
                            .referenceId(dr.getId())
                            .lineType(InvoiceLineType.ROOM_RATE)
                            .description(roomPrefix + detail.getRoomTypeName() + " (" + dr.getStayDate() + ")")
                            .quantity(1)
                            .unitPrice(dr.getNetPrice())
                            .subtotal(lineSubtotal)
                            .serviceFeeRate(serviceFeeRate)
                            .serviceFeeAmount(lineServiceFee)
                            .vatRate(dr.getVatPercent())
                            .vatAmount(lineVat)
                            .totalAmount(lineTotal)
                            .build();

                    details.add(invDetail);
                    subTotal = subTotal.add(lineSubtotal);
                    totalVat = totalVat.add(lineVat);
                }

                for (BookingCharge charge : br.getCharges()) {
                    String roomSuffix = (br.getRoomInstance() != null && br.getRoomInstance().getRoomNumber() != null)
                            ? " [Phòng " + br.getRoomInstance().getRoomNumber() + "]"
                            : "";

                    InvoiceDetail invDetail = InvoiceDetail.builder()
                            .invoice(invoice)
                            .referenceId(charge.getId())
                            .lineType(InvoiceLineType.SURCHARGE)
                            .description((charge.getItemName() != null ? charge.getItemName() : charge.getChargeType().name()) + roomSuffix)
                            .quantity(charge.getQuantity())
                            .unitPrice(charge.getUnitPrice())
                            .subtotal(charge.getSubtotal())
                            .serviceFeeRate(charge.getServiceFeeRate())
                            .serviceFeeAmount(charge.getServiceFeeAmount())
                            .vatRate(charge.getVatRate())
                            .vatAmount(charge.getVatAmount())
                            .totalAmount(charge.getTotalAmount())
                            .build();

                    details.add(invDetail);
                    subTotal = subTotal.add(charge.getSubtotal());
                    totalVat = totalVat.add(charge.getVatAmount());
                }
            }
        }

        BigDecimal serviceFeeAmount = subTotal.multiply(serviceFeeRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = subTotal.add(serviceFeeAmount).add(totalVat);

        invoice.setDetails(details);
        invoice.setSubTotal(subTotal);
        invoice.setServiceFeeAmount(serviceFeeAmount);
        invoice.setVatAmount(totalVat);
        invoice.setGrandTotal(grandTotal);

        Invoice saved = invoiceRepository.save(invoice);
        return financeMapper.toResponse(saved);
    }

    @Override
    public PageResponse<InvoiceResponse> filter(InvoiceSearchDto searchDto) {
        int page = searchDto.getPage() != null && searchDto.getPage() > 0 ? searchDto.getPage() - 1 : 0;
        int size = searchDto.getPageSize() != null && searchDto.getPageSize() > 0 ? searchDto.getPageSize() : 10;

        Specification<Invoice> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (searchDto.getBookingId() != null) {
                predicates.add(cb.equal(root.get("booking").get("id"), searchDto.getBookingId()));
            }
            if (searchDto.getInvoiceNumber() != null && !searchDto.getInvoiceNumber().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("invoiceNumber")),
                        "%" + searchDto.getInvoiceNumber().trim().toLowerCase() + "%"));
            }
            if (searchDto.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), searchDto.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Invoice> pageResult = invoiceRepository.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.from(pageResult.map(financeMapper::toResponse));
    }

    @Override
    public InvoiceResponse getById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_FOUND, "Không tìm thấy hóa đơn ID: " + id));
        return financeMapper.toResponse(invoice);
    }

    @Override
    public InvoiceResponse getByBookingId(Long bookingId) {
        List<Invoice> invoices = invoiceRepository.findByBookingId(bookingId);
        if (invoices == null || invoices.isEmpty()) {
            throw new AppException(ErrorCode.INVOICE_NOT_FOUND, "Chưa có hóa đơn cho đơn đặt phòng: " + bookingId);
        }
        return financeMapper.toResponse(invoices.get(0));
    }

    @Override
    @Transactional
    public InvoiceResponse issueInvoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_FOUND, "Không tìm thấy hóa đơn ID: " + id));

        if (invoice.getStatus() == InvoiceStatus.ISSUED) {
            throw new AppException(ErrorCode.INVOICE_ALREADY_ISSUED, "Hóa đơn này đã được phát hành trước đó");
        }

        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(OffsetDateTime.now());
        Invoice saved = invoiceRepository.save(invoice);

        return financeMapper.toResponse(saved);
    }

    private String generateInvoiceNumber() {
        return "INV" + System.currentTimeMillis() + (int) (Math.random() * 900 + 100);
    }
}
