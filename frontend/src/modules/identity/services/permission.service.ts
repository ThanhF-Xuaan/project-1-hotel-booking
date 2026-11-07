import { apiClient } from '../../../core/api/client';
import type { PageResponse } from '../../../core/api/types';
import type { PermissionDto, PermissionSearchDto } from '../types/role.types';

const BASE_URL = '/api/v1/permissions';

export const permissionService = {
  getAll: () => apiClient.get<PermissionDto[]>(BASE_URL),

  filter: (params: PermissionSearchDto) =>
    apiClient.post<PageResponse<PermissionDto>>(`${BASE_URL}/filter`, params),
};

export default permissionService;

