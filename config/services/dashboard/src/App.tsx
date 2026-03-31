import { Routes, Route, Navigate } from 'react-router-dom'
import ProtectedRoute from './components/common/ProtectedRoute'
import LoginPage from './pages/LoginPage'
import AdminLayout from './components/layouts/AdminLayout'
import UserLayout from './components/layouts/UserLayout'
import AdminDashboard from './pages/admin/Dashboard'
import AdminTransactions from './pages/admin/Transactions'
import AdminTransactionDetail from './pages/admin/TransactionDetail'
import AdminAnalytics from './pages/admin/Analytics'
import UserDashboard from './pages/user/Dashboard'
import UserPayments from './pages/user/Payments'

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      
      <Route
        path="/admin/*"
        element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <AdminLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<AdminDashboard />} />
        <Route path="transactions" element={<AdminTransactions />} />
        <Route path="transactions/:id" element={<AdminTransactionDetail />} />
        <Route path="analytics" element={<AdminAnalytics />} />
      </Route>
      
      <Route
        path="/user/*"
        element={
          <ProtectedRoute allowedRoles={['USER']}>
            <UserLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<UserDashboard />} />
        <Route path="payments" element={<UserPayments />} />
      </Route>
      
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}

export default App
