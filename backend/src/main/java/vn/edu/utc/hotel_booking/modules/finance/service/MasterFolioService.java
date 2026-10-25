package vn.edu.utc.hotel_booking.modules.finance.service;

import vn.edu.utc.hotel_booking.modules.booking.entity.GroupContract;
import vn.edu.utc.hotel_booking.modules.finance.entity.MasterFolio;

import java.math.BigDecimal;
import java.util.Optional;

public interface MasterFolioService {

    /**
     * Tự động khởi tạo MasterFolio và các BillingRoutingRules mặc định khi hợp đồng đoàn được xác nhận cọc 50%.
     *
     * @param contract Hợp đồng đoàn đã xác nhận cọc
     * @param depositAmount Số tiền cọc đã thanh toán
     * @return MasterFolio đã lưu
     */
    MasterFolio createMasterFolioForContract(GroupContract contract, BigDecimal depositAmount);

    Optional<MasterFolio> getByContractId(Long contractId);
}
