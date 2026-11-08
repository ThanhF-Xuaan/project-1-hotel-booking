import { apiClient } from '../../../core/api/client'
import type { PageResponse } from '../../../core/api/types'
import type {
  HotelAgePolicyDto,
  HotelAgePolicyCreateRequest,
  HotelAgePolicyUpdateRequest,
  HotelAgePolicySearchDto,
} from '../types/pricing.types'

const BASE_URL = '/api/v1/pricing/age-policies'

export const agePolicyService = {
  filter: (params: HotelAgePolicySearchDto) =>
    apiClient.post<PageResponse<HotelAgePolicyDto>>(`${BASE_URL}/filter`, params),

  getById: (id: number) => apiClient.get<HotelAgePolicyDto>(`${BASE_URL}/${id}`),

  getEffective: (hotelId?: number) =>
    apiClient.get<HotelAgePolicyDto[]>(`${BASE_URL}/effective`, {
      params: hotelId ? { hotelId } : {},
    }),

  create: (data: HotelAgePolicyCreateRequest) =>
    apiClient.post<HotelAgePolicyDto>(BASE_URL, data),

  update: (id: number, data: HotelAgePolicyUpdateRequest) =>
    apiClient.put<HotelAgePolicyDto>(`${BASE_URL}/${id}`, data),

  deleteBatch: (ids: number[]) =>
    apiClient.delete<void>(`${BASE_URL}/delete`, { data: ids }),
}

export default agePolicyService
