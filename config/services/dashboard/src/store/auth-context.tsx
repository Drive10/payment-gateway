import { createContext, useContext, useState, useCallback, type ReactNode } from 'react'

interface User {
  id: string
  email: string
  name: string
  role: 'ADMIN' | 'USER'
  avatar?: string
}

interface AuthContextType {
  user: User | null
  isAuthenticated: boolean
  login: (email: string, password: string) => Promise<boolean>
  logout: () => void
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

const MOCK_USERS: Record<string, { password: string; user: User }> = {
  'admin@payflow.com': {
    password: 'Test@1234',
    user: {
      id: '1',
      email: 'admin@payflow.com',
      name: 'Admin User',
      role: 'ADMIN',
      avatar: 'AU',
    },
  },
  'john.doe@example.com': {
    password: 'Test@1234',
    user: {
      id: '2',
      email: 'john.doe@example.com',
      name: 'John Doe',
      role: 'USER',
      avatar: 'JD',
    },
  },
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(() => {
    const stored = localStorage.getItem('user')
    return stored ? JSON.parse(stored) : null
  })

  const login = useCallback(async (email: string, password: string): Promise<boolean> => {
    await new Promise((resolve) => setTimeout(resolve, 500))
    
    const mockUser = MOCK_USERS[email.toLowerCase()]
    if (mockUser && mockUser.password === password) {
      setUser(mockUser.user)
      localStorage.setItem('user', JSON.stringify(mockUser.user))
      return true
    }
    return false
  }, [])

  const logout = useCallback(() => {
    setUser(null)
    localStorage.removeItem('user')
  }, [])

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        login,
        logout,
      }}
    >
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
