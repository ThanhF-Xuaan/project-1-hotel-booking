import { apiClient } from '../../../core/api/client';
import type {
  MasterFolioResponse,
  BillingRoutingRuleResponse,
  BillingRoutingRuleRequest,
  MasterFolioAdjustmentRequest,
  MasterFolioStatusUpdateRequest,
} from '../types/masterFolio.types';

const BASE_URL = '/api/v1/master-folios';

export const masterFolioService = {
  /**
   * Lấy Sổ cái Master Folio theo ID hợp đồng đoàn
   */
  getByContractId: (contractId: number) =>
    apiClient.get<MasterFolioResponse>(`${BASE_URL}/contract/${contractId}`),

  /**
   * Lấy Sổ cái Master Folio theo ID
   */
  getById: (id: number) =>
    apiClient.get<MasterFolioResponse>(`${BASE_URL}/${id}`),

  /**
   * Tra cứu Sổ cái Master Folio theo mã Folio
   */
  getByNumber: (folioNumber: string) =>
    apiClient.get<MasterFolioResponse>(`${BASE_URL}/number/${folioNumber}`),

  /**
   * Lấy danh sách quy tắc định tuyến chi phí của Master Folio
   */
  getRoutingRules: (id: number) =>
    apiClient.get<BillingRoutingRuleResponse[]>(`${BASE_URL}/${id}/routing-rules`),

  /**
   * Thêm mới quy tắc định tuyến chi phí
   */
  addRoutingRule: (id: number, request: BillingRoutingRuleRequest) =>
    apiClient.post<BillingRoutingRuleResponse>(`${BASE_URL}/${id}/routing-rules`, request),

  /**
   * Cập nhật quy tắc định tuyến chi phí
   */
  updateRoutingRule: (id: number, ruleId: number, request: BillingRoutingRuleRequest) =>
    apiClient.put<BillingRoutingRuleResponse>(`${BASE_URL}/${id}/routing-rules/${ruleId}`, request),

  /**
   * Vô hiệu hóa quy tắc định tuyến chi phí
   */
  deleteRoutingRule: (id: number, ruleId: number) =>
    apiClient.delete<void>(`${BASE_URL}/${id}/routing-rules/${ruleId}`),

  /**
   * Điều chuyển thủ công khoản mục chi phí giữa Master và Sub-Folio
   */
  adjustRouting: (id: number, request: MasterFolioAdjustmentRequest) =>
    apiClient.post<MasterFolioResponse>(`${BASE_URL}/${id}/adjust-routing`, request),

  /**
   * Cập nhật trạng thái Master Folio (Khóa sổ quyết toán / Hoàn tất)
   */
  updateStatus: (id: number, request: MasterFolioStatusUpdateRequest) =>
    apiClient.patch<MasterFolioResponse>(`${BASE_URL}/${id}/status`, request),
};

export default masterFolioService;

