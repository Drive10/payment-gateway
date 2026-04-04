import { Outlet, Navigate, useLocation } from 'react-router-dom'
import { useAuth, type UserRole } from '@/store/auth-context'

interface ProtectedRouteProps {
  allowedRoles?: UserRole[]
}

export function ProtectedRoute({ allowedRoles }: ProtectedRouteProps) {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="h-12 w-12 animate-pulse rounded-full bg-muted" />
      </div>
    )
  }

  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  const hasRole = (role: UserRole) => user?.roles?.includes(role) || user?.role === role

  if (allowedRoles && !allowedRoles.some(role => hasRole(role))) {
    return <Navigate to={hasRole('ADMIN') ? '/admin' : '/user'} replace />
  }

  return <Outlet />
}
