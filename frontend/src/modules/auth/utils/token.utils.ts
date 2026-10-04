export interface UserProfile {
  name: string
  email: string
  role: string
  username: string
}

const ROLE_LABELS: Record<string, string> = {
  CHAIN_ADMIN: 'Admin Toàn Chuỗi',
  ROLE_CHAIN_ADMIN: 'Admin Toàn Chuỗi',
  REGION_MANAGER: 'Quản Lý Vùng',
  ROLE_REGION_MANAGER: 'Quản Lý Vùng',
  PROPERTY_MANAGER: 'Tổng Quản Lý',
  ROLE_PROPERTY_MANAGER: 'Tổng Quản Lý',
  RECEPTIONIST: 'Lễ Tân',
  ROLE_RECEPTIONIST: 'Lễ Tân',
  HOUSEKEEPING: 'Buồng Phòng',
  ROLE_HOUSEKEEPING: 'Buồng Phòng',
  CUSTOMER: 'Khách Hàng',
  ROLE_CUSTOMER: 'Khách Hàng',
}

export function parseJwt(token: string): Record<string, any> | null {
  try {
    const parts = token.split('.')
    if (parts.length < 2) return null

    const base64Url = parts[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const jsonPayload = decodeURIComponent(
      window
        .atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    return JSON.parse(jsonPayload)
  } catch (error) {
    console.error('Lỗi khi parse JWT token:', error)
    return null
  }
}

export function getUserProfile(): UserProfile | null {
  const token = localStorage.getItem('auth_token')
  if (!token) return null

  const payload = parseJwt(token)
  if (!payload) return null

  // Trích xuất vai trò từ realm_access.roles
  const realmRoles: string[] = payload.realm_access?.roles || []
  let matchedRole = 'Nhân viên'
  for (const r of realmRoles) {
    if (ROLE_LABELS[r]) {
      matchedRole = ROLE_LABELS[r]
      break
    }
  }

  const name =
    payload.name ||
    [payload.given_name, payload.family_name].filter(Boolean).join(' ') ||
    payload.preferred_username ||
    'Người dùng'

  const email = payload.email || 'Chưa cập nhật email'
  const username = payload.preferred_username || payload.sub || ''

  return {
    name,
    email,
    role: matchedRole,
    username,
  }
}

export function getUserRoles(): string[] {
  const token = localStorage.getItem('auth_token')
  if (!token) return []

  const payload = parseJwt(token)
  if (!payload) return []

  const roles: string[] = payload.realm_access?.roles || []
  return roles.map((r) => r.replace(/^ROLE_/, ''))
}

export function hasAnyRole(requiredRoles?: string[]): boolean {
  if (!requiredRoles || requiredRoles.length === 0) {
    return true
  }

  const userRoles = getUserRoles()
  if (userRoles.length === 0) {
    return false
  }

  if (userRoles.includes('CHAIN_ADMIN')) {
    return true
  }

  const normalizedRequired = requiredRoles.map((r) => r.replace(/^ROLE_/, ''))
  return userRoles.some((role) => normalizedRequired.includes(role))
}

