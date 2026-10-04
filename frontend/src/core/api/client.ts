import axios, { type AxiosError, type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import type { ApiResponse } from './types'

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

// ── Keycloak (lấy/làm mới token) ─────────────────────────────────────────────
const KEYCLOAK_BASE =
  import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8081'
const KEYCLOAK_REALM = import.meta.env.VITE_KEYCLOAK_REALM || 'hotel-realm'
const KEYCLOAK_CLIENT_ID = import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'hotel-frontend'
const KEYCLOAK_TOKEN_URL = `${KEYCLOAK_BASE}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/token`

const TOKEN_KEY = 'auth_token'
const REFRESH_TOKEN_KEY = 'auth_refresh_token'

/**
 * Làm mới access_token bằng refresh_token (grant_type=refresh_token).
 * - Chống "dội" refresh khi nhiều request cùng gặp 401: gộp chung 1 Promise.
 * - Trả về access_token mới, hoặc null nếu không có/không refresh được.
 */
let refreshPromise: Promise<string | null> | null = null

const doRefresh = async (): Promise<string | null> => {
  try {
    const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)
    if (!refreshToken) return null
    const res = await fetch(KEYCLOAK_TOKEN_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        grant_type: 'refresh_token',
        refresh_token: refreshToken,
        client_id: KEYCLOAK_CLIENT_ID,
      }),
    })
    if (!res.ok) return null
    const data = (await res.json()) as {
      access_token?: string
      refresh_token?: string
    }
    if (!data.access_token) return null
    localStorage.setItem(TOKEN_KEY, data.access_token)
    // Keycloak cấp refresh_token mới (rotation) → lưu lại
    if (data.refresh_token) localStorage.setItem(REFRESH_TOKEN_KEY, data.refresh_token)
    return data.access_token
  } catch {
    return null
  }
}

const refreshAccessToken = (): Promise<string | null> => {
  if (!refreshPromise) {
    // .finally đặt NGOÀI async body: nếu đặt trong, nhánh "không có refresh_token"
    // (return đồng bộ) sẽ gán refreshPromise = null TRƯỚC lệnh gán → promise "kẹt"
    // và không bao giờ refresh lại được cho tới khi tải lại trang.
    refreshPromise = doRefresh().finally(() => {
      refreshPromise = null
    })
  }
  return refreshPromise
}

/** Lỗi có phải do "hết phiên đăng nhập" (401 + không refresh được) không? */
export const isSessionExpiredError = (error: unknown): boolean =>
  typeof error === 'object' && error !== null && (error as { isSessionExpired?: boolean }).isSessionExpired === true

/** Lấy HTTP status của lỗi API (nếu có) — dùng để phân biệt 404 "không tìm thấy" với lỗi khác. */
export const getErrorStatus = (error: unknown): number | undefined => {
  if (typeof error === 'object' && error !== null) {
    const resp = (error as AxiosError).response
    if (resp && typeof resp.status === 'number') return resp.status
  }
  return undefined
}

const markSessionExpired = (error: AxiosError): AxiosError => {
  ;(error as AxiosError & { isSessionExpired?: boolean }).isSessionExpired = true
  return error
}

const axiosInstance: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000,
})

// Request Interceptor: Attach JWT Token if available
axiosInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Response Interceptor: Parse standard ApiResponse
axiosInstance.interceptors.response.use(
  (response: AxiosResponse) => response,
  async (error: AxiosError) => {
    const status = error.response?.status
    const originalConfig = error.config as (AxiosRequestConfig & { __retriedAfterRefresh?: boolean }) | undefined

    // 401 → thử LÀM MỚI token bằng refresh_token rồi gọi lại request gốc 1 lần
    if (status === 401 && originalConfig && !originalConfig.__retriedAfterRefresh) {
      const hadToken = !!localStorage.getItem(TOKEN_KEY)
      const newToken = await refreshAccessToken()
      if (newToken) {
        originalConfig.__retriedAfterRefresh = true
        originalConfig.headers = { ...originalConfig.headers, Authorization: `Bearer ${newToken}` }
        console.info('Đã tự làm mới token, thử lại request...')
        return axiosInstance.request(originalConfig)
      }
      // Không refresh được: nếu TRƯỚC ĐÓ có token (hết hạn thật) → về trang đăng nhập.
      // Không có token từ đầu (API công khai/khách vãng lai) → để trang tự xử lý, không ép chuyển trang.
      if (hadToken) {
        console.warn('Phiên làm việc hết hạn — chuyển về trang đăng nhập')
        localStorage.removeItem(TOKEN_KEY)
        localStorage.removeItem(REFRESH_TOKEN_KEY)
        if (!window.location.pathname.startsWith('/login')) {
          window.location.replace('/login?session=expired')
        }
      } else {
        console.warn('Phiên làm việc hết hạn hoặc chưa đăng nhập')
      }
      return Promise.reject(markSessionExpired(error))
    }

    if (status === 401) {
      // Đã thử refresh rồi vẫn 401 (hoặc gọi lại vẫn bị chặn)
      console.warn('Phiên làm việc hết hạn hoặc chưa đăng nhập')
      return Promise.reject(markSessionExpired(error))
    }
    return Promise.reject(error)
  }
)

export const apiClient = {
  get: <T>(url: string, config?: AxiosRequestConfig) =>
    axiosInstance.get<ApiResponse<T>>(url, config).then((res) => res.data),

  post: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    axiosInstance.post<ApiResponse<T>>(url, data, config).then((res) => res.data),

  put: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    axiosInstance.put<ApiResponse<T>>(url, data, config).then((res) => res.data),

  patch: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    axiosInstance.patch<ApiResponse<T>>(url, data, config).then((res) => res.data),

  delete: <T>(url: string, config?: AxiosRequestConfig) =>
    axiosInstance.delete<ApiResponse<T>>(url, config).then((res) => res.data),
}

export default apiClient
