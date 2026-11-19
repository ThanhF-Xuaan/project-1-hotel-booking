import { apiClient } from '../../../core/api/client'
import type { PageResponse, ApiResponse } from '../../../core/api/types'
import type {
  RoomInstanceDto,
  RoomInstanceCreateRequest,
  RoomInstanceUpdateRequest,
  RoomInstanceSearchDto,
  RoomAssignmentCandidateDto,
  RoomAssignmentCandidateSearchDto,
} from '../types/inventory.types'

const BASE_URL = '/api/v1/inventory/rooms'

export const roomInstanceService = {
  filter: (params: RoomInstanceSearchDto) =>
    apiClient.post<PageResponse<RoomInstanceDto>>(`${BASE_URL}/filter`, params),

  getAvailableRoomsForAssignment: async (
    params: RoomAssignmentCandidateSearchDto
  ): Promise<ApiResponse<RoomAssignmentCandidateDto[]>> => {
    const res = await apiClient.post<RoomAssignmentCandidateDto[]>(
      `${BASE_URL}/available-for-assignment`,
      params
    )
    if (res && res.result) {
      res.result = res.result.map((item) => ({
        ...item,
        isAvailable: item.isAvailable ?? item.available ?? false,
        isSameRoomType: item.isSameRoomType ?? item.sameRoomType ?? false,
      }))
    }
    return res
  },

  getById: (id: number) => apiClient.get<RoomInstanceDto>(`${BASE_URL}/${id}`),

  create: (data: RoomInstanceCreateRequest) => apiClient.post<RoomInstanceDto>(BASE_URL, data),

  update: (id: number, data: RoomInstanceUpdateRequest) =>
    apiClient.put<RoomInstanceDto>(`${BASE_URL}/${id}`, data),

  updateStatus: (id: number, status: string) =>
    apiClient.patch<RoomInstanceDto>(`${BASE_URL}/${id}/status?status=${status}`),

  deleteBatch: (ids: number[]) => apiClient.delete<void>(`${BASE_URL}/delete`, { data: ids }),
}

export default roomInstanceService
