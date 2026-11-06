import { apiClient } from '../../../core/api/client';
import type {
  MasterFolioResponse,
  BillingRoutingRuleResponse,
  GroupCheckoutRequest,
  GroupCheckoutResponse,
  MasterFolioAdjustmentRequest,
} from '../types/masterFolio.types';

const BASE_URL = '/api/v1/master-folios';

export const masterFolioService = {
  getMasterFolioById: (id: number) =>
    apiClient.get<MasterFolioResponse>(`${BASE_URL}/${id}`),

  getMasterFolioByContractId: (contractId: number) =>
    apiClient.get<MasterFolioResponse>(`${BASE_URL}/contract/${contractId}`),

  getMasterFolioByNumber: (folioNumber: string) =>
    apiClient.get<MasterFolioResponse>(`${BASE_URL}/number/${folioNumber}`),

  getRoutingRules: (masterFolioId: number) =>
    apiClient.get<BillingRoutingRuleResponse[]>(`${BASE_URL}/${masterFolioId}/routing-rules`),

  adjustItemRouting: (masterFolioId: number, data: MasterFolioAdjustmentRequest) =>
    apiClient.post<MasterFolioResponse>(`${BASE_URL}/${masterFolioId}/adjust-routing`, data),

  checkoutGroup: (masterFolioId: number, data: GroupCheckoutRequest) =>
    apiClient.post<GroupCheckoutResponse>(`${BASE_URL}/${masterFolioId}/group-checkout`, data),
};

