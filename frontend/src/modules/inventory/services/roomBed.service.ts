import { apiClient } from '../../../core/api/client'
import type { PageResponse } from '../../../core/api/types'
import type {
  RoomBedDto,
  RoomBedCreateRequest,
  RoomBedUpdateRequest,
  RoomBedSearchDto,
} from '../types/inventory.types'

const BASE_URL = '/api/v1/inventory/room-beds'

export const roomBedService = {
  filter: (params: RoomBedSearchDto) =>
    apiClient.post<PageResponse<RoomBedDto>>(`${BASE_URL}/filter`, params),

  getById: (id: number) => apiClient.get<RoomBedDto>(`${BASE_URL}/${id}`),

  create: (data: RoomBedCreateRequest) => apiClient.post<RoomBedDto>(BASE_URL, data),

  update: (id: number, data: RoomBedUpdateRequest) =>
    apiClient.put<RoomBedDto>(`${BASE_URL}/${id}`, data),

  deleteBatch: (ids: number[]) => apiClient.delete<void>(`${BASE_URL}/delete`, { data: ids }),
}

export default roomBedService

