package vn.edu.utc.hotel_booking.modules.finance.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.entity.*;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingLookupService;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.BillingRoutingRuleRequest;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.MasterFolioAdjustmentRequest;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.MasterFolioStatusUpdateRequest;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.BillingRoutingRuleResponse;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.MasterFolioLineItemDto;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.MasterFolioResponse;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.SubFolioSummaryDto;
import vn.edu.utc.hotel_booking.modules.finance.entity.*;
import vn.edu.utc.hotel_booking.modules.finance.repository.BillingRoutingRuleRepository;
import vn.edu.utc.hotel_booking.modules.finance.repository.MasterFolioItemAdjustmentRepository;
import vn.edu.utc.hotel_booking.modules.finance.repository.MasterFolioRepository;
import vn.edu.utc.hotel_booking.modules.finance.service.MasterFolioService;
import vn.edu.utc.hotel_booking.modules.identity.service.AuditLogService;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.ServiceOrderDetailResponse;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.ServiceOrderResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderPaymentMethod;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderPaymentStatus;
import vn.edu.utc.hotel_booking.modules.operation.service.ServiceOrderService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MasterFolioServiceImpl implements MasterFolioService {

    MasterFolioRepository masterFolioRepository;
    BillingRoutingRuleRepository billingRoutingRuleRepository;
    MasterFolioItemAdjustmentRepository masterFolioItemAdjustmentRepository;
    BookingLookupService bookingLookupService;
    ServiceOrderService serviceOrderService;
    AuditLogService auditLogService;

    @Override
    @Transactional
    public MasterFolio createMasterFolioForContract(GroupContract contract, BigDecimal depositAmount) {
        String folioNumber = generateFolioNumber();

        BigDecimal subtotal = contract.getSubtotalAmount() != null ? contract.getSubtotalAmount() : BigDecimal.ZERO;
        BigDecimal serviceFee = contract.getServiceFeeAmount() != null ? contract.getServiceFeeAmount() : BigDecimal.ZERO;
        BigDecimal vat = contract.getVatAmount() != null ? contract.getVatAmount() : BigDecimal.ZERO;
        BigDecimal total = contract.getTotalContractValue() != null ? contract.getTotalContractValue() : BigDecimal.ZERO;
        BigDecimal deposit = depositAmount != null ? depositAmount : BigDecimal.ZERO;
        BigDecimal balance = total.subtract(deposit);

        String companyAddress = contract.getCompany() != null && contract.getCompany().getAddress() != null
                ? contract.getCompany().getAddress() : "Địa chỉ đăng ký kinh doanh theo GPKD";
        String companyEmail = contract.getCompany() != null && contract.getCompany().getContactEmail() != null
                ? contract.getCompany().getContactEmail() : "accounting@" + contract.getCompanyTaxCode() + ".vn";

        MasterFolio masterFolio = MasterFolio.builder()
                .folioNumber(folioNumber)
                .contract(contract)
                .hotel(contract.getHotel())
                .company(contract.getCompany())
                .buyerLegalName(contract.getGroupName())
                .buyerTaxCode(contract.getCompanyTaxCode())
                .buyerAddress(companyAddress)
                .buyerEmailForInvoice(companyEmail)
                .subtotalAmount(subtotal)
                .serviceFeeAmount(serviceFee)
                .totalVatAmount(vat)
                .totalAmount(total)
                .depositCredited(deposit)
                .totalPaid(deposit)
                .balance(balance)
                .status(MasterFolioStatus.OPEN)
                .build();

        List<BillingRoutingRule> rules = new ArrayList<>();

        // 1. Tiền phòng (ROOM_RATE) -> Thanh toán bởi Hợp đồng đoàn (MASTER)
        rules.add(BillingRoutingRule.builder()
                .masterFolio(masterFolio)
                .chargeCategory(ChargeCategory.ROOM_RATE)
                .routeTarget(RoutingTarget.MASTER)
                .isActive(true)
                .build());

        // 2. Tiệc/Hội nghị đoàn (BANQUET) -> Thanh toán bởi Hợp đồng đoàn (MASTER)
        rules.add(BillingRoutingRule.builder()
                .masterFolio(masterFolio)
                .chargeCategory(ChargeCategory.BANQUET)
                .routeTarget(RoutingTarget.MASTER)
                .isActive(true)
                .build());

        // 3. Minibar phòng (MINIBAR) -> Khách lẻ tự chi trả (SUB_FOLIO)
        rules.add(BillingRoutingRule.builder()
                .masterFolio(masterFolio)
                .chargeCategory(ChargeCategory.MINIBAR)
                .routeTarget(RoutingTarget.SUB_FOLIO)
                .isActive(true)
                .build());

        // 4. Giặt ủi (LAUNDRY) -> Khách lẻ tự chi trả (SUB_FOLIO)
        rules.add(BillingRoutingRule.builder()
                .masterFolio(masterFolio)
                .chargeCategory(ChargeCategory.LAUNDRY)
                .routeTarget(RoutingTarget.SUB_FOLIO)
                .isActive(true)
                .build());

        masterFolio.setRoutingRules(rules);
        MasterFolio saved = masterFolioRepository.save(masterFolio);

        auditLogService.recordLog(
                null,
                "CREATE_MASTER_FOLIO",
                "MASTER_FOLIO",
                String.valueOf(saved.getId()),
                null,
                "Tự động khởi tạo Master Folio " + saved.getFolioNumber() + " khi xác nhận cọc 50%",
                "SYSTEM",
                "MasterFolioService"
        );

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MasterFolio> getByContractId(Long contractId) {
        return masterFolioRepository.findByContractId(contractId);
    }

    @Override
    @Transactional
    public MasterFolioResponse getMasterFolioById(Long masterFolioId) {
        MasterFolio masterFolio = masterFolioRepository.findById(masterFolioId)
                .orElseThrow(() -> new AppException(ErrorCode.MASTER_FOLIO_NOT_FOUND));
        return buildMasterFolioResponse(masterFolio);
    }

    @Override
    @Transactional
    public MasterFolioResponse getMasterFolioByContractId(Long contractId) {
        MasterFolio masterFolio = masterFolioRepository.findByContractId(contractId)
                .orElseThrow(() -> new AppException(ErrorCode.MASTER_FOLIO_NOT_FOUND));
        return buildMasterFolioResponse(masterFolio);
    }

    @Override
    @Transactional
    public MasterFolioResponse getMasterFolioByNumber(String folioNumber) {
        MasterFolio masterFolio = masterFolioRepository.findByFolioNumber(folioNumber)
                .orElseThrow(() -> new AppException(ErrorCode.MASTER_FOLIO_NOT_FOUND));
        return buildMasterFolioResponse(masterFolio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillingRoutingRuleResponse> getRoutingRules(Long masterFolioId) {
        if (!masterFolioRepository.existsById(masterFolioId)) {
            throw new AppException(ErrorCode.MASTER_FOLIO_NOT_FOUND);
        }
        List<BillingRoutingRule> rules = billingRoutingRuleRepository.findByMasterFolioIdAndIsActiveTrue(masterFolioId);
        List<BillingRoutingRuleResponse> responses = new ArrayList<>();
        for (BillingRoutingRule r : rules) {
            responses.add(mapToRuleResponse(r));
        }
        return responses;
    }

    @Override
    @Transactional
    public BillingRoutingRuleResponse addRoutingRule(Long masterFolioId, BillingRoutingRuleRequest request) {
        MasterFolio masterFolio = masterFolioRepository.findById(masterFolioId)
                .orElseThrow(() -> new AppException(ErrorCode.MASTER_FOLIO_NOT_FOUND));

        if (masterFolio.getStatus() == MasterFolioStatus.CLOSED) {
            throw new AppException(ErrorCode.MASTER_FOLIO_ALREADY_CLOSED);
        }

        Optional<BillingRoutingRule> existing = billingRoutingRuleRepository
                .findByMasterFolioIdAndChargeCategoryAndIsActiveTrue(masterFolioId, request.getChargeCategory());

        BillingRoutingRule rule;
        if (existing.isPresent()) {
            rule = existing.get();
            rule.setRouteTarget(request.getRouteTarget());
            rule.setMaxCoveredAmount(request.getMaxCoveredAmount());
            rule.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        } else {
            rule = BillingRoutingRule.builder()
                    .masterFolio(masterFolio)
                    .chargeCategory(request.getChargeCategory())
                    .routeTarget(request.getRouteTarget())
                    .maxCoveredAmount(request.getMaxCoveredAmount())
                    .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                    .build();
        }

        BillingRoutingRule saved = billingRoutingRuleRepository.save(rule);

        auditLogService.recordLog(
                null,
                "ADD_BILLING_ROUTING_RULE",
                "BILLING_ROUTING_RULE",
                String.valueOf(saved.getId()),
                null,
                "Thiết lập quy tắc định tuyến: " + saved.getChargeCategory() + " -> " + saved.getRouteTarget(),
                "STAFF",
                "MasterFolioService"
        );

        return mapToRuleResponse(saved);
    }

    @Override
    @Transactional
    public BillingRoutingRuleResponse updateRoutingRule(Long masterFolioId, Long ruleId, BillingRoutingRuleRequest request) {
        BillingRoutingRule rule = billingRoutingRuleRepository.findById(ruleId)
                .orElseThrow(() -> new AppException(ErrorCode.BILLING_ROUTING_RULE_NOT_FOUND));

        if (!rule.getMasterFolio().getId().equals(masterFolioId)) {
            throw new AppException(ErrorCode.BILLING_ROUTING_RULE_NOT_FOUND);
        }

        if (rule.getMasterFolio().getStatus() == MasterFolioStatus.CLOSED) {
            throw new AppException(ErrorCode.MASTER_FOLIO_ALREADY_CLOSED);
        }

        rule.setRouteTarget(request.getRouteTarget());
        rule.setMaxCoveredAmount(request.getMaxCoveredAmount());
        if (request.getIsActive() != null) {
            rule.setIsActive(request.getIsActive());
        }

        BillingRoutingRule saved = billingRoutingRuleRepository.save(rule);

        auditLogService.recordLog(
                null,
                "UPDATE_BILLING_ROUTING_RULE",
                "BILLING_ROUTING_RULE",
                String.valueOf(saved.getId()),
                null,
                "Cập nhật quy tắc định tuyến ID " + saved.getId() + " sang " + saved.getRouteTarget(),
                "STAFF",
                "MasterFolioService"
        );

        return mapToRuleResponse(saved);
    }

    @Override
    @Transactional
    public void deleteRoutingRule(Long masterFolioId, Long ruleId) {
        BillingRoutingRule rule = billingRoutingRuleRepository.findById(ruleId)
                .orElseThrow(() -> new AppException(ErrorCode.BILLING_ROUTING_RULE_NOT_FOUND));

        if (!rule.getMasterFolio().getId().equals(masterFolioId)) {
            throw new AppException(ErrorCode.BILLING_ROUTING_RULE_NOT_FOUND);
        }

        if (rule.getMasterFolio().getStatus() == MasterFolioStatus.CLOSED) {
            throw new AppException(ErrorCode.MASTER_FOLIO_ALREADY_CLOSED);
        }

        rule.setIsActive(false);
        billingRoutingRuleRepository.save(rule);

        auditLogService.recordLog(
                null,
                "DEACTIVATE_BILLING_ROUTING_RULE",
                "BILLING_ROUTING_RULE",
                String.valueOf(rule.getId()),
                null,
                "Vô hiệu hóa quy tắc định tuyến " + rule.getChargeCategory(),
                "STAFF",
                "MasterFolioService"
        );
    }

    @Override
    @Transactional
    public MasterFolioResponse adjustItemRouting(Long masterFolioId, MasterFolioAdjustmentRequest request, String staffUsername) {
        MasterFolio masterFolio = masterFolioRepository.findById(masterFolioId)
                .orElseThrow(() -> new AppException(ErrorCode.MASTER_FOLIO_NOT_FOUND));

        if (masterFolio.getStatus() == MasterFolioStatus.CLOSED) {
            throw new AppException(ErrorCode.MASTER_FOLIO_ALREADY_CLOSED);
        }

        BookingRoom bookingRoom = null;
        if (request.getBookingRoomId() != null) {
            try {
                bookingRoom = bookingLookupService.getBookingRoomEntityById(request.getBookingRoomId());
            } catch (Exception ignored) {
                // optional
            }
        }

        MasterFolioItemAdjustment adjustment = MasterFolioItemAdjustment.builder()
                .masterFolio(masterFolio)
                .bookingRoom(bookingRoom)
                .chargeCategory(request.getChargeCategory())
                .itemType(request.getItemType())
                .referenceId(request.getReferenceId())
                .itemDescription(request.getItemDescription())
                .adjustedTarget(request.getAdjustedTarget())
                .amount(request.getAmount())
                .adjustmentReason(request.getAdjustmentReason())
                .adjustedBy(staffUsername != null ? staffUsername : "STAFF")
                .build();

        masterFolioItemAdjustmentRepository.save(adjustment);

        auditLogService.recordLog(
                null,
                "MANUAL_ROUTING_ADJUSTMENT",
                "MASTER_FOLIO",
                String.valueOf(masterFolio.getId()),
                null,
                "Điều chuyển thủ công " + request.getItemDescription() + " (" + request.getAmount() + " VNĐ) sang " + request.getAdjustedTarget() + ". Lý do: " + request.getAdjustmentReason(),
                staffUsername != null ? staffUsername : "STAFF",
                "MasterFolioService"
        );

        return buildMasterFolioResponse(masterFolio);
    }

    @Override
    @Transactional
    public MasterFolioResponse updateMasterFolioStatus(Long masterFolioId, MasterFolioStatusUpdateRequest request) {
        MasterFolio masterFolio = masterFolioRepository.findById(masterFolioId)
                .orElseThrow(() -> new AppException(ErrorCode.MASTER_FOLIO_NOT_FOUND));

        MasterFolioStatus oldStatus = masterFolio.getStatus();
        MasterFolioStatus newStatus = request.getStatus();

        masterFolio.setStatus(newStatus);
        if (newStatus == MasterFolioStatus.CLOSED) {
            masterFolio.setClosedAt(OffsetDateTime.now());
        }

        MasterFolio saved = masterFolioRepository.save(masterFolio);

        auditLogService.recordLog(
                null,
                "UPDATE_MASTER_FOLIO_STATUS",
                "MASTER_FOLIO",
                String.valueOf(saved.getId()),
                oldStatus.name(),
                newStatus.name(),
                staffUsernameOrSystem("STAFF"),
                "MasterFolioService"
        );

        return buildMasterFolioResponse(saved);
    }

    private MasterFolioResponse buildMasterFolioResponse(MasterFolio masterFolio) {
        List<BillingRoutingRule> activeRules = billingRoutingRuleRepository.findByMasterFolioIdAndIsActiveTrue(masterFolio.getId());
        Map<ChargeCategory, BillingRoutingRule> ruleMap = new EnumMap<>(ChargeCategory.class);
        List<BillingRoutingRuleResponse> ruleResponses = new ArrayList<>();

        for (BillingRoutingRule rule : activeRules) {
            ruleMap.put(rule.getChargeCategory(), rule);
            ruleResponses.add(mapToRuleResponse(rule));
        }

        List<MasterFolioItemAdjustment> adjustments = masterFolioItemAdjustmentRepository.findByMasterFolioId(masterFolio.getId());
        Map<String, MasterFolioItemAdjustment> adjustmentMap = new HashMap<>();
        for (MasterFolioItemAdjustment adj : adjustments) {
            adjustmentMap.put(adj.getItemType() + ":" + adj.getReferenceId(), adj);
        }

        List<Booking> bookings = bookingLookupService.getBookingsByContractId(masterFolio.getContract().getId());

        List<MasterFolioLineItemDto> masterDebits = new ArrayList<>();
        List<SubFolioSummaryDto> subFolios = new ArrayList<>();

        BigDecimal totalSubtotal = BigDecimal.ZERO;
        BigDecimal totalServiceFee = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;

        if (bookings != null && !bookings.isEmpty()) {
            for (Booking booking : bookings) {
                BigDecimal bookingPersonalDebit = BigDecimal.ZERO;

                if (booking.getBookingDetails() != null) {
                    for (BookingDetail detail : booking.getBookingDetails()) {
                        if (detail.getBookingRooms() == null) continue;

                        for (BookingRoom br : detail.getBookingRooms()) {
                            BigDecimal roomCoveredByMaster = BigDecimal.ZERO;
                            BigDecimal roomPersonalDebit = BigDecimal.ZERO;
                            String roomNumber = br.getRoomInstance() != null ? br.getRoomInstance().getRoomNumber() : "Chưa gán";

                            String guestName = null;
                            if (br.getStayGuests() != null && !br.getStayGuests().isEmpty()) {
                                guestName = br.getStayGuests().get(0).getFullName();
                            } else if (booking.getBookingGuest() != null) {
                                guestName = booking.getBookingGuest().getFullName();
                            }

                            // 1. Tiền phòng từng đêm (Daily Rates)
                            if (br.getDailyRates() != null) {
                                for (BookingDailyRate dr : br.getDailyRates()) {
                                    BigDecimal lineSubtotal = dr.getNetPrice() != null ? dr.getNetPrice() : BigDecimal.ZERO;
                                    BigDecimal serviceFeeRate = booking.getServiceFeeRate() != null ? booking.getServiceFeeRate() : BigDecimal.ZERO;
                                    BigDecimal lineServiceFee = lineSubtotal.multiply(serviceFeeRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                                    BigDecimal lineTaxable = lineSubtotal.add(lineServiceFee);
                                    BigDecimal vatPercent = dr.getVatPercent() != null ? dr.getVatPercent() : BigDecimal.ZERO;
                                    BigDecimal lineVat = lineTaxable.multiply(vatPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                                    BigDecimal lineTotal = lineTaxable.add(lineVat);

                                    String adjKey = "ROOM_RATE:" + dr.getId();
                                    MasterFolioItemAdjustment manualAdj = adjustmentMap.get(adjKey);

                                    RoutingTarget finalTarget = RoutingTarget.MASTER;
                                    boolean isAdjusted = false;
                                    String adjReason = null;

                                    if (manualAdj != null) {
                                        finalTarget = manualAdj.getAdjustedTarget();
                                        isAdjusted = true;
                                        adjReason = manualAdj.getAdjustmentReason();
                                    } else {
                                        BillingRoutingRule rule = ruleMap.get(ChargeCategory.ROOM_RATE);
                                        if (rule != null) {
                                            finalTarget = rule.getRouteTarget();
                                        }
                                    }

                                    if (finalTarget == RoutingTarget.MASTER) {
                                        masterDebits.add(MasterFolioLineItemDto.builder()
                                                .referenceId(dr.getId())
                                                .itemType("ROOM_RATE")
                                                .chargeCategory(ChargeCategory.ROOM_RATE)
                                                .description(detail.getRoomTypeName() + " (" + dr.getStayDate() + ")")
                                                .itemDate(dr.getStayDate())
                                                .roomNumber(roomNumber)
                                                .guestName(guestName)
                                                .quantity(1)
                                                .unitPrice(dr.getNetPrice())
                                                .subtotal(lineSubtotal)
                                                .serviceFeeRate(serviceFeeRate)
                                                .serviceFeeAmount(lineServiceFee)
                                                .vatRate(vatPercent)
                                                .vatAmount(lineVat)
                                                .totalAmount(lineTotal)
                                                .routingTarget(RoutingTarget.MASTER)
                                                .isManuallyAdjusted(isAdjusted)
                                                .adjustmentReason(adjReason)
                                                .build());

                                        totalSubtotal = totalSubtotal.add(lineSubtotal);
                                        totalServiceFee = totalServiceFee.add(lineServiceFee);
                                        totalVat = totalVat.add(lineVat);
                                        roomCoveredByMaster = roomCoveredByMaster.add(lineTotal);
                                    } else {
                                        roomPersonalDebit = roomPersonalDebit.add(lineTotal);
                                    }
                                }
                            }

                            // 2. Phụ phí phòng (BookingCharge)
                            if (br.getCharges() != null) {
                                for (BookingCharge charge : br.getCharges()) {
                                    String adjKey = "BOOKING_CHARGE:" + charge.getId();
                                    MasterFolioItemAdjustment manualAdj = adjustmentMap.get(adjKey);

                                    RoutingTarget finalTarget = RoutingTarget.SUB_FOLIO;
                                    boolean isAdjusted = false;
                                    String adjReason = null;

                                    if (manualAdj != null) {
                                        finalTarget = manualAdj.getAdjustedTarget();
                                        isAdjusted = true;
                                        adjReason = manualAdj.getAdjustmentReason();
                                    }

                                    if (finalTarget == RoutingTarget.MASTER) {
                                        masterDebits.add(MasterFolioLineItemDto.builder()
                                                .referenceId(charge.getId())
                                                .itemType("BOOKING_CHARGE")
                                                .chargeCategory(ChargeCategory.CUSTOM)
                                                .description(charge.getItemName() != null ? charge.getItemName() : charge.getChargeType().name())
                                                .itemDate(charge.getCreatedAt() != null ? charge.getCreatedAt().toLocalDate() : LocalDate.now())
                                                .roomNumber(roomNumber)
                                                .guestName(guestName)
                                                .quantity(charge.getQuantity())
                                                .unitPrice(charge.getUnitPrice())
                                                .subtotal(charge.getSubtotal())
                                                .serviceFeeRate(charge.getServiceFeeRate())
                                                .serviceFeeAmount(charge.getServiceFeeAmount())
                                                .vatRate(charge.getVatRate())
                                                .vatAmount(charge.getVatAmount())
                                                .totalAmount(charge.getTotalAmount())
                                                .routingTarget(RoutingTarget.MASTER)
                                                .isManuallyAdjusted(isAdjusted)
                                                .adjustmentReason(adjReason)
                                                .build());

                                        totalSubtotal = totalSubtotal.add(charge.getSubtotal());
                                        totalServiceFee = totalServiceFee.add(charge.getServiceFeeAmount());
                                        totalVat = totalVat.add(charge.getVatAmount());
                                        roomCoveredByMaster = roomCoveredByMaster.add(charge.getTotalAmount());
                                    } else {
                                        roomPersonalDebit = roomPersonalDebit.add(charge.getTotalAmount());
                                    }
                                }
                            }

                            subFolios.add(SubFolioSummaryDto.builder()
                                    .bookingId(booking.getId())
                                    .bookingNumber(booking.getBookingNumber())
                                    .bookingRoomId(br.getId())
                                    .roomNumber(roomNumber)
                                    .roomTypeName(detail.getRoomTypeName())
                                    .guestName(guestName)
                                    .roomStatus(br.getStatus() != null ? br.getStatus().name() : "EXPECTED")
                                    .coveredByMasterAmount(roomCoveredByMaster)
                                    .personalDebitAmount(roomPersonalDebit)
                                    .personalPaidAmount(BigDecimal.ZERO)
                                    .personalBalance(roomPersonalDebit)
                                    .build());
                        }
                    }
                }

                // 3. Đơn dịch vụ POS (ServiceOrder)
                List<ServiceOrderResponse> orders = serviceOrderService.getOrdersByBookingId(booking.getId());
                if (orders != null) {
                    for (ServiceOrderResponse order : orders) {
                        if (order.getPaymentMethod() != null && order.getPaymentMethod() != ServiceOrderPaymentMethod.POST_TO_ROOM) {
                            continue;
                        }
                        if (order.getPaymentStatus() == ServiceOrderPaymentStatus.PAID) {
                            continue;
                        }

                        if (order.getDetails() != null) {
                            for (ServiceOrderDetailResponse item : order.getDetails()) {
                                ChargeCategory category = resolveChargeCategory(item.getItemName(), item.getItemType());

                                String adjKey = "SERVICE_ORDER_DETAIL:" + item.getId();
                                MasterFolioItemAdjustment manualAdj = adjustmentMap.get(adjKey);

                                RoutingTarget finalTarget = RoutingTarget.SUB_FOLIO;
                                boolean isAdjusted = false;
                                String adjReason = null;

                                if (manualAdj != null) {
                                    finalTarget = manualAdj.getAdjustedTarget();
                                    isAdjusted = true;
                                    adjReason = manualAdj.getAdjustmentReason();
                                } else {
                                    BillingRoutingRule rule = ruleMap.get(category);
                                    if (rule != null) {
                                        finalTarget = rule.getRouteTarget();
                                    }
                                }

                                LocalDate orderDate = order.getIssuedAt() != null
                                        ? order.getIssuedAt().toLocalDate()
                                        : (order.getCreatedAt() != null ? order.getCreatedAt().toLocalDate() : LocalDate.now());

                                if (finalTarget == RoutingTarget.MASTER) {
                                    masterDebits.add(MasterFolioLineItemDto.builder()
                                            .referenceId(item.getId())
                                            .itemType("SERVICE_ORDER_DETAIL")
                                            .chargeCategory(category)
                                            .description(item.getItemName() != null ? item.getItemName() : "Dịch vụ/Sản phẩm POS")
                                            .itemDate(orderDate)
                                            .roomNumber(order.getRoomNumber())
                                            .guestName("Khách đoàn")
                                            .quantity(item.getQuantity())
                                            .unitPrice(item.getUnitPrice())
                                            .subtotal(item.getSubtotal())
                                            .serviceFeeRate(item.getServiceFeeRate())
                                            .serviceFeeAmount(item.getServiceFeeAmount())
                                            .vatRate(item.getVatRate())
                                            .vatAmount(item.getVatAmount())
                                            .totalAmount(item.getTotalAmount())
                                            .routingTarget(RoutingTarget.MASTER)
                                            .isManuallyAdjusted(isAdjusted)
                                            .adjustmentReason(adjReason)
                                            .build());

                                    totalSubtotal = totalSubtotal.add(item.getSubtotal());
                                    totalServiceFee = totalServiceFee.add(item.getServiceFeeAmount());
                                    totalVat = totalVat.add(item.getVatAmount());
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Khi chua upload Rooming List: hien thi theo gia tri hop dong thoa thuan
            GroupContract contract = masterFolio.getContract();
            totalSubtotal = contract.getSubtotalAmount() != null ? contract.getSubtotalAmount() : BigDecimal.ZERO;
            totalServiceFee = contract.getServiceFeeAmount() != null ? contract.getServiceFeeAmount() : BigDecimal.ZERO;
            totalVat = contract.getVatAmount() != null ? contract.getVatAmount() : BigDecimal.ZERO;
        }

        BigDecimal calculatedTotal = totalSubtotal.add(totalServiceFee).add(totalVat);
        BigDecimal depositCredited = masterFolio.getDepositCredited() != null ? masterFolio.getDepositCredited() : BigDecimal.ZERO;
        BigDecimal totalPaid = masterFolio.getTotalPaid() != null ? masterFolio.getTotalPaid() : depositCredited;
        BigDecimal balance = calculatedTotal.subtract(totalPaid);

        // Cap nhat cache tren MasterFolio entity
        masterFolio.setSubtotalAmount(totalSubtotal);
        masterFolio.setServiceFeeAmount(totalServiceFee);
        masterFolio.setTotalVatAmount(totalVat);
        masterFolio.setTotalAmount(calculatedTotal);
        masterFolio.setBalance(balance);
        masterFolioRepository.save(masterFolio);

        return MasterFolioResponse.builder()
                .id(masterFolio.getId())
                .folioNumber(masterFolio.getFolioNumber())
                .contractId(masterFolio.getContract() != null ? masterFolio.getContract().getId() : null)
                .contractNumber(masterFolio.getContract() != null ? masterFolio.getContract().getContractNumber() : null)
                .hotelId(masterFolio.getHotel() != null ? masterFolio.getHotel().getId() : null)
                .hotelName(masterFolio.getHotel() != null ? masterFolio.getHotel().getName() : null)
                .companyId(masterFolio.getCompany() != null ? masterFolio.getCompany().getId() : null)
                .companyName(masterFolio.getCompany() != null ? masterFolio.getCompany().getName() : null)
                .buyerLegalName(masterFolio.getBuyerLegalName())
                .buyerTaxCode(masterFolio.getBuyerTaxCode())
                .buyerAddress(masterFolio.getBuyerAddress())
                .buyerEmailForInvoice(masterFolio.getBuyerEmailForInvoice())
                .subtotalAmount(totalSubtotal)
                .serviceFeeAmount(totalServiceFee)
                .totalVatAmount(totalVat)
                .totalAmount(calculatedTotal)
                .depositCredited(depositCredited)
                .totalPaid(totalPaid)
                .balance(balance)
                .status(masterFolio.getStatus())
                .closedAt(masterFolio.getClosedAt())
                .createdAt(masterFolio.getCreatedAt())
                .routingRules(ruleResponses)
                .debits(masterDebits)
                .subFolios(subFolios)
                .build();
    }

    private ChargeCategory resolveChargeCategory(String itemName, String itemType) {
        if (itemName == null) {
            return ChargeCategory.CUSTOM;
        }
        String lower = itemName.toLowerCase();
        if (lower.contains("minibar") || lower.contains("nước suối") || lower.contains("snack")) {
            return ChargeCategory.MINIBAR;
        }
        if (lower.contains("giặt") || lower.contains("laundry")) {
            return ChargeCategory.LAUNDRY;
        }
        if (lower.contains("ăn sáng") || lower.contains("breakfast") || lower.contains("buffet")) {
            return ChargeCategory.BREAKFAST;
        }
        if (lower.contains("tiệc") || lower.contains("hội nghị") || lower.contains("banquet") || lower.contains("gala")) {
            return ChargeCategory.BANQUET;
        }
        return ChargeCategory.CUSTOM;
    }

    private BillingRoutingRuleResponse mapToRuleResponse(BillingRoutingRule rule) {
        return BillingRoutingRuleResponse.builder()
                .id(rule.getId())
                .masterFolioId(rule.getMasterFolio() != null ? rule.getMasterFolio().getId() : null)
                .chargeCategory(rule.getChargeCategory())
                .routeTarget(rule.getRouteTarget())
                .maxCoveredAmount(rule.getMaxCoveredAmount())
                .isActive(rule.getIsActive())
                .createdAt(rule.getCreatedAt())
                .build();
    }

    private String generateFolioNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
        return "MF-" + datePart + "-" + randomSuffix;
    }

    private String staffUsernameOrSystem(String defaultName) {
        return defaultName;
    }
}
