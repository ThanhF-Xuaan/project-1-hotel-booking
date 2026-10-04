import type { UserRole } from '../types/auth.types'

/**
 * Quản lý phiên đăng nhập Frontend với Keycloak (password grant cho môi trường dev).
 * - auth_token: access_token (hạn ~1 giờ, được client.ts tự làm mới bằng refresh_token)
 * - auth_refresh_token: refresh_token (dùng khi access_token hết hạn)
 */

const TOKEN_KEY = 'auth_token'
const REFRESH_TOKEN_KEY = 'auth_refresh_token'

const KEYCLOAK_BASE = import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8081'
const KEYCLOAK_REALM = import.meta.env.VITE_KEYCLOAK_REALM || 'hotel-realm'
const KEYCLOAK_CLIENT_ID = import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'hotel-frontend'
export const KEYCLOAK_TOKEN_URL = `${KEYCLOAK_BASE}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/token`

interface TokenResponse {
  access_token: string
  refresh_token?: string
  expires_in?: number
}

export interface SessionUser {
  username: string
  fullName: string
  roles: UserRole[]
}

/** Giải mã payload của JWT (base64url) — không cần thư viện bên thứ ba. */
const decodeJwtPayload = (token: string): Record<string, unknown> | null => {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const bytes = Uint8Array.from(atob(base64), (c) => c.charCodeAt(0))
    return JSON.parse(new TextDecoder().decode(bytes))
  } catch {
    return null
  }
}

export const getAccessToken = (): string | null => localStorage.getItem(TOKEN_KEY)

export const isAuthenticated = (): boolean => !!getAccessToken()

/** Đọc thông tin user hiện tại trực tiếp từ JWT (không gọi API). */
export const getCurrentUser = (): SessionUser | null => {
  const token = getAccessToken()
  if (!token) return null
  const payload = decodeJwtPayload(token)
  if (!payload) return null
  const realmAccess = payload.realm_access as { roles?: string[] } | undefined
  const username = (payload.preferred_username as string) || (payload.username as string) || ''
  return {
    username,
    fullName: (payload.name as string) || username,
    // Keycloak cấp role dạng "CHAIN_ADMIN" → bổ sung tiền tố "ROLE_" khớp định danh backend
    roles: (realmAccess?.roles || []).map((r) =>
      r.startsWith('ROLE_') ? r : `ROLE_${r}`
    ) as UserRole[],
  }
}

/**
 * Đăng nhập: gửi username/password tới Keycloak (grant_type=password).
 * Thành công → lưu token; thất bại → throw Error với thông báo tiếng Việt.
 */
export const login = async (username: string, password: string): Promise<void> => {
  const res = await fetch(KEYCLOAK_TOKEN_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'password',
      client_id: KEYCLOAK_CLIENT_ID,
      username,
      password,
    }),
  })

  if (!res.ok) {
    const err = (await res.json().catch(() => ({}))) as {
      error?: string
      error_description?: string
    }
    if (err.error === 'invalid_grant') {
      throw new Error('Tên đăng nhập hoặc mật khẩu không đúng')
    }
    throw new Error(err.error_description || err.error || 'Đăng nhập thất bại, vui lòng thử lại')
  }

  const data = (await res.json()) as TokenResponse
  localStorage.setItem(TOKEN_KEY, data.access_token)
  if (data.refresh_token) localStorage.setItem(REFRESH_TOKEN_KEY, data.refresh_token)
}

/** Đăng xuất: xóa token khỏi trình duyệt (gọi trước khi chuyển về /login). */
export const logout = (): void => {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}
