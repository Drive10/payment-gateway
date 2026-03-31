import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '@/store/auth-context'

interface ProtectedRouteProps {
  children: React.ReactNode
  allowedRoles: ('ADMIN' | 'USER')[]
}

export default function ProtectedRoute({ children, allowedRoles }: ProtectedRouteProps) {
  const { user, isAuthenticated } = useAuth()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  if (user && !allowedRoles.includes(user.role)) {
    const defaultPath = user.role === 'ADMIN' ? '/admin' : '/user'
    return <Navigate to={defaultPath} replace />
  }

  return <>{children}</>
}
