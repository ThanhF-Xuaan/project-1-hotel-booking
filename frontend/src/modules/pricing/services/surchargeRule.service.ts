import { apiClient } from '../../../core/api/client'
import type { PageResponse } from '../../../core/api/types'
import type {
  SurchargeRuleDto,
  SurchargeRuleCreateRequest,
  SurchargeRuleUpdateRequest,
  SurchargeRuleSearchDto,
} from '../types/pricing.types'

const BASE_URL = '/api/v1/pricing/surcharge-rules'

export const surchargeRuleService = {
  filter: (params: SurchargeRuleSearchDto) =>
    apiClient.post<PageResponse<SurchargeRuleDto>>(`${BASE_URL}/filter`, params),

  getById: (id: number) => apiClient.get<SurchargeRuleDto>(`${BASE_URL}/${id}`),

  create: (data: SurchargeRuleCreateRequest) =>
    apiClient.post<SurchargeRuleDto>(BASE_URL, data),

  update: (id: number, data: SurchargeRuleUpdateRequest) =>
    apiClient.put<SurchargeRuleDto>(`${BASE_URL}/${id}`, data),

  deleteBatch: (ids: number[]) =>
    apiClient.delete<void>(`${BASE_URL}/delete`, { data: ids }),
}

export default surchargeRuleService
