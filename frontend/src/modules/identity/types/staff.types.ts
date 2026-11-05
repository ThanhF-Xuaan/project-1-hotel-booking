export type StaffScopeType = 'CHAIN' | 'REGION' | 'PROPERTY';
export type StaffStatus = 'ACTIVE' | 'SUSPENDED' | 'RESIGNED';

export interface StaffDto {
  id: number;
  keycloakId?: string;
  roleId: number;
  roleName: string;
  roleCode: string;
  scopeType: StaffScopeType;
  scopeEntityId?: number;
  departmentId?: number;
  departmentName?: string;
  username: string;
  email: string;
  phone: string;
  firstName: string;
  lastName: string;
  fullName: string;
  status: StaffStatus;
  createdAt?: string;
  updatedAt?: string;
}

export interface StaffSearchDto {
  page?: number;
  pageSize?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
  keyword?: string;
  roleId?: number;
  departmentId?: number;
  scopeType?: StaffScopeType;
  scopeEntityId?: number;
  status?: string;
}

export interface StaffCreateRequest {
  username: string;
  password?: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  roleId: number;
  scopeType: StaffScopeType;
  scopeEntityId?: number;
  departmentId?: number;
  status?: StaffStatus;
}

export interface StaffUpdateRequest {
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  roleId?: number;
  scopeType?: StaffScopeType;
  scopeEntityId?: number;
  departmentId?: number;
  status?: StaffStatus;
  password?: string;
}

export interface StaffStatusUpdateRequest {
  status: StaffStatus;
  reason?: string;
}

export interface StaffPasswordResetRequest {
  newPassword: string;
}
