package vn.edu.utc.hotel_booking.modules.finance.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.modules.booking.entity.*;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingLookupService;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.FolioLineItemDto;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.FolioPaymentDto;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.FolioResponse;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.FolioRoomSummaryDto;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.PaymentResponse;
import vn.edu.utc.hotel_booking.modules.finance.entity.*;
import vn.edu.utc.hotel_booking.modules.finance.repository.BillingRoutingRuleRepository;
import vn.edu.utc.hotel_booking.modules.finance.repository.MasterFolioItemAdjustmentRepository;
import vn.edu.utc.hotel_booking.modules.finance.repository.MasterFolioRepository;
import vn.edu.utc.hotel_booking.modules.finance.service.FolioService;
import vn.edu.utc.hotel_booking.modules.finance.service.PaymentService;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.ServiceOrderDetailResponse;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.ServiceOrderResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderPaymentMethod;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderPaymentStatus;
import vn.edu.utc.hotel_booking.modules.operation.service.ServiceOrderService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class FolioServiceImpl implements FolioService {

    BookingLookupService bookingLookupService;
    PaymentService paymentService;
    ServiceOrderService serviceOrderService;
    MasterFolioRepository masterFolioRepository;
    BillingRoutingRuleRepository billingRoutingRuleRepository;
    MasterFolioItemAdjustmentRepository masterFolioItemAdjustmentRepository;

    @Override
    public FolioResponse getFolioByBookingId(Long bookingId) {
        Booking booking = bookingLookupService.getBookingEntityById(bookingId);
        return buildFolioResponse(booking, null);
    }

    @Override
    public FolioResponse getFolioByBookingRoomId(Long bookingRoomId) {
        Booking booking = bookingLookupService.getBookingByRoomId(bookingRoomId);
        return buildFolioResponse(booking, bookingRoomId);
    }

    @Override
    public FolioResponse getFolioByBookingNumber(String bookingNumber) {
        Booking booking = bookingLookupService.getBookingEntityByNumber(bookingNumber);
        return buildFolioResponse(booking, null);
    }

    private FolioResponse buildFolioResponse(Booking booking, Long targetBookingRoomId) {
        List<FolioRoomSummaryDto> rooms = new ArrayList<>();
        List<FolioLineItemDto> debits = new ArrayList<>();

        BigDecimal subTotal = BigDecimal.ZERO;
        BigDecimal serviceFeeAmount = BigDecimal.ZERO;
        BigDecimal vatAmount = BigDecimal.ZERO;
        BigDecimal serviceFeeRate = booking.getServiceFeeRate() != null ? booking.getServiceFeeRate() : BigDecimal.ZERO;
        BigDecimal coveredByMasterFolioAmount = BigDecimal.ZERO;

        LocalDate checkInDate = null;
        LocalDate checkOutDate = null;

        boolean isGroupBooking = (booking.getGroupContract() != null);
        Map<ChargeCategory, BillingRoutingRule> ruleMap = new EnumMap<>(ChargeCategory.class);
        Map<String, MasterFolioItemAdjustment> adjustmentMap = new HashMap<>();

        if (isGroupBooking && masterFolioRepository != null) {
            Optional<MasterFolio> mfOpt = masterFolioRepository.findByContractId(booking.getGroupContract().getId());
            if (mfOpt.isPresent()) {
                MasterFolio mf = mfOpt.get();
                if (billingRoutingRuleRepository != null) {
                    List<BillingRoutingRule> rules = billingRoutingRuleRepository.findByMasterFolioIdAndIsActiveTrue(mf.getId());
                    if (rules != null) {
                        for (BillingRoutingRule r : rules) {
                            ruleMap.put(r.getChargeCategory(), r);
                        }
                    }
                }
                if (masterFolioItemAdjustmentRepository != null) {
                    List<MasterFolioItemAdjustment> adjs = masterFolioItemAdjustmentRepository.findByMasterFolioId(mf.getId());
                    if (adjs != null) {
                        for (MasterFolioItemAdjustment a : adjs) {
                            adjustmentMap.put(a.getItemType() + ":" + a.getReferenceId(), a);
                        }
                    }
                }
            }
        }

        if (booking.getBookingDetails() != null) {
            for (BookingDetail detail : booking.getBookingDetails()) {
                if (checkInDate == null || (detail.getCheckInDate() != null && detail.getCheckInDate().isBefore(checkInDate))) {
                    checkInDate = detail.getCheckInDate();
                }
                if (checkOutDate == null || (detail.getCheckOutDate() != null && detail.getCheckOutDate().isAfter(checkOutDate))) {
                    checkOutDate = detail.getCheckOutDate();
                }

                if (detail.getBookingRooms() == null) continue;

                for (BookingRoom br : detail.getBookingRooms()) {
                    boolean isTargetRoom = (targetBookingRoomId == null || br.getId().equals(targetBookingRoomId));

                    List<String> guestNames = new ArrayList<>();
                    if (br.getStayGuests() != null) {
                        for (StayGuest sg : br.getStayGuests()) {
                            if (sg.getFullName() != null && !sg.getFullName().isBlank()) {
                                guestNames.add(sg.getFullName());
                            }
                        }
                    }

                    FolioRoomSummaryDto roomSummary = FolioRoomSummaryDto.builder()
                            .bookingRoomId(br.getId())
                            .roomInstanceId(br.getRoomInstance() != null ? br.getRoomInstance().getId() : null)
                            .roomNumber(br.getRoomInstance() != null ? br.getRoomInstance().getRoomNumber() : null)
                            .roomTypeName(detail.getRoomTypeName())
                            .status(br.getStatus() != null ? br.getStatus().name() : null)
                            .adultCount(br.getAdultCount())
                            .childCount(br.getChildCount())
                            .guestNames(guestNames)
                            .build();

                    if (isTargetRoom) {
                        rooms.add(roomSummary);
                    }

                    if (!isTargetRoom) {
                        continue;
                    }

                    String roomNum = br.getRoomInstance() != null ? br.getRoomInstance().getRoomNumber() : "Chưa gán";

                    // 1. Tiền phòng từng đêm (Daily Rates -> ROOM_RATE)
                    if (br.getDailyRates() != null) {
                        for (BookingDailyRate dr : br.getDailyRates()) {
                            BigDecimal lineSubtotal = dr.getNetPrice() != null ? dr.getNetPrice() : BigDecimal.ZERO;
                            BigDecimal lineServiceFee = lineSubtotal.multiply(serviceFeeRate)
                                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                            BigDecimal lineTaxable = lineSubtotal.add(lineServiceFee);
                            BigDecimal vatPercent = dr.getVatPercent() != null ? dr.getVatPercent() : BigDecimal.ZERO;
                            BigDecimal lineVat = lineTaxable.multiply(vatPercent)
                                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                            BigDecimal lineTotal = lineTaxable.add(lineVat);

                            boolean isCoveredByMaster = false;
                            String routingTarget = "SUB_FOLIO";

                            if (isGroupBooking) {
                                MasterFolioItemAdjustment adj = adjustmentMap.get("ROOM_RATE:" + dr.getId());
                                if (adj != null) {
                                    isCoveredByMaster = (adj.getAdjustedTarget() == RoutingTarget.MASTER);
                                    routingTarget = adj.getAdjustedTarget().name();
                                } else {
                                    BillingRoutingRule rule = ruleMap.get(ChargeCategory.ROOM_RATE);
                                    if (rule != null && rule.getRouteTarget() == RoutingTarget.MASTER) {
                                        isCoveredByMaster = true;
                                        routingTarget = "MASTER";
                                    }
                                }
                            }

                            FolioLineItemDto lineItem = FolioLineItemDto.builder()
                                    .referenceId(dr.getId())
                                    .lineType("ROOM_RATE")
                                    .description(detail.getRoomTypeName() + " (" + dr.getStayDate() + ")")
                                    .itemDate(dr.getStayDate())
                                    .quantity(1)
                                    .unitPrice(dr.getNetPrice())
                                    .subtotal(lineSubtotal)
                                    .serviceFeeRate(serviceFeeRate)
                                    .serviceFeeAmount(lineServiceFee)
                                    .vatRate(vatPercent)
                                    .vatAmount(lineVat)
                                    .totalAmount(lineTotal)
                                    .roomNumber(roomNum)
                                    .coveredByMasterFolio(isCoveredByMaster)
                                    .routingTarget(routingTarget)
                                    .build();

                            debits.add(lineItem);
                            subTotal = subTotal.add(lineSubtotal);
                            serviceFeeAmount = serviceFeeAmount.add(lineServiceFee);
                            vatAmount = vatAmount.add(lineVat);

                            if (isCoveredByMaster) {
                                coveredByMasterFolioAmount = coveredByMasterFolioAmount.add(lineTotal);
                            }
                        }
                    }

                    // 2. Phụ phí & Phạt (BookingCharge -> SURCHARGE / PENALTY)
                    if (br.getCharges() != null) {
                        for (BookingCharge charge : br.getCharges()) {
                            String chargeType = (charge.getChargeType() == BookingChargeType.PENALTY)
                                    ? "PENALTY"
                                    : "SURCHARGE";

                            LocalDate chargeDate = charge.getCreatedAt() != null
                                    ? charge.getCreatedAt().toLocalDate()
                                    : LocalDate.now();

                            boolean isCoveredByMaster = false;
                            String routingTarget = "SUB_FOLIO";

                            if (isGroupBooking) {
                                MasterFolioItemAdjustment adj = adjustmentMap.get("BOOKING_CHARGE:" + charge.getId());
                                if (adj != null) {
                                    isCoveredByMaster = (adj.getAdjustedTarget() == RoutingTarget.MASTER);
                                    routingTarget = adj.getAdjustedTarget().name();
                                }
                            }

                            FolioLineItemDto lineItem = FolioLineItemDto.builder()
                                    .referenceId(charge.getId())
                                    .lineType(chargeType)
                                    .description(charge.getItemName() != null ? charge.getItemName() : charge.getChargeType().name())
                                    .itemDate(chargeDate)
                                    .quantity(charge.getQuantity())
                                    .unitPrice(charge.getUnitPrice())
                                    .subtotal(charge.getSubtotal())
                                    .serviceFeeRate(charge.getServiceFeeRate())
                                    .serviceFeeAmount(charge.getServiceFeeAmount())
                                    .vatRate(charge.getVatRate())
                                    .vatAmount(charge.getVatAmount())
                                    .totalAmount(charge.getTotalAmount())
                                    .roomNumber(roomNum)
                                    .coveredByMasterFolio(isCoveredByMaster)
                                    .routingTarget(routingTarget)
                                    .build();

                            debits.add(lineItem);
                            subTotal = subTotal.add(charge.getSubtotal());
                            serviceFeeAmount = serviceFeeAmount.add(charge.getServiceFeeAmount());
                            vatAmount = vatAmount.add(charge.getVatAmount());

                            if (isCoveredByMaster) {
                                coveredByMasterFolioAmount = coveredByMasterFolioAmount.add(charge.getTotalAmount());
                            }
                        }
                    }
                }
            }
        }

        // 3. Đơn dịch vụ phòng POS (ServiceOrder ghi nợ phòng POST_TO_ROOM)
        if (targetBookingRoomId == null) {
            List<ServiceOrderResponse> orders = serviceOrderService.getOrdersByBookingId(booking.getId());
            if (orders != null) {
                for (ServiceOrderResponse order : orders) {
                    if (order.getPaymentMethod() != null && order.getPaymentMethod() != ServiceOrderPaymentMethod.POST_TO_ROOM) {
                        continue;
                    }
                    if (order.getPaymentStatus() == ServiceOrderPaymentStatus.PAID) {
                        continue;
                    }

                    LocalDate orderDate = order.getIssuedAt() != null
                            ? order.getIssuedAt().toLocalDate()
                            : (order.getCreatedAt() != null ? order.getCreatedAt().toLocalDate() : LocalDate.now());

                    if (order.getDetails() != null) {
                        for (ServiceOrderDetailResponse item : order.getDetails()) {
                            String itemLineType = "PRODUCT".equalsIgnoreCase(item.getItemType())
                                    ? "PRODUCT"
                                    : "SERVICE";

                            boolean isCoveredByMaster = false;
                            String routingTarget = "SUB_FOLIO";

                            if (isGroupBooking) {
                                MasterFolioItemAdjustment adj = adjustmentMap.get("SERVICE_ORDER_DETAIL:" + item.getId());
                                if (adj != null) {
                                    isCoveredByMaster = (adj.getAdjustedTarget() == RoutingTarget.MASTER);
                                    routingTarget = adj.getAdjustedTarget().name();
                                } else {
                                    ChargeCategory cat = resolveCategory(item.getItemName());
                                    BillingRoutingRule rule = ruleMap.get(cat);
                                    if (rule != null && rule.getRouteTarget() == RoutingTarget.MASTER) {
                                        isCoveredByMaster = true;
                                        routingTarget = "MASTER";
                                    }
                                }
                            }

                            FolioLineItemDto lineItem = FolioLineItemDto.builder()
                                    .referenceId(item.getId())
                                    .lineType(itemLineType)
                                    .description(item.getItemName() != null ? item.getItemName() : "Dịch vụ/Sản phẩm POS")
                                    .itemDate(orderDate)
                                    .quantity(item.getQuantity())
                                    .unitPrice(item.getUnitPrice())
                                    .subtotal(item.getSubtotal())
                                    .serviceFeeRate(item.getServiceFeeRate())
                                    .serviceFeeAmount(item.getServiceFeeAmount())
                                    .vatRate(item.getVatRate())
                                    .vatAmount(item.getVatAmount())
                                    .totalAmount(item.getTotalAmount())
                                    .roomNumber(order.getRoomNumber())
                                    .coveredByMasterFolio(isCoveredByMaster)
                                    .routingTarget(routingTarget)
                                    .build();

                            debits.add(lineItem);
                            subTotal = subTotal.add(item.getSubtotal());
                            serviceFeeAmount = serviceFeeAmount.add(item.getServiceFeeAmount());
                            vatAmount = vatAmount.add(item.getVatAmount());

                            if (isCoveredByMaster) {
                                coveredByMasterFolioAmount = coveredByMasterFolioAmount.add(item.getTotalAmount());
                            }
                        }
                    }
                }
            }
        }

        BigDecimal totalDebits = subTotal.add(serviceFeeAmount).add(vatAmount);

        // 4. Lịch sử thanh toán & Đặt cọc (Credits)
        List<FolioPaymentDto> credits = new ArrayList<>();
        BigDecimal totalCredits = BigDecimal.ZERO;

        List<PaymentResponse> payments = paymentService.getByBookingId(booking.getId());
        if (payments != null) {
            for (PaymentResponse p : payments) {
                FolioPaymentDto paymentDto = FolioPaymentDto.builder()
                        .paymentId(p.getId())
                        .paymentMethod(p.getPaymentMethod() != null ? p.getPaymentMethod().name() : "OTHER")
                        .paymentPurpose(p.getPaymentPurpose() != null ? p.getPaymentPurpose().name() : "PAYMENT")
                        .provider(p.getPaymentProvider())
                        .referenceCode(p.getTransactionReference())
                        .amount(p.getTotalAmount())
                        .status(p.getStatus() != null ? p.getStatus().name() : null)
                        .paidAt(p.getPaidAt() != null ? p.getPaidAt() : p.getCreatedAt())
                        .build();

                credits.add(paymentDto);

                if (p.getStatus() == PaymentStatus.SUCCESS) {
                    if (p.getPaymentPurpose() == PaymentPurpose.REFUND) {
                        totalCredits = totalCredits.subtract(p.getTotalAmount());
                    } else {
                        totalCredits = totalCredits.add(p.getTotalAmount());
                    }
                }
            }
        }

        BigDecimal balance = totalDebits.subtract(totalCredits);
        BigDecimal personalBalance = balance.subtract(coveredByMasterFolioAmount);
        if (personalBalance.compareTo(BigDecimal.ZERO) < 0) {
            personalBalance = BigDecimal.ZERO;
        }

        boolean canCheckOut = isGroupBooking
                ? (personalBalance.compareTo(BigDecimal.ZERO) == 0)
                : (balance.compareTo(BigDecimal.ZERO) == 0);

        boolean hasOverpayment = balance.compareTo(BigDecimal.ZERO) < 0;
        BigDecimal overpaymentAmount = hasOverpayment ? balance.abs() : BigDecimal.ZERO;

        return FolioResponse.builder()
                .bookingId(booking.getId())
                .bookingNumber(booking.getBookingNumber())
                .bookingStatus(booking.getStatus() != null ? booking.getStatus().name() : null)
                .hotelId(booking.getHotel() != null ? booking.getHotel().getId() : null)
                .hotelName(booking.getHotel() != null ? booking.getHotel().getName() : null)
                .leadGuestName(booking.getBookingGuest() != null ? booking.getBookingGuest().getFullName() : null)
                .leadGuestPhone(booking.getBookingGuest() != null ? booking.getBookingGuest().getPhone() : null)
                .leadGuestEmail(booking.getBookingGuest() != null ? booking.getBookingGuest().getEmail() : null)
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .rooms(rooms)
                .debits(debits)
                .credits(credits)
                .subTotal(subTotal)
                .serviceFeeAmount(serviceFeeAmount)
                .vatAmount(vatAmount)
                .totalDebits(totalDebits)
                .totalCredits(totalCredits)
                .balance(balance)
                .canCheckOut(canCheckOut)
                .hasOverpayment(hasOverpayment)
                .overpaymentAmount(overpaymentAmount)
                .coveredByMasterFolioAmount(coveredByMasterFolioAmount)
                .personalBalance(personalBalance)
                .build();
    }

    private ChargeCategory resolveCategory(String itemName) {
        if (itemName == null) return ChargeCategory.CUSTOM;
        String l = itemName.toLowerCase();
        if (l.contains("minibar") || l.contains("nước suối") || l.contains("snack")) return ChargeCategory.MINIBAR;
        if (l.contains("giặt") || l.contains("laundry")) return ChargeCategory.LAUNDRY;
        if (l.contains("ăn sáng") || l.contains("breakfast")) return ChargeCategory.BREAKFAST;
        if (l.contains("tiệc") || l.contains("hội nghị") || l.contains("banquet")) return ChargeCategory.BANQUET;
        return ChargeCategory.CUSTOM;
    }
}
