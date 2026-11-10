import { apiClient } from '../../../core/api/client'
import type {
  PricingRuleTypeDto,
  PricingRuleTypeCreateRequest,
  PricingRuleTypeUpdateRequest,
} from '../types/pricing.types'

const BASE_URL = '/api/v1/pricing/rule-types'

export const pricingRuleTypeService = {
  getAll: () => apiClient.get<PricingRuleTypeDto[]>(BASE_URL),

  getActive: () => apiClient.get<PricingRuleTypeDto[]>(`${BASE_URL}/active`),

  getByCode: (code: string) => apiClient.get<PricingRuleTypeDto>(`${BASE_URL}/${code}`),

  create: (data: PricingRuleTypeCreateRequest) =>
    apiClient.post<PricingRuleTypeDto>(BASE_URL, data),

  update: (code: string, data: PricingRuleTypeUpdateRequest) =>
    apiClient.put<PricingRuleTypeDto>(`${BASE_URL}/${code}`, data),

  delete: (code: string) => apiClient.delete<void>(`${BASE_URL}/${code}`),
}

export default pricingRuleTypeService
