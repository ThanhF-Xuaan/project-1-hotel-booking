import { apiClient } from '../../../core/api/client';
import type { PageResponse } from '../../../core/api/types';
import type {
  DepartmentDto,
  DepartmentCreateRequest,
  DepartmentUpdateRequest,
  DepartmentSearchDto,
} from '../types/department.types';

const BASE_URL = '/api/v1/departments';

export const departmentService = {
  filter: (params: DepartmentSearchDto) =>
    apiClient.post<PageResponse<DepartmentDto>>(`${BASE_URL}/filter`, params),

  getAllActive: () => apiClient.get<DepartmentDto[]>(`${BASE_URL}/active`),

  getById: (id: number) => apiClient.get<DepartmentDto>(`${BASE_URL}/${id}`),

  create: (data: DepartmentCreateRequest) => apiClient.post<DepartmentDto>(BASE_URL, data),

  update: (id: number, data: DepartmentUpdateRequest) =>
    apiClient.put<DepartmentDto>(`${BASE_URL}/${id}`, data),

  deleteBatch: (ids: number[]) =>
    apiClient.delete<void>(`${BASE_URL}/delete`, { data: ids }),
};

export default departmentService;

