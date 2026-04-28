import { useState } from 'react'
import { useNavigate, Routes, Route } from 'react-router-dom'
import PaymentPage from './Payment'

const API_BASE = '/api/v1'

function Login() {
  const navigate = useNavigate()
  const [step, setStep] = useState(1) // 1: login, 2: create payment
  const [merchantId, setMerchantId] = useState('')
  const [apiKey, setApiKey] = useState('')
  const [amount, setAmount] = useState('')
  const [currency, setCurrency] = useState('INR')
  const [orderId, setOrderId] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [orders, setOrders] = useState([])
  const [token, setToken] = useState('')

  const handleLogin = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')

    try {
      const loginRes = await fetch(`${API_BASE}/merchant/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: merchantId, password: apiKey })
      })
      const loginData = await loginRes.json()
      
      if (!loginData.success || !loginData.data?.accessToken) {
        throw new Error(loginData.error?.message || 'Login failed')
      }

      const token = loginData.data.accessToken
      setToken(token)

      // Fetch merchant orders
      const ordersRes = await fetch(`${API_BASE}/payments/orders`, {
        headers: { 'Authorization': `Bearer ${token}` }
      })
      const ordersData = await ordersRes.json()
      
      if (ordersData.success) {
        setOrders(ordersData.data || [])
      }

      setStep(2)
    } catch (err) {
      setError(err.message || 'Something went wrong')
    } finally {
      setLoading(false)
    }
  }

  const handleCreatePayment = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')

    try {
      const orderRes = await fetch(`${API_BASE}/payments/create-order`, {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          orderId: orderId || `ORD-${Date.now()}`,
          amount: parseFloat(amount),
          currency
        })
      })
      const orderData = await orderRes.json()

      if (!orderData.success) {
        throw new Error(orderData.error?.message || 'Order creation failed')
      }

      navigate(`/pay/${orderData.data.paymentId}`, { 
        state: { payment: orderData.data, token } 
      })
    } catch (err) {
      setError(err.message || 'Something went wrong')
    } finally {
      setLoading(false)
    }
  }

  const formatDate = (dateStr) => {
    if (!dateStr) return '-'
    return new Date(dateStr).toLocaleString()
  }

  const getStatusColor = (s) => {
    switch(s) {
      case 'CAPTURED': return 'bg-green-100 text-green-800'
      case 'AUTHORIZED': return 'bg-blue-100 text-blue-800'
      case 'FAILED': return 'bg-red-100 text-red-800'
      default: return 'bg-yellow-100 text-yellow-800'
    }
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8 px-4">
      <div className="max-w-2xl mx-auto">
        <h1 className="text-3xl font-bold text-center text-gray-900 mb-8">Payment Dashboard</h1>
        
        {step === 1 && (
          <div className="bg-white rounded-lg shadow-md p-8">
            <h2 className="text-xl font-semibold mb-6">Merchant Login</h2>
            <form onSubmit={handleLogin} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
                <input
                  type="text"
                  value={merchantId}
                  onChange={(e) => setMerchantId(e.target.value)}
                  placeholder="merchant@example.com"
                  className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
                <input
                  type="password"
                  value={apiKey}
                  onChange={(e) => setApiKey(e.target.value)}
                  className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  required
                />
              </div>
              {error && <div className="text-red-600 text-sm">{error}</div>}
              <button
                type="submit"
                disabled={loading}
                className="w-full bg-blue-600 text-white py-3 px-4 rounded-lg font-medium hover:bg-blue-700 disabled:bg-gray-400 transition"
              >
                {loading ? 'Logging in...' : 'Login'}
              </button>
            </form>
          </div>
        )}

        {step === 2 && (
          <div className="space-y-6">
            {/* Create New Payment */}
            <div className="bg-white rounded-lg shadow-md p-8">
              <h2 className="text-xl font-semibold mb-6">Create New Payment</h2>
              <form onSubmit={handleCreatePayment} className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Amount</label>
                    <input
                      type="number"
                      value={amount}
                      onChange={(e) => setAmount(e.target.value)}
                      placeholder="1000"
                      className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Currency</label>
                    <select
                      value={currency}
                      onChange={(e) => setCurrency(e.target.value)}
                      className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500"
                    >
                      <option value="INR">INR</option>
                      <option value="USD">USD</option>
                      <option value="EUR">EUR</option>
                      <option value="GBP">GBP</option>
                    </select>
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Order ID (optional)</label>
                  <input
                    type="text"
                    value={orderId}
                    onChange={(e) => setOrderId(e.target.value)}
                    placeholder="ORD-12345"
                    className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                {error && <div className="text-red-600 text-sm">{error}</div>}
                <button
                  type="submit"
                  disabled={loading}
                  className="w-full bg-blue-600 text-white py-3 px-4 rounded-lg font-medium hover:bg-blue-700 disabled:bg-gray-400 transition"
                >
                  {loading ? 'Creating...' : 'Create Payment'}
                </button>
              </form>
            </div>

            {/* Recent Orders */}
            <div className="bg-white rounded-lg shadow-md p-8">
              <h2 className="text-xl font-semibold mb-4">Recent Payments</h2>
              {orders.length === 0 ? (
                <p className="text-gray-500 text-center py-4">No payments yet</p>
              ) : (
                <div className="space-y-3">
                  {orders.map((order) => (
                    <div 
                      key={order.paymentId} 
                      className="flex items-center justify-between p-4 border rounded-lg hover:bg-gray-50 cursor-pointer"
                      onClick={() => navigate(`/pay/${order.paymentId}`, { state: { payment: order, token } })}
                    >
                      <div>
                        <p className="font-medium">{order.orderId || order.paymentId}</p>
                        <p className="text-sm text-gray-500">{formatDate(order.createdAt)}</p>
                      </div>
                      <div className="text-right">
                        <p className="font-bold">{order.amount} {order.currency}</p>
                        <span className={`px-2 py-1 rounded text-xs ${getStatusColor(order.status)}`}>
                          {order.status}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Login />} />
      <Route path="/pay/:paymentId" element={<PaymentPage />} />
    </Routes>
  )
}