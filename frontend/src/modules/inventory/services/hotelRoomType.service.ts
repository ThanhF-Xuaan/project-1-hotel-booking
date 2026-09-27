import { apiClient } from '../../../core/api/client'
import type { PageResponse } from '../../../core/api/types'
import type {
  HotelRoomTypeDto,
  HotelRoomTypeCreateRequest,
  HotelRoomTypeUpdateRequest,
  HotelRoomTypeSearchDto,
} from '../types/inventory.types'

const BASE_URL = '/api/v1/inventory/hotel-room-types'

export const hotelRoomTypeService = {
  filter: (params: HotelRoomTypeSearchDto) =>
    apiClient.post<PageResponse<HotelRoomTypeDto>>(`${BASE_URL}/filter`, params),

  getById: (id: number) => apiClient.get<HotelRoomTypeDto>(`${BASE_URL}/${id}`),

  create: (data: HotelRoomTypeCreateRequest) => apiClient.post<HotelRoomTypeDto>(BASE_URL, data),

  update: (id: number, data: HotelRoomTypeUpdateRequest) =>
    apiClient.put<HotelRoomTypeDto>(`${BASE_URL}/${id}`, data),

  deleteBatch: (ids: number[]) => apiClient.delete<void>(`${BASE_URL}/delete`, { data: ids }),
}

export default hotelRoomTypeService
