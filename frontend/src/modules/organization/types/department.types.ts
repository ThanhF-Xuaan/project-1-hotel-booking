export interface DepartmentDto {
  id: number
  code: string
  name: string
  status: 'ACTIVE' | 'INACTIVE' | string
  createdAt?: string
  updatedAt?: string
}

export interface DepartmentSearchDto {
  page?: number
  pageSize?: number
  keyword?: string
  status?: string
}

export interface DepartmentCreateRequest {
  code: string
  name: string
  description?: string
}

export interface DepartmentUpdateRequest {
  name: string
  description?: string
  status: 'ACTIVE' | 'INACTIVE' | string
}
