export interface StaffDto {
  id: number
  keycloakId?: string
  roleId: number
  roleName?: string
  roleCode?: string
  scopeType?: 'CHAIN' | 'REGION' | 'PROPERTY' | string
  scopeEntityId?: number
  departmentId?: number
  departmentName?: string
  username: string
  email: string
  phone?: string
  firstName?: string
  lastName?: string
  fullName: string
  status: 'ACTIVE' | 'SUSPENDED' | 'RESIGNED' | string
  createdAt?: string
  updatedAt?: string
}

export interface StaffSearchDto {
  page?: number
  pageSize?: number
  keyword?: string
  roleId?: number
  departmentId?: number
  scopeType?: string
  scopeEntityId?: number
  status?: string
}

export interface StaffCreateRequest {
  username: string
  password?: string
  email: string
  phone?: string
  firstName: string
  lastName: string
  roleId: number
  departmentId?: number
  scopeType: 'CHAIN' | 'REGION' | 'PROPERTY' | string
  scopeEntityId?: number
}

export interface StaffUpdateRequest {
  email: string
  phone?: string
  firstName: string
  lastName: string
  roleId: number
  departmentId?: number
  scopeType: 'CHAIN' | 'REGION' | 'PROPERTY' | string
  scopeEntityId?: number
}

export interface StaffStatusUpdateRequest {
  status: 'ACTIVE' | 'SUSPENDED' | 'RESIGNED' | string
  reason?: string
}

export interface PermissionDto {
  id: number
  code: string
  name: string
  module: string
  description?: string
}

export interface RoleDto {
  id: number
  name: string
  code: string
  status: 'ACTIVE' | 'INACTIVE' | string
  permissions?: PermissionDto[]
  createdAt?: string
  updatedAt?: string
}

export interface RoleSearchDto {
  page?: number
  pageSize?: number
  keyword?: string
  status?: string
}

export interface RoleCreateRequest {
  name: string
  code: string
  permissionIds?: number[]
}

export interface RoleUpdateRequest {
  name: string
  permissionIds?: number[]
  status: 'ACTIVE' | 'INACTIVE' | string
}

export interface GuestDto {
  id: number
  publicId?: string
  fullName: string
  birthDate?: string
  identityType?: string
  identityNumber?: string
  nationality?: string
  email?: string
  phone: string
  status: 'ACTIVE' | 'INACTIVE' | string
  createdAt?: string
  updatedAt?: string
}

export interface GuestSearchDto {
  page?: number
  pageSize?: number
  fullName?: string
  phone?: string
  email?: string
  identityNumber?: string
}

export interface GuestCreateRequest {
  fullName: string
  birthDate?: string
  identityType?: string
  identityNumber?: string
  nationality?: string
  email?: string
  phone: string
}

export interface GuestUpdateRequest {
  fullName: string
  birthDate?: string
  identityType?: string
  identityNumber?: string
  nationality?: string
  email?: string
  phone: string
  status: string
}

export interface GuestBookingSummaryDto {
  bookingId: number
  bookingNumber: string
  hotelName?: string
  status: string
  totalPrice?: number
  bookingDate?: string
}

export interface GuestStayHistoryDto {
  guestId: number
  fullName: string
  phone: string
  totalStays: number
  totalSpent: number
  bookings?: GuestBookingSummaryDto[]
}

export interface CompanyDto {
  id: number
  name: string
  taxCode: string
  address?: string
  contactName?: string
  contactPhone?: string
  contactEmail?: string
  status: 'ACTIVE' | 'INACTIVE' | string
  createdAt?: string
  updatedAt?: string
}

export interface CompanySearchDto {
  page?: number
  pageSize?: number
  companyName?: string
  taxCode?: string
  contactPerson?: string
  contactPhone?: string
  status?: string
}

export interface CompanyCreateRequest {
  name: string
  taxCode: string
  address?: string
  contactName?: string
  contactPhone?: string
  contactEmail?: string
}

export interface CompanyUpdateRequest {
  name: string
  taxCode: string
  address?: string
  contactName?: string
  contactPhone?: string
  contactEmail?: string
  status: string
}

export interface AuditLogDto {
  id: number
  staffId?: number
  staffUsername?: string
  staffFullName?: string
  actionType: string
  entityName: string
  entityId?: string
  oldValues?: string
  newValues?: string
  ipAddress?: string
  userAgent?: string
  createdAt: string
}

export interface AuditLogSearchDto {
  page?: number
  pageSize?: number
  actionType?: string
  entityName?: string
  entityId?: string
  actorUsername?: string
  fromDate?: string
  toDate?: string
}
