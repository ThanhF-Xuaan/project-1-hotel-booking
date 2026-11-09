import { apiClient } from '../../../core/api/client';
import type { PageResponse } from '../../../core/api/types';
import type {
  PermissionDto,
  PermissionSearchDto,
  PermissionCreateRequest,
  PermissionUpdateRequest,
} from '../types/role.types';

const BASE_URL = '/api/v1/permissions';

export const permissionService = {
  getAll: () => apiClient.get<PermissionDto[]>(BASE_URL),

  filter: (params: PermissionSearchDto) =>
    apiClient.post<PageResponse<PermissionDto>>(`${BASE_URL}/filter`, params),

  create: (data: PermissionCreateRequest) =>
    apiClient.post<PermissionDto>(BASE_URL, data),

  update: (id: number, data: PermissionUpdateRequest) =>
    apiClient.put<PermissionDto>(`${BASE_URL}/${id}`, data),

  deleteBatch: (ids: number[]) =>
    apiClient.delete<void>(`${BASE_URL}/delete`, { data: ids }),

  getResources: () => apiClient.get<string[]>(`${BASE_URL}/resources`),

  getActions: () => apiClient.get<string[]>(`${BASE_URL}/actions`),
};

export default permissionService;

