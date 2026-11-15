import axios, { type AxiosError, type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import type { ApiResponse } from './types'

const rawBaseUrl =
  import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || ''

// Chuẩn hóa: Tự động loại bỏ hậu tố '/api' hoặc '/api/' nếu có từ biến môi trường
// do toàn bộ service trong frontend đều đã tự định nghĩa prefix '/api/v1/...'
const API_BASE_URL = rawBaseUrl.replace(/\/api\/?$/, '')

// ── Quản lý Token & Làm mới phiên đăng nhập (Silent Refresh Token) ─────────
const TOKEN_KEY = 'auth_token'
const REFRESH_TOKEN_KEY = 'auth_refresh_token'

/**
 * Làm mới access_token bằng refresh_token thông qua Backend API /api/v1/auth/refresh.
 * - Chống "dội" refresh khi nhiều request cùng gặp lúc token hết hạn: gộp chung 1 Promise (Shared Promise Queue).
 * - Trả về access_token mới, hoặc null nếu không có/không refresh được.
 */
let refreshPromise: Promise<string | null> | null = null

const doRefresh = async (): Promise<string | null> => {
  try {
    const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)
    if (!refreshToken) return null

    const refreshUrl = `${API_BASE_URL}/api/v1/auth/refresh`
    const res = await fetch(refreshUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        refreshToken: refreshToken,
        refresh_token: refreshToken,
      }),
    })

    if (!res.ok) {
      console.warn('Yêu cầu refresh token không thành công, mã HTTP:', res.status)
      return null
    }

    const data = (await res.json()) as {
      code?: number
      result?: {
        access_token?: string
        refresh_token?: string
        accessToken?: string
        refreshToken?: string
      }
    }

    const newAccessToken = data.result?.access_token || data.result?.accessToken
    const newRefreshToken = data.result?.refresh_token || data.result?.refreshToken

    if (!newAccessToken) return null

    localStorage.setItem(TOKEN_KEY, newAccessToken)
    // Cập nhật refresh_token mới (Keycloak Refresh Token Rotation)
    if (newRefreshToken) {
      localStorage.setItem(REFRESH_TOKEN_KEY, newRefreshToken)
    }
    console.info('Làm mới phiên làm việc thành công')
    return newAccessToken
  } catch (error) {
    console.error('Lỗi khi làm mới access token:', error)
    return null
  }
}

export const refreshAccessToken = (): Promise<string | null> => {
  if (!refreshPromise) {
    // .finally đặt NGOÀI async body: đảm bảo giải phóng singleton promise khi xong
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

/**
 * Kiểm tra xem token JWT sắp hết hạn trong offsetSeconds hay không.
 */
export const isTokenExpiringSoon = (token: string, offsetSeconds = 15): boolean => {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return true
    const payloadBase64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const decodedJson = atob(payloadBase64)
    const payload = JSON.parse(decodedJson) as { exp?: number }
    if (!payload.exp) return false
    return payload.exp * 1000 <= Date.now() + offsetSeconds * 1000
  } catch {
    return true
  }
}

/**
 * Kiểm tra xem access_token JWT đã hết hạn chưa (buffer 5 giây phòng lệch đồng hồ).
 */
export const isTokenExpired = (token: string): boolean => isTokenExpiringSoon(token, 5)

const axiosInstance: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000,
})

