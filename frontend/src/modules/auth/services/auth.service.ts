import { apiClient } from '../../../core/api/client'
import type { LoginRequest, TokenResponse } from '../types/auth.types'

export const AuthService = {
  login: async (data: LoginRequest) => {
    return apiClient.post<TokenResponse>('/api/v1/auth/login', data)
  },
  
  logout: async () => {
    return apiClient.post<void>('/api/v1/auth/logout', {})
  }
}
