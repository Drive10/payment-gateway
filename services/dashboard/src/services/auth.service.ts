import { apiClient } from '@/services/api-client'

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
  firstName: string
  lastName: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: User
}

export interface User {
  id: string
  email: string
  firstName: string
  lastName: string
  role: 'ADMIN' | 'USER' | 'MERCHANT'
  enabled: boolean
  createdAt: string
}

class AuthApi {
  async login(request: LoginRequest): Promise<AuthResponse> {
    return apiClient.post<AuthResponse>('/auth/login', request)
  }

  async register(request: RegisterRequest): Promise<AuthResponse> {
    return apiClient.post<AuthResponse>('/auth/register', request)
  }

  async refreshToken(refreshToken: string): Promise<{ accessToken: string; refreshToken: string }> {
    return apiClient.post<{ accessToken: string; refreshToken: string }>('/auth/refresh', { refreshToken })
  }

  async logout(): Promise<void> {
    return apiClient.post<void>('/auth/logout')
  }

  async getCurrentUser(): Promise<User> {
    return apiClient.get<User>('/auth/me')
  }

  async updateProfile(data: Partial<User>): Promise<User> {
    return apiClient.patch<User>('/auth/profile', data)
  }

  async changePassword(currentPassword: string, newPassword: string): Promise<void> {
    return apiClient.post<void>('/auth/change-password', { currentPassword, newPassword })
  }
}

export const authApi = new AuthApi()
