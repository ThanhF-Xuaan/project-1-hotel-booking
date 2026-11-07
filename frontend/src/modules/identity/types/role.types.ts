export interface PermissionDto {
  id: number;
  action: string;
  resource: string;
  description?: string;
  roleCodes?: string[];
  roleNames?: string[];
  status?: string;
}

export interface PermissionSearchDto {
  page?: number;
  pageSize?: number;
  keyword?: string;
  resource?: string;
  action?: string;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export interface RoleDto {
  id: number;
  name: string;
  code: string;
  status: 'ACTIVE' | 'INACTIVE';
  permissions?: PermissionDto[];
  createdAt?: string;
  updatedAt?: string;
}

export interface RoleCreateRequest {
  name: string;
  code: string;
  status?: string;
  permissionIds?: number[];
}

export interface RoleUpdateRequest {
  name: string;
  status?: string;
  permissionIds?: number[];
}

export interface RoleSearchDto {
  page?: number;
  pageSize?: number;
  keyword?: string;
  status?: string;
}

