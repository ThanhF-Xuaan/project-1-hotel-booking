import { apiClient } from '../../../core/api/client'
import type { PageResponse } from '../../../core/api/types'
import type {
  VatRuleDto,
  VatRuleCreateRequest,
  VatRuleUpdateRequest,
  VatRuleSearchDto,
} from '../types/pricing.types'

const BASE_URL = '/api/v1/pricing/vat-rules'

export const vatRuleService = {
  filter: (params: VatRuleSearchDto) =>
    apiClient.post<PageResponse<VatRuleDto>>(`${BASE_URL}/filter`, params),

  getById: (id: number) => apiClient.get<VatRuleDto>(`${BASE_URL}/${id}`),

  create: (data: VatRuleCreateRequest) =>
    apiClient.post<VatRuleDto>(BASE_URL, data),

  update: (id: number, data: VatRuleUpdateRequest) =>
    apiClient.put<VatRuleDto>(`${BASE_URL}/${id}`, data),

  deleteBatch: (ids: number[]) =>
    apiClient.delete<void>(`${BASE_URL}/delete`, { data: ids }),
}

export default vatRuleService
