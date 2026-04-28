import { useState, useEffect } from 'react'
import { useParams, useLocation } from 'react-router-dom'

export default function PaymentPage() {
  const { paymentId } = useParams()
  const location = useLocation()
  const [payment, setPayment] = useState(location.state?.payment || null)
  const [status, setStatus] = useState('CREATED')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!paymentId) return

    const fetchPayment = async () => {
      try {
        const res = await fetch(`/api/v1/payments/${paymentId}`)
        const data = await res.json()
        
        if (data.success) {
          setPayment(data.data)
          setStatus(data.data.status)
        } else {
          setError(data.error?.message || 'Payment not found')
        }
      } catch (err) {
        setError('Failed to fetch payment')
      } finally {
        setLoading(false)
      }
    }

    fetchPayment()
    
    // Poll for status updates
    const interval = setInterval(fetchPayment, 3000)
    return () => clearInterval(interval)
  }, [paymentId])

  const getStatusColor = (s) => {
    switch(s) {
      case 'CAPTURED': return 'bg-green-100 text-green-800'
      case 'AUTHORIZED': return 'bg-blue-100 text-blue-800'
      case 'FAILED': case 'EXPIRED': return 'bg-red-100 text-red-800'
      case 'CREATED': return 'bg-yellow-100 text-yellow-800'
      default: return 'bg-gray-100 text-gray-800'
    }
  }

  const getStatusIcon = (s) => {
    switch(s) {
      case 'CAPTURED': return '✓'
      case 'AUTHORIZED': return '✓'
      case 'FAILED': case 'EXPIRED': return '✕'
      default: return '⟳'
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading payment details...</p>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="bg-white rounded-lg shadow-md p-8 max-w-md w-full">
          <div className="text-red-600 text-center">{error}</div>
        </div>
      </div>
    )
  }

  const isCompleted = status === 'CAPTURED' || status === 'AUTHORIZED'
  const isPending = status === 'CREATED' || status === 'AUTHORIZATION_PENDING'
  const isFailed = status === 'FAILED' || status === 'EXPIRED'

  return (
    <div className="min-h-screen bg-gray-50 py-8 px-4">
      <div className="max-w-lg mx-auto">
        {/* Header */}
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-gray-900">Payment</h1>
        </div>

        {/* Status Card */}
        <div className={`rounded-lg shadow-md p-6 mb-6 ${getStatusColor(status)}`}>
          <div className="flex items-center justify-center">
            <span className="text-4xl mr-3">{getStatusIcon(status)}</span>
            <span className="text-xl font-semibold">
              {status === 'CAPTURED' && 'Payment Successful'}
              {status === 'AUTHORIZED' && 'Payment Authorized'}
              {status === 'CREATED' && 'Awaiting Payment'}
              {status === 'AUTHORIZATION_PENDING' && 'Processing'}
              {status === 'FAILED' && 'Payment Failed'}
              {status === 'EXPIRED' && 'Payment Expired'}
              {status === 'REFUNDED' && 'Payment Refunded'}
            </span>
          </div>
        </div>

        {/* Payment Details Card */}
        <div className="bg-white rounded-lg shadow-md p-6">
          <h2 className="text-lg font-semibold text-gray-800 mb-4">Payment Details</h2>
          
          <div className="space-y-4">
            <div className="flex justify-between py-2 border-b">
              <span className="text-gray-600">Payment ID</span>
              <span className="font-mono text-sm text-gray-800">{payment?.paymentId}</span>
            </div>
            
            <div className="flex justify-between py-2 border-b">
              <span className="text-gray-600">Order ID</span>
              <span className="font-medium text-gray-800">{payment?.orderId || '-'}</span>
            </div>
            
            <div className="flex justify-between py-2 border-b">
              <span className="text-gray-600">Amount</span>
              <span className="font-bold text-2xl text-gray-800">
                {payment?.amount} {payment?.currency}
              </span>
            </div>
            
            <div className="flex justify-between py-2 border-b">
              <span className="text-gray-600">Status</span>
              <span className={`px-3 py-1 rounded-full text-sm font-medium ${getStatusColor(status)}`}>
                {payment?.status}
              </span>
            </div>

            {payment?.failureReason && (
              <div className="flex justify-between py-2 border-b">
                <span className="text-gray-600">Failure Reason</span>
                <span className="text-red-600">{payment.failureReason}</span>
              </div>
            )}
          </div>
        </div>

        {/* Action Buttons */}
        {isPending && (
          <div className="mt-6 space-y-3">
            <button 
              className="w-full bg-blue-600 text-white py-3 px-4 rounded-lg font-medium hover:bg-blue-700 transition"
              onClick={() => window.location.reload()}
            >
              Refresh Status
            </button>
            <p className="text-center text-sm text-gray-500">
              Please complete the payment to continue
            </p>
          </div>
        )}

        {isCompleted && (
          <div className="mt-6">
            <button 
              className="w-full bg-green-600 text-white py-3 px-4 rounded-lg font-medium hover:bg-green-700 transition"
              onClick={() => window.location.href = '/'}
            >
              Create New Payment
            </button>
          </div>
        )}

        {isFailed && (
          <div className="mt-6 space-y-3">
            <button 
              className="w-full bg-blue-600 text-white py-3 px-4 rounded-lg font-medium hover:bg-blue-700 transition"
              onClick={() => window.location.href = '/'}
            >
              Try Again
            </button>
          </div>
        )}
      </div>
    </div>
  )
}