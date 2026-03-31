import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './store/auth-context'
import { AdminLayout } from './components/layouts/AdminLayout'
import { UserLayout } from './components/layouts/UserLayout'
import { ProtectedRoute } from './components/common/ProtectedRoute'
import { AdminDashboard } from './pages/admin/Dashboard'
import { AdminTransactions } from './pages/admin/Transactions'
import { AdminTransactionDetail } from './pages/admin/TransactionDetail'
import { AdminAnalytics } from './pages/admin/Analytics'
import { AdminOrders } from './pages/admin/Orders'
import { AdminUsers } from './pages/admin/Users'
import { AdminSystemHealth } from './pages/admin/SystemHealth'
import { AdminDebugger } from './pages/admin/Debugger'
import { UserDashboard } from './pages/user/Dashboard'
import { UserPayments } from './pages/user/Payments'
import { UserOrders } from './pages/user/Orders'
import { LoginPage } from './pages/LoginPage'
import { Skeleton } from './components/ui/skeleton'

function App() {
  const { user, loading } = useAuth()

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <Skeleton className="h-12 w-12 rounded-full" />
      </div>
    )
  }

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={user ? <Navigate to={user.role === 'ADMIN' ? '/admin' : '/user'} /> : <LoginPage />} />
        
        <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
          <Route element={<AdminLayout />}>
            <Route path="/admin" element={<AdminDashboard />} />
            <Route path="/admin/transactions" element={<AdminTransactions />} />
            <Route path="/admin/transactions/:id" element={<AdminTransactionDetail />} />
            <Route path="/admin/analytics" element={<AdminAnalytics />} />
            <Route path="/admin/orders" element={<AdminOrders />} />
            <Route path="/admin/users" element={<AdminUsers />} />
            <Route path="/admin/system-health" element={<AdminSystemHealth />} />
            <Route path="/admin/debugger" element={<AdminDebugger />} />
          </Route>
        </Route>

        <Route element={<ProtectedRoute allowedRoles={['USER', 'ADMIN']} />}>
          <Route element={<UserLayout />}>
            <Route path="/user" element={<UserDashboard />} />
            <Route path="/user/payments" element={<UserPayments />} />
            <Route path="/user/orders" element={<UserOrders />} />
          </Route>
        </Route>

        <Route path="/" element={<Navigate to={user ? (user.role === 'ADMIN' ? '/admin' : '/user') : '/login'} />} />
        <Route path="*" element={<Navigate to="/" />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
