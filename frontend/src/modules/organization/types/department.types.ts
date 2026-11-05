export interface DepartmentDto {
  id: number;
  code: string;
  name: string;
  status: 'ACTIVE' | 'INACTIVE';
  createdAt?: string;
  updatedAt?: string;
}

export interface DepartmentCreateRequest {
  code: string;
  name: string;
  status?: string;
}

export interface DepartmentUpdateRequest {
  name: string;
  status: string;
}

export interface DepartmentSearchDto {
  page?: number;
  pageSize?: number;
  keyword?: string;
  status?: string;
}

