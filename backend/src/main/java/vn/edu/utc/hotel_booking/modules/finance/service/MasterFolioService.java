package vn.edu.utc.hotel_booking.modules.finance.service;

import vn.edu.utc.hotel_booking.modules.booking.entity.GroupContract;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.BillingRoutingRuleRequest;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.MasterFolioAdjustmentRequest;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.MasterFolioStatusUpdateRequest;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.BillingRoutingRuleResponse;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.MasterFolioResponse;
import vn.edu.utc.hotel_booking.modules.finance.entity.MasterFolio;

import java.math.BigDecimal;
import java.util.List;
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

    /**
     * Lấy thực thể MasterFolio theo ID hợp đồng đoàn
     *
     * @param contractId ID hợp đồng
     * @return Optional MasterFolio
     */
    Optional<MasterFolio> getByContractId(Long contractId);

    /**
     * Lấy Sổ cái Master Folio tổng hợp theo ID Master Folio
     *
     * @param masterFolioId ID Master Folio
     * @return MasterFolioResponse thời gian thực
     */
    MasterFolioResponse getMasterFolioById(Long masterFolioId);

    /**
     * Lấy Sổ cái Master Folio tổng hợp theo ID hợp đồng đoàn
     *
     * @param contractId ID hợp đồng
     * @return MasterFolioResponse thời gian thực
     */
    MasterFolioResponse getMasterFolioByContractId(Long contractId);

    /**
     * Tra cứu Sổ cái Master Folio theo mã Folio Number
     *
     * @param folioNumber Mã folio (MF-YYYYMMDD-XXXX)
     * @return MasterFolioResponse thời gian thực
     */
    MasterFolioResponse getMasterFolioByNumber(String folioNumber);

    /**
     * Lấy danh sách quy tắc định tuyến chi phí của Master Folio
     *
     * @param masterFolioId ID Master Folio
     * @return Danh sách quy tắc định tuyến
     */
    List<BillingRoutingRuleResponse> getRoutingRules(Long masterFolioId);

    /**
     * Thêm mới quy tắc định tuyến chi phí cho Master Folio
     *
     * @param masterFolioId ID Master Folio
     * @param request Thông tin quy tắc
     * @return Quy tắc đã tạo
     */
    BillingRoutingRuleResponse addRoutingRule(Long masterFolioId, BillingRoutingRuleRequest request);

    /**
     * Cập nhật quy tắc định tuyến chi phí
     *
     * @param masterFolioId ID Master Folio
     * @param ruleId ID quy tắc
     * @param request Thông tin cập nhật
     * @return Quy tắc đã cập nhật
     */
    BillingRoutingRuleResponse updateRoutingRule(Long masterFolioId, Long ruleId, BillingRoutingRuleRequest request);

    /**
     * Xóa hoặc vô hiệu hóa quy tắc định tuyến
     *
     * @param masterFolioId ID Master Folio
     * @param ruleId ID quy tắc
     */
    void deleteRoutingRule(Long masterFolioId, Long ruleId);

    /**
     * Điều chuyển thủ công chi phí giữa Sub-Folio và Master Folio (Ngoại lệ 2a SRS UC-FIN-04)
     * Có lưu vết kiểm toán bất biến theo Luật Kế toán 2015 & Luật Giao dịch điện tử 2023.
     *
     * @param masterFolioId ID Master Folio
     * @param request Dữ liệu điều chuyển
     * @param staffUsername Tên nhân viên thực hiện
     * @return MasterFolioResponse sau khi cập nhật
     */
    MasterFolioResponse adjustItemRouting(Long masterFolioId, MasterFolioAdjustmentRequest request, String staffUsername);

    /**
     * Cập nhật trạng thái Master Folio (khóa quyết toán, đóng sổ...)
     *
     * @param masterFolioId ID Master Folio
     * @param request Trạng thái mới
     * @return MasterFolioResponse sau cập nhật
     */
    MasterFolioResponse updateMasterFolioStatus(Long masterFolioId, MasterFolioStatusUpdateRequest request);
}
