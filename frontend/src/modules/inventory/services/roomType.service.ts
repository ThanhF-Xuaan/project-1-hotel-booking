import { apiClient } from '../../../core/api/client'
import type { PageResponse } from '../../../core/api/types'
import type {
  RoomTypeDto,
  RoomTypeCreateRequest,
  RoomTypeUpdateRequest,
  RoomTypeSearchDto,
} from '../types/inventory.types'

const BASE_URL = '/api/v1/inventory/room-types'

export const roomTypeService = {
  filter: (params: RoomTypeSearchDto) =>
    apiClient.post<PageResponse<RoomTypeDto>>(`${BASE_URL}/filter`, params),

  getById: (id: number) => apiClient.get<RoomTypeDto>(`${BASE_URL}/${id}`),

  create: (data: RoomTypeCreateRequest) => apiClient.post<RoomTypeDto>(BASE_URL, data),

  update: (id: number, data: RoomTypeUpdateRequest) =>
    apiClient.put<RoomTypeDto>(`${BASE_URL}/${id}`, data),

  deleteBatch: (ids: number[]) => apiClient.delete<void>(`${BASE_URL}/delete`, { data: ids }),
}

export default roomTypeService
