import { apiClient } from '../../../core/api/client'
import type { PageResponse } from '../../../core/api/types'
import type {
  DiscountRuleDto,
  DiscountRuleCreateRequest,
  DiscountRuleUpdateRequest,
  DiscountRuleSearchDto,
} from '../types/pricing.types'

const BASE_URL = '/api/v1/pricing/discount-rules'

export const discountRuleService = {
  filter: (params: DiscountRuleSearchDto) =>
    apiClient.post<PageResponse<DiscountRuleDto>>(`${BASE_URL}/filter`, params),

  getById: (id: number) => apiClient.get<DiscountRuleDto>(`${BASE_URL}/${id}`),

  create: (data: DiscountRuleCreateRequest) =>
    apiClient.post<DiscountRuleDto>(BASE_URL, data),

  update: (id: number, data: DiscountRuleUpdateRequest) =>
    apiClient.put<DiscountRuleDto>(`${BASE_URL}/${id}`, data),

  deleteBatch: (ids: number[]) =>
    apiClient.delete<void>(`${BASE_URL}/delete`, { data: ids }),
}

export default discountRuleService
