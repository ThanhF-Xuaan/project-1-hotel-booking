import { apiClient } from '../../../core/api/client'
import type { PageResponse } from '../../../core/api/types'
import type { HotelImageDto } from '../../organization/types/organization.types'
import type {
  HotelRoomTypeDto,
  HotelRoomTypeCreateRequest,
  HotelRoomTypeUpdateRequest,
  HotelRoomTypeSearchDto,
  HotelRoomTypeBedItemRequest,
} from '../types/inventory.types'

const BASE_URL = '/api/v1/inventory/hotel-room-types'
const PUBLIC_BASE_URL = '/api/v1/public/hotel-room-types'

export const hotelRoomTypeService = {
  filter: (params: HotelRoomTypeSearchDto) =>
    apiClient.post<PageResponse<HotelRoomTypeDto>>(`${BASE_URL}/filter`, params),

  getById: (id: number) => apiClient.get<HotelRoomTypeDto>(`${BASE_URL}/${id}`),

  create: (data: HotelRoomTypeCreateRequest) => apiClient.post<HotelRoomTypeDto>(BASE_URL, data),

  update: (id: number, data: HotelRoomTypeUpdateRequest) =>
    apiClient.put<HotelRoomTypeDto>(`${BASE_URL}/${id}`, data),

  updateBeds: (id: number, beds: HotelRoomTypeBedItemRequest[]) =>
    apiClient.put<HotelRoomTypeDto>(`${BASE_URL}/${id}/beds`, beds),

  updateFeatures: (id: number, featureIds: number[]) =>
    apiClient.put<HotelRoomTypeDto>(`${BASE_URL}/${id}/features`, featureIds),

  deleteBatch: (ids: number[]) => apiClient.delete<void>(`${BASE_URL}/delete`, { data: ids }),

  getImages: (hotelRoomTypeId: number) =>
    apiClient.get<HotelImageDto[]>(`${BASE_URL}/${hotelRoomTypeId}/images`),

  uploadImages: (hotelRoomTypeId: number, files: File[]) => {
    const formData = new FormData()
    files.forEach((file) => formData.append('files', file))
    return apiClient.post<HotelImageDto[]>(`${BASE_URL}/${hotelRoomTypeId}/images`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  setPrimaryImage: (hotelRoomTypeId: number, imageId: number) =>
    apiClient.patch<void>(`${BASE_URL}/${hotelRoomTypeId}/images/${imageId}/primary`),

  deleteImage: (hotelRoomTypeId: number, imageId: number) =>
    apiClient.delete<void>(`${BASE_URL}/${hotelRoomTypeId}/images/${imageId}`),

  getPublicImages: (hotelRoomTypeId: number) =>
    apiClient.get<HotelImageDto[]>(`${PUBLIC_BASE_URL}/${hotelRoomTypeId}/images`),
}

export default hotelRoomTypeService
