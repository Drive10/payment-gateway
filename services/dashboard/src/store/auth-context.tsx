import { createContext, useContext, useState, useEffect, ReactNode } from 'react'
import { ApiError } from '@/services/api/client'

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1'

export type UserRole = 'ADMIN' | 'USER'

export interface User {
  id: string
  email: string
  fullName: string
  role: UserRole
  avatar?: string
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
        const { user: storedUser, token: storedToken } = JSON.parse(stored)
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
      const { token: authToken, user: userData } = data.data

      const user: User = {
        id: userData.id,
        email: userData.email,
        fullName: userData.fullName,
        role: userData.role,
        avatar: userData.avatar,
      }

      setUser(user)
      setToken(authToken)
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ user, token: authToken }))
    } catch (error) {
      if (error instanceof ApiError) {
        throw error
      }
      const mockUsers: Record<string, User> = {
        'admin@payflow.com': {
          id: '1',
          email: 'admin@payflow.com',
          fullName: 'Admin User',
          role: 'ADMIN',
        },
        'john.doe@example.com': {
          id: '3',
          email: 'john.doe@example.com',
          fullName: 'John Doe',
          role: 'USER',
        },
        'jane.smith@example.com': {
          id: '4',
          email: 'jane.smith@example.com',
          fullName: 'Jane Smith',
          role: 'USER',
        },
      }

      const foundUser = mockUsers[email.toLowerCase()]
      if (!foundUser || password !== 'Test@1234') {
        throw new ApiError('Invalid credentials', 401)
      }

      const mockToken = `mock_token_${Date.now()}`
      setUser(foundUser)
      setToken(mockToken)
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ user: foundUser, token: mockToken }))
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
