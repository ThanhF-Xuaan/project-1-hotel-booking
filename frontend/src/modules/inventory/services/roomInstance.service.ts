import { apiClient } from '../../../core/api/client'
import type { PageResponse } from '../../../core/api/types'
import type {
  RoomInstanceDto,
  RoomInstanceCreateRequest,
  RoomInstanceUpdateRequest,
  RoomInstanceSearchDto,
} from '../types/inventory.types'

const BASE_URL = '/api/v1/inventory/rooms'

export const roomInstanceService = {
  filter: (params: RoomInstanceSearchDto) =>
    apiClient.post<PageResponse<RoomInstanceDto>>(`${BASE_URL}/filter`, params),

  getById: (id: number) => apiClient.get<RoomInstanceDto>(`${BASE_URL}/${id}`),

  create: (data: RoomInstanceCreateRequest) => apiClient.post<RoomInstanceDto>(BASE_URL, data),

  update: (id: number, data: RoomInstanceUpdateRequest) =>
    apiClient.put<RoomInstanceDto>(`${BASE_URL}/${id}`, data),

  updateStatus: (id: number, status: string) =>
    apiClient.patch<RoomInstanceDto>(`${BASE_URL}/${id}/status?status=${status}`),

  deleteBatch: (ids: number[]) => apiClient.delete<void>(`${BASE_URL}/delete`, { data: ids }),
}

export default roomInstanceService
