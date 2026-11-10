import { apiClient } from '../../../core/api/client'
import type {
  DiscountRuleTypeDto,
  DiscountRuleTypeCreateRequest,
  DiscountRuleTypeUpdateRequest,
} from '../types/pricing.types'

const BASE_URL = '/api/v1/pricing/discount-rule-types'

export const discountRuleTypeService = {
  getAll: () => apiClient.get<DiscountRuleTypeDto[]>(BASE_URL),

  getActive: () => apiClient.get<DiscountRuleTypeDto[]>(`${BASE_URL}/active`),

  getByCode: (code: string) => apiClient.get<DiscountRuleTypeDto>(`${BASE_URL}/${code}`),

  create: (data: DiscountRuleTypeCreateRequest) =>
    apiClient.post<DiscountRuleTypeDto>(BASE_URL, data),

  update: (code: string, data: DiscountRuleTypeUpdateRequest) =>
    apiClient.put<DiscountRuleTypeDto>(`${BASE_URL}/${code}`, data),

  delete: (code: string) => apiClient.delete<void>(`${BASE_URL}/${code}`),
}

export default discountRuleTypeService
