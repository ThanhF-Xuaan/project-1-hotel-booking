export type UserRole =
  | 'ROLE_CHAIN_ADMIN'
  | 'ROLE_REGION_MANAGER'
  | 'ROLE_PROPERTY_MANAGER'
  | 'ROLE_RECEPTIONIST'
  | 'ROLE_HOUSEKEEPING'
  | 'ROLE_CUSTOMER'

export type DataScope = 'CHAIN' | 'REGION' | 'PROPERTY'

export interface CurrentUser {
  id: number
  keycloakId: string
  username: string
  fullName: string
  email?: string
  phone?: string
  roles: UserRole[]
  scopeType: DataScope
  scopeEntityId?: number
  departmentId?: number
}
