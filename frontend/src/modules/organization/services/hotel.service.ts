import { apiClient } from '../../../core/api/client'
import type { PageResponse } from '../../../core/api/types'
import type {
  HotelDto,
  HotelCreateRequest,
  HotelUpdateRequest,
  HotelSearchDto,
} from '../types/organization.types'

const BASE_URL = '/api/v1/hotels'

export const hotelService = {
  filter: (params: HotelSearchDto) =>
    apiClient.post<PageResponse<HotelDto>>(`${BASE_URL}/filter`, params),

  getById: (id: number) => apiClient.get<HotelDto>(`${BASE_URL}/${id}`),

  create: (data: HotelCreateRequest) => apiClient.post<HotelDto>(BASE_URL, data),

  update: (id: number, data: HotelUpdateRequest) =>
    apiClient.put<HotelDto>(`${BASE_URL}/${id}`, data),

  deleteBatch: (ids: number[]) => apiClient.delete<void>(`${BASE_URL}/delete`, { data: ids }),
}

export default hotelService
