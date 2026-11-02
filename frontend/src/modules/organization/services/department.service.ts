import { apiClient } from '../../../core/api/client'
import type { ApiResponse, PageResponse } from '../../../core/api/types'
import type {
  DepartmentDto,
  DepartmentSearchDto,
  DepartmentCreateRequest,
  DepartmentUpdateRequest,
} from '../types/department.types'

export const departmentService = {
  filter: async (searchDto: DepartmentSearchDto): Promise<ApiResponse<PageResponse<DepartmentDto>>> => {
    return apiClient.post<PageResponse<DepartmentDto>>('/api/v1/departments/filter', searchDto)
  },

  getAllActive: async (): Promise<ApiResponse<DepartmentDto[]>> => {
    return apiClient.get<DepartmentDto[]>('/api/v1/departments/active')
  },

  getById: async (id: number): Promise<ApiResponse<DepartmentDto>> => {
    return apiClient.get<DepartmentDto>(`/api/v1/departments/${id}`)
  },

  create: async (request: DepartmentCreateRequest): Promise<ApiResponse<DepartmentDto>> => {
    return apiClient.post<DepartmentDto>('/api/v1/departments', request)
  },

  update: async (id: number, request: DepartmentUpdateRequest): Promise<ApiResponse<DepartmentDto>> => {
    return apiClient.put<DepartmentDto>(`/api/v1/departments/${id}`, request)
  },

  deleteBatch: async (ids: number[]): Promise<ApiResponse<void>> => {
    return apiClient.delete<void>('/api/v1/departments/delete', { data: ids })
  },
}

export default departmentService
