import { apiClient } from '../../../core/api/client'
import type { ApiResponse, PageResponse } from '../../../core/api/types'
import type {
  StaffDto,
  StaffSearchDto,
  StaffCreateRequest,
  StaffUpdateRequest,
  StaffStatusUpdateRequest,
  RoleDto,
  RoleSearchDto,
  RoleCreateRequest,
  RoleUpdateRequest,
  PermissionDto,
  GuestDto,
  GuestSearchDto,
  GuestCreateRequest,
  GuestUpdateRequest,
  GuestStayHistoryDto,
  CompanyDto,
  CompanySearchDto,
  CompanyCreateRequest,
  CompanyUpdateRequest,
  AuditLogDto,
  AuditLogSearchDto,
} from '../types/identity.types'

export const staffService = {
  filter: async (dto: StaffSearchDto): Promise<ApiResponse<PageResponse<StaffDto>>> => {
    return apiClient.post<PageResponse<StaffDto>>('/api/v1/staffs/filter', dto)
  },
  getById: async (id: number): Promise<ApiResponse<StaffDto>> => {
    return apiClient.get<StaffDto>(`/api/v1/staffs/${id}`)
  },
  create: async (req: StaffCreateRequest): Promise<ApiResponse<StaffDto>> => {
    return apiClient.post<StaffDto>('/api/v1/staffs', req)
  },
  update: async (id: number, req: StaffUpdateRequest): Promise<ApiResponse<StaffDto>> => {
    return apiClient.put<StaffDto>(`/api/v1/staffs/${id}`, req)
  },
  updateStatus: async (id: number, req: StaffStatusUpdateRequest): Promise<ApiResponse<StaffDto>> => {
    return apiClient.patch<StaffDto>(`/api/v1/staffs/${id}/status`, req)
  },
  deleteBatch: async (ids: number[]): Promise<ApiResponse<void>> => {
    return apiClient.delete<void>('/api/v1/staffs/delete', { data: ids })
  },
}

export const roleService = {
  filter: async (dto: RoleSearchDto): Promise<ApiResponse<PageResponse<RoleDto>>> => {
    return apiClient.post<PageResponse<RoleDto>>('/api/v1/roles/filter', dto)
  },
  getAllPermissions: async (): Promise<ApiResponse<PermissionDto[]>> => {
    return apiClient.get<PermissionDto[]>('/api/v1/roles/permissions')
  },
  getById: async (id: number): Promise<ApiResponse<RoleDto>> => {
    return apiClient.get<RoleDto>(`/api/v1/roles/${id}`)
  },
  create: async (req: RoleCreateRequest): Promise<ApiResponse<RoleDto>> => {
    return apiClient.post<RoleDto>('/api/v1/roles', req)
  },
  update: async (id: number, req: RoleUpdateRequest): Promise<ApiResponse<RoleDto>> => {
    return apiClient.put<RoleDto>(`/api/v1/roles/${id}`, req)
  },
  deleteBatch: async (ids: number[]): Promise<ApiResponse<void>> => {
    return apiClient.delete<void>('/api/v1/roles/delete', { data: ids })
  },
}

export const guestService = {
  filter: async (dto: GuestSearchDto): Promise<ApiResponse<PageResponse<GuestDto>>> => {
    return apiClient.post<PageResponse<GuestDto>>('/api/v1/guests/filter', dto)
  },
  getById: async (id: number): Promise<ApiResponse<GuestDto>> => {
    return apiClient.get<GuestDto>(`/api/v1/guests/${id}`)
  },
  getByPhone: async (phone: string): Promise<ApiResponse<GuestDto>> => {
    return apiClient.get<GuestDto>(`/api/v1/guests/phone/${phone}`)
  },
  getByIdentityNumber: async (num: string): Promise<ApiResponse<GuestDto>> => {
    return apiClient.get<GuestDto>(`/api/v1/guests/identity/${num}`)
  },
  getStayHistory: async (id: number): Promise<ApiResponse<GuestStayHistoryDto>> => {
    return apiClient.get<GuestStayHistoryDto>(`/api/v1/guests/${id}/history`)
  },
  create: async (req: GuestCreateRequest): Promise<ApiResponse<GuestDto>> => {
    return apiClient.post<GuestDto>('/api/v1/guests', req)
  },
  update: async (id: number, req: GuestUpdateRequest): Promise<ApiResponse<GuestDto>> => {
    return apiClient.put<GuestDto>(`/api/v1/guests/${id}`, req)
  },
  deleteBatch: async (ids: number[]): Promise<ApiResponse<void>> => {
    return apiClient.delete<void>('/api/v1/guests/delete', { data: ids })
  },
}

export const companyService = {
  filter: async (dto: CompanySearchDto): Promise<ApiResponse<PageResponse<CompanyDto>>> => {
    return apiClient.post<PageResponse<CompanyDto>>('/api/v1/companies/filter', dto)
  },
  getById: async (id: number): Promise<ApiResponse<CompanyDto>> => {
    return apiClient.get<CompanyDto>(`/api/v1/companies/${id}`)
  },
  create: async (req: CompanyCreateRequest): Promise<ApiResponse<CompanyDto>> => {
    return apiClient.post<CompanyDto>('/api/v1/companies', req)
  },
  update: async (id: number, req: CompanyUpdateRequest): Promise<ApiResponse<CompanyDto>> => {
    return apiClient.put<CompanyDto>(`/api/v1/companies/${id}`, req)
  },
  deleteBatch: async (ids: number[]): Promise<ApiResponse<void>> => {
    return apiClient.delete<void>('/api/v1/companies/delete', { data: ids })
  },
}

export const auditLogService = {
  filter: async (dto: AuditLogSearchDto): Promise<ApiResponse<PageResponse<AuditLogDto>>> => {
    return apiClient.post<PageResponse<AuditLogDto>>('/api/v1/audit-logs/filter', dto)
  },
}