// Request Interceptor: Attach JWT Token & Proactive Refresh
axiosInstance.interceptors.request.use(
  async (config) => {
    // Bỏ qua làm mới token cho endpoint refresh chính nó (phòng ngừa vòng lặp)
    const isRefreshRequest = config.url?.includes('/api/v1/auth/refresh')
    if (isRefreshRequest) {
      return config
    }

    let token = localStorage.getItem(TOKEN_KEY)
    const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)

    // Proactive Refresh: Nếu token sắp hết hạn trong 15s và còn refresh token
    if (token && isTokenExpiringSoon(token, 15)) {
      if (refreshToken) {
        const newToken = await refreshAccessToken()
        if (newToken) {
          token = newToken
        } else {
          // Refresh token đã hết hạn hoặc không hợp lệ -> dọn dẹp storage
          localStorage.removeItem(TOKEN_KEY)
          localStorage.removeItem(REFRESH_TOKEN_KEY)
          token = null
        }
      } else {
        localStorage.removeItem(TOKEN_KEY)
        token = null
      }
    }

    if (token && config.headers) {
      if (typeof config.headers.set === 'function') {
        config.headers.set('Authorization', `Bearer ${token}`)
      } else {
        config.headers.Authorization = `Bearer ${token}`
      }
    } else if (!token && config.headers) {
      if (typeof (config.headers as unknown as { delete?: (k: string) => void }).delete === 'function') {
        ;(config.headers as unknown as { delete: (k: string) => void }).delete('Authorization')
      } else {
        delete (config.headers as Record<string, unknown>).Authorization
      }
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Response Interceptor: Parse standard ApiResponse & Reactive 401 Fallback
axiosInstance.interceptors.response.use(
  (response: AxiosResponse) => response,
  async (error: AxiosError) => {
    const status = error.response?.status
    const originalConfig = error.config as
      | (AxiosRequestConfig & { __retriedAfterRefresh?: boolean; __retriedWithoutAuth?: boolean })
      | undefined

    // 401 → thử LÀM MỚI token bằng refresh_token rồi gọi lại request gốc 1 lần
    if (status === 401 && originalConfig && !originalConfig.__retriedAfterRefresh) {
      const hadSession = !!localStorage.getItem(TOKEN_KEY) || !!localStorage.getItem(REFRESH_TOKEN_KEY)
      const newToken = await refreshAccessToken()
      if (newToken) {
        originalConfig.__retriedAfterRefresh = true
        originalConfig.headers = { ...originalConfig.headers, Authorization: `Bearer ${newToken}` }
        console.info('Đã tự làm mới token sau 401, thử lại request...')
        return axiosInstance.request(originalConfig)
      }

      // Không refresh được: dọn dẹp token trong storage
      if (hadSession) {
        console.warn('Phiên làm việc hết hạn — dọn dẹp token')
        localStorage.removeItem(TOKEN_KEY)
        localStorage.removeItem(REFRESH_TOKEN_KEY)
      } else {
        console.warn('Phiên làm việc hết hạn hoặc chưa đăng nhập')
      }

      const isPublicArea =
        typeof window !== 'undefined' &&
        (window.location.pathname === '/' ||
          window.location.pathname.startsWith('/portal') ||
          window.location.pathname.startsWith('/payment/result') ||
          Boolean(originalConfig.url?.includes('/api/v1/public/')) ||
          Boolean(originalConfig.url?.includes('/api/public/')))

      // Tự hồi phục (Self-healing): Nếu request thuộc khu vực công khai, thử lại 1 lần dưới dạng khách vãng lai
      if (isPublicArea && !originalConfig.__retriedWithoutAuth) {
        originalConfig.__retriedWithoutAuth = true
        if (originalConfig.headers) {
          if (typeof (originalConfig.headers as unknown as { delete?: (k: string) => void }).delete === 'function') {
            ;(originalConfig.headers as unknown as { delete: (k: string) => void }).delete('Authorization')
          } else {
            delete (originalConfig.headers as Record<string, unknown>).Authorization
          }
        }
        console.info('Khu vực công khai: Tự động thử lại request dưới tư cách khách vãng lai (không kèm token)...')
        return axiosInstance.request(originalConfig)
      }

      if (hadSession && !isPublicArea && !window.location.pathname.startsWith('/login')) {
        window.location.replace('/login?session=expired')
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

  postBlob: async (url: string, data?: unknown, config?: AxiosRequestConfig): Promise<Blob> => {
    try {
      const res = await axiosInstance.post(url, data, {
        ...config,
        responseType: 'blob',
      })
      return res.data as Blob
    } catch (err: unknown) {
      if (
        err &&
        typeof err === 'object' &&
        'response' in err &&
        (err as AxiosError).response?.data instanceof Blob
      ) {
        const errorBlob = (err as AxiosError).response!.data as Blob
        if (errorBlob.type?.includes('application/json')) {
          const text = await errorBlob.text()
          try {
            const json = JSON.parse(text)
            throw new Error(json.message || 'Lỗi khi tải file từ máy chủ', { cause: err })
          } catch (parseErr) {
            if (parseErr instanceof Error && parseErr.message !== text) {
              throw parseErr
            }
          }
        }
      }
      throw err
    }
  },
}

export default apiClient
