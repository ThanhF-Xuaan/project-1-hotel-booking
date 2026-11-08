import { apiClient } from '../../../core/api/client'
import type { PageResponse } from '../../../core/api/types'
import type {
  RoomFeatureDto,
  RoomFeatureCreateRequest,
  RoomFeatureUpdateRequest,
  RoomFeatureSearchDto,
} from '../types/inventory.types'

const BASE_URL = '/api/v1/inventory/room-features'

export const roomFeatureService = {
  filter: (params: RoomFeatureSearchDto) =>
    apiClient.post<PageResponse<RoomFeatureDto>>(`${BASE_URL}/filter`, params),

  getById: (id: number) => apiClient.get<RoomFeatureDto>(`${BASE_URL}/${id}`),

  create: (data: RoomFeatureCreateRequest) => apiClient.post<RoomFeatureDto>(BASE_URL, data),

  update: (id: number, data: RoomFeatureUpdateRequest) =>
    apiClient.put<RoomFeatureDto>(`${BASE_URL}/${id}`, data),

  deleteBatch: (ids: number[]) => apiClient.delete<void>(`${BASE_URL}/delete`, { data: ids }),
}

export default roomFeatureService

