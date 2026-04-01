import axios, { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse, AxiosError } from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_URL || '/api/v1'

export interface ApiResponse<T> {
  data: T
  message?: string
  success: boolean
}

export interface PaginatedResponse<T> {
  data: T[]
  total: number
  page: number
  limit: number
  totalPages: number
}

class ApiClient {
  private client: AxiosInstance

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      headers: {
        'Content-Type': 'application/json',
      },
      timeout: 30000,
    })

    this.setupInterceptors()
  }

  private setupInterceptors(): void {
    this.client.interceptors.request.use(
      (config: InternalAxiosRequestConfig) => {
        const token = this.getToken()
        if (token && config.headers) {
          config.headers.Authorization = `Bearer ${token}`
        }
        return config
      },
      (error) => Promise.reject(error)
    )

    this.client.interceptors.response.use(
      (response: AxiosResponse) => response,
      async (error: AxiosError) => {
        const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

        if (error.response?.status === 401 && !originalRequest._retry) {
          originalRequest._retry = true

          try {
            const newToken = await this.refreshToken()
            if (newToken && originalRequest.headers) {
              originalRequest.headers.Authorization = `Bearer ${newToken}`
              return this.client(originalRequest)
            }
          } catch (refreshError) {
            this.clearAuth()
            window.location.href = '/login'
            return Promise.reject(refreshError)
          }
        }

        return Promise.reject(error)
      }
    )
  }

  private getToken(): string | null {
    const stored = localStorage.getItem('payflow_auth')
    if (stored) {
      try {
        const { accessToken } = JSON.parse(stored)
        return accessToken
      } catch {
        return null
      }
    }
    return null
  }

  private async refreshToken(): Promise<string | null> {
    const stored = localStorage.getItem('payflow_auth')
    if (!stored) return null

    try {
      const { refreshToken } = JSON.parse(stored)
      const response = await axios.post(`${API_BASE_URL}/auth/refresh`, { refreshToken })
      const { accessToken, refreshToken: newRefreshToken } = response.data.data

      localStorage.setItem(
        'payflow_auth',
        JSON.stringify({ accessToken, refreshToken: newRefreshToken })
      )

      return accessToken
    } catch {
      return null
    }
  }

  private clearAuth(): void {
    localStorage.removeItem('payflow_auth')
  }

  async get<T>(endpoint: string, params?: Record<string, unknown>): Promise<T> {
    const response = await this.client.get<ApiResponse<T>>(endpoint, { params })
    return response.data.data
  }

  async post<T>(endpoint: string, data?: unknown): Promise<T> {
    const response = await this.client.post<ApiResponse<T>>(endpoint, data)
    return response.data.data
  }

  async put<T>(endpoint: string, data?: unknown): Promise<T> {
    const response = await this.client.put<ApiResponse<T>>(endpoint, data)
    return response.data.data
  }

  async patch<T>(endpoint: string, data?: unknown): Promise<T> {
    const response = await this.client.patch<ApiResponse<T>>(endpoint, data)
    return response.data.data
  }

  async delete<T>(endpoint: string): Promise<T> {
    const response = await this.client.delete<ApiResponse<T>>(endpoint)
    return response.data.data
  }

  getRawClient(): AxiosInstance {
    return this.client
  }
}

export const apiClient = new ApiClient()
