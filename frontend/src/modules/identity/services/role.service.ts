import { apiClient } from '../../../core/api/client';
import type { PageResponse } from '../../../core/api/types';
import type {
  RoleDto,
  PermissionDto,
  RoleCreateRequest,
  RoleUpdateRequest,
  RoleSearchDto,
} from '../types/role.types';

const BASE_URL = '/api/v1/roles';

export const roleService = {
  filter: (params: RoleSearchDto) =>
    apiClient.post<PageResponse<RoleDto>>(`${BASE_URL}/filter`, params),

  getAllPermissions: () =>
    apiClient.get<PermissionDto[]>(`${BASE_URL}/permissions`),

  getById: (id: number) => apiClient.get<RoleDto>(`${BASE_URL}/${id}`),

  create: (data: RoleCreateRequest) => apiClient.post<RoleDto>(BASE_URL, data),

  update: (id: number, data: RoleUpdateRequest) =>
    apiClient.put<RoleDto>(`${BASE_URL}/${id}`, data),

  deleteBatch: (ids: number[]) =>
    apiClient.delete<void>(`${BASE_URL}/delete`, { data: ids }),
};

export default roleService;

