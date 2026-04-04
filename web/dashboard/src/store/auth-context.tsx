import { createContext, useContext, useState, useEffect, ReactNode } from 'react'
import { ApiError } from '@/services/api/client'

const API_BASE_URL = '/api/v1'

export type UserRole = 'ADMIN' | 'USER'

export interface User {
  id: string
  email: string
  fullName: string
  role: UserRole
  roles: UserRole[]
  avatar?: string
  merchantId?: string
}

interface AuthContextType {
  user: User | null
  token: string | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

const STORAGE_KEY = 'payflow_auth'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [token, setToken] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored) {
      try {
        const { user: storedUser, accessToken: storedToken } = JSON.parse(stored)
        setUser(storedUser)
        setToken(storedToken)
      } catch {
        localStorage.removeItem(STORAGE_KEY)
      }
    }
    setLoading(false)
  }, [])

  const login = async (email: string, password: string) => {
    try {
      const response = await fetch(`${API_BASE_URL}/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, password }),
      })

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}))
        throw new ApiError(
          errorData.message || 'Invalid credentials',
          response.status
        )
      }

      const data = await response.json()
      const { accessToken: authToken, user: userData } = data

      const roles: UserRole[] = userData.roles || [userData.role || 'USER']

      const user: User = {
        id: userData.id,
        email: userData.email,
        fullName: userData.fullName,
        role: roles.includes('ADMIN') ? 'ADMIN' : roles[0],
        roles,
        avatar: userData.avatar,
        merchantId: userData.merchantId,
      }

      setUser(user)
      setToken(authToken)
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ user, accessToken: authToken }))
    } catch (error) {
      if (error instanceof ApiError) {
        throw error
      }
      throw new ApiError('Authentication failed. Please check your connection and try again.', 500)
    }
  }

  const logout = () => {
    setUser(null)
    setToken(null)
    localStorage.removeItem(STORAGE_KEY)
  }

  return (
    <AuthContext.Provider value={{ user, token, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
