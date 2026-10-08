import { apiClient } from '../../../core/api/client'
import type { PageResponse } from '../../../core/api/types'
import type {
  PriceCalculationRequest,
  PriceBreakdownDto,
  PricingRuleDto,
  PricingRuleCreateRequest,
  PricingRuleUpdateRequest,
  PricingRuleSearchDto,
} from '../types/pricing.types'

const BASE_URL = '/api/v1/pricing'

export const pricingService = {
  calculate: (request: PriceCalculationRequest) =>
    apiClient.post<PriceBreakdownDto>(`${BASE_URL}/calculate`, {
      adults: 1,
      children: 0,
      extraBeds: 0,
      ...request,
    }),

  filterRules: (params: PricingRuleSearchDto) =>
    apiClient.post<PageResponse<PricingRuleDto>>(`${BASE_URL}/rules/filter`, params),

  getRuleById: (id: number) => apiClient.get<PricingRuleDto>(`${BASE_URL}/rules/${id}`),

  createRule: (data: PricingRuleCreateRequest) =>
    apiClient.post<PricingRuleDto>(`${BASE_URL}/rules`, data),

  updateRule: (id: number, data: PricingRuleUpdateRequest) =>
    apiClient.put<PricingRuleDto>(`${BASE_URL}/rules/${id}`, data),

  deleteRulesBatch: (ids: number[]) =>
    apiClient.delete<void>(`${BASE_URL}/rules/delete`, { data: ids }),
}

export default pricingService
