import { apiClient } from '../../../core/api/client'
import type { PageResponse } from '../../../core/api/types'
import type {
  HotelDto,
  HotelCreateRequest,
  HotelUpdateRequest,
  HotelSearchDto,
  HotelImageDto,
} from '../types/organization.types'

const BASE_URL = '/api/v1/hotels'
const PUBLIC_BASE_URL = '/api/v1/public/hotels'

export const hotelService = {
  filter: (params: HotelSearchDto) =>
    apiClient.post<PageResponse<HotelDto>>(`${BASE_URL}/filter`, params),

  getById: (id: number) => apiClient.get<HotelDto>(`${BASE_URL}/${id}`),

  create: (data: HotelCreateRequest) => apiClient.post<HotelDto>(BASE_URL, data),

  update: (id: number, data: HotelUpdateRequest) =>
    apiClient.put<HotelDto>(`${BASE_URL}/${id}`, data),

  deleteBatch: (ids: number[]) => apiClient.delete<void>(`${BASE_URL}/delete`, { data: ids }),

  getHotelImages: (hotelId: number) =>
    apiClient.get<HotelImageDto[]>(`${BASE_URL}/${hotelId}/images`),

  uploadHotelImages: (hotelId: number, files: File[]) => {
    const formData = new FormData()
    files.forEach((file) => formData.append('files', file))
    return apiClient.post<HotelImageDto[]>(`${BASE_URL}/${hotelId}/images`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  setPrimaryImage: (hotelId: number, imageId: number) =>
    apiClient.patch<void>(`${BASE_URL}/${hotelId}/images/${imageId}/primary`),

  deleteHotelImage: (hotelId: number, imageId: number) =>
    apiClient.delete<void>(`${BASE_URL}/${hotelId}/images/${imageId}`),

  getPublicHotelImages: (hotelId: number) =>
    apiClient.get<HotelImageDto[]>(`${PUBLIC_BASE_URL}/${hotelId}/images`),
}

export default hotelService
