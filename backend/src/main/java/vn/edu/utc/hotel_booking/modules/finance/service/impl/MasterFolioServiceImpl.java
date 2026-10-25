package vn.edu.utc.hotel_booking.modules.finance.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupContract;
import vn.edu.utc.hotel_booking.modules.finance.entity.*;
import vn.edu.utc.hotel_booking.modules.finance.repository.MasterFolioRepository;
import vn.edu.utc.hotel_booking.modules.finance.service.MasterFolioService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MasterFolioServiceImpl implements MasterFolioService {

    MasterFolioRepository masterFolioRepository;

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
        return masterFolioRepository.save(masterFolio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MasterFolio> getByContractId(Long contractId) {
        return masterFolioRepository.findByContractId(contractId);
    }

    private String generateFolioNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
        return "MF-" + datePart + "-" + randomSuffix;
    }
}
