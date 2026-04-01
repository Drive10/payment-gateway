import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { ArrowLeft, Copy, CheckCircle2, XCircle, Clock, AlertTriangle } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Separator } from '@/components/ui/separator'
import { api } from '@/services/api/client'
import { formatCurrency, formatDateTime } from '@/lib/utils'
import { useToast } from '@/hooks/useToast'

interface PaymentDetail {
  id: string
  orderId: string
  orderReference: string
  merchantId: string
  amount: number
  currency: string
  status: string
  provider: string
  method: string
  transactionMode: string
  
  platformFee: number
  gatewayFee: number
  totalFee: number
  netAmount: number
  
  providerOrderId: string
  providerPaymentId: string | null
  providerSignature: string | null
  simulated: boolean
  
  createdAt: string
  updatedAt: string
  capturedAt: string | null
  
  refundedAmount: number
  refundCount: number
  refunds: RefundDetail[]
  
  timeline: TransactionEvent[]
  
  notes: string | null
}

interface RefundDetail {
  id: string
  refundReference: string
  amount: number
  status: string
  reason: string | null
  createdAt: string
}

interface TransactionEvent {
  id: string
  type: string
  status: string
  amount: number
  remarks: string
  createdAt: string
}

export function PaymentDetail() {
  const { id } = useParams<{ id: string }>()
  const [payment, setPayment] = useState<PaymentDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const { addToast } = useToast()

  useEffect(() => {
    if (id) {
      loadPayment(id)
    }
  }, [id])

  const loadPayment = async (paymentId: string) => {
    try {
      const response = await api.get<{ data: PaymentDetail }>(`/payments/${paymentId}/detail`)
      setPayment(response.data)
    } catch (error) {
      console.error('Failed to load payment:', error)
      addToast('error', 'Failed to load payment', 'Please try again')
    } finally {
      setLoading(false)
    }
  }

  const copyToClipboard = (text: string, label: string) => {
    navigator.clipboard.writeText(text)
    addToast('success', `${label} copied`, 'Copied to clipboard')
  }

  const getStatusIcon = (status: string) => {
    switch (status.toUpperCase()) {
      case 'SUCCESS':
      case 'CAPTURED':
        return <CheckCircle2 className="h-5 w-5 text-green-500" />
      case 'FAILED':
        return <XCircle className="h-5 w-5 text-red-500" />
      case 'PENDING':
      case 'CREATED':
        return <Clock className="h-5 w-5 text-yellow-500" />
      default:
        return <AlertTriangle className="h-5 w-5 text-gray-500" />
    }
  }

  const getStatusBadge = (status: string) => {
    switch (status.toUpperCase()) {
      case 'CAPTURED':
      case 'SUCCESS':
        return <Badge variant="success">Success</Badge>
      case 'FAILED':
        return <Badge variant="error">Failed</Badge>
      case 'PENDING':
      case 'CREATED':
        return <Badge variant="warning">Pending</Badge>
      case 'REFUNDED':
        return <Badge variant="outline">Refunded</Badge>
      default:
        return <Badge variant="secondary">{status}</Badge>
    }
  }

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-10 w-32" />
        <Skeleton className="h-64 w-full" />
        <Skeleton className="h-64 w-full" />
      </div>
    )
  }

  if (!payment) {
    return (
      <div className="flex flex-col items-center justify-center py-12">
        <p className="text-muted-foreground">Payment not found</p>
        <Button variant="outline" className="mt-4" asChild>
          <Link to="/user/payments">Back to Payments</Link>
        </Button>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" asChild>
          <Link to="/user/payments">
            <ArrowLeft className="h-4 w-4" />
          </Link>
        </Button>
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Payment Details</h1>
          <p className="text-muted-foreground">{payment.orderReference}</p>
        </div>
        {payment.simulated && (
          <Badge variant="outline" className="ml-auto">Test Payment</Badge>
        )}
      </div>

      {/* Status Card */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            Payment Status
            {getStatusIcon(payment.status)}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center justify-between">
            {getStatusBadge(payment.status)}
            <p className="text-sm text-muted-foreground">
              Created: {formatDateTime(payment.createdAt)}
            </p>
          </div>
        </CardContent>
      </Card>

      <div className="grid gap-6 md:grid-cols-2">
        {/* Amount & Fees */}
        <Card>
          <CardHeader>
            <CardTitle>Amount Breakdown</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex justify-between items-center">
              <span className="text-muted-foreground">Gross Amount</span>
              <span className="text-xl font-bold">{formatCurrency(payment.amount, payment.currency)}</span>
            </div>
            <Separator />
            <div className="flex justify-between items-center text-sm">
              <span className="text-muted-foreground">Platform Fee</span>
              <span className="text-red-600">-{formatCurrency(payment.platformFee, payment.currency)}</span>
            </div>
            <div className="flex justify-between items-center text-sm">
              <span className="text-muted-foreground">Gateway Fee</span>
              <span className="text-red-600">-{formatCurrency(payment.gatewayFee, payment.currency)}</span>
            </div>
            <Separator />
            <div className="flex justify-between items-center">
              <span className="font-medium">Net Amount</span>
              <span className="text-xl font-bold text-green-600">
                {formatCurrency(payment.netAmount, payment.currency)}
              </span>
            </div>
          </CardContent>
        </Card>

        {/* Payment Info */}
        <Card>
          <CardHeader>
            <CardTitle>Payment Information</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm text-muted-foreground">Provider</p>
                <p className="font-medium">{payment.provider}</p>
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Method</p>
                <p className="font-medium">{payment.method}</p>
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Mode</p>
                <p className="font-medium">{payment.transactionMode}</p>
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Currency</p>
                <p className="font-medium">{payment.currency}</p>
              </div>
            </div>
            <Separator />
            <div>
              <p className="text-sm text-muted-foreground">Provider Order ID</p>
              <div className="flex items-center gap-2">
                <p className="font-mono text-sm">{payment.providerOrderId}</p>
                <Button variant="ghost" size="icon" className="h-6 w-6" onClick={() => copyToClipboard(payment.providerOrderId, 'Provider Order ID')}>
                  <Copy className="h-3 w-3" />
                </Button>
              </div>
            </div>
            {payment.providerPaymentId && (
              <div>
                <p className="text-sm text-muted-foreground">Provider Payment ID</p>
                <div className="flex items-center gap-2">
                  <p className="font-mono text-sm">{payment.providerPaymentId}</p>
                  <Button variant="ghost" size="icon" className="h-6 w-6" onClick={() => copyToClipboard(payment.providerPaymentId || '', 'Provider Payment ID')}>
                    <Copy className="h-3 w-3" />
                  </Button>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Timeline */}
      <Card>
        <CardHeader>
          <CardTitle>Transaction Timeline</CardTitle>
          <CardDescription>Full lifecycle of this payment</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {payment.timeline && payment.timeline.length > 0 ? (
              payment.timeline.map((event, index) => (
                <div key={event.id} className="flex gap-4">
                  <div className="flex flex-col items-center">
                    <div className={`w-3 h-3 rounded-full ${
                      event.status === 'SUCCESS' ? 'bg-green-500' : 
                      event.status === 'FAILED' ? 'bg-red-500' : 'bg-yellow-500'
                    }`} />
                    {index < payment.timeline.length - 1 && (
                      <div className="w-0.5 h-full bg-border mt-1" />
                    )}
                  </div>
                  <div className="flex-1 pb-4">
                    <p className="font-medium">{event.type.replace(/_/g, ' ')}</p>
                    <p className="text-sm text-muted-foreground">{event.remarks}</p>
                    <p className="text-xs text-muted-foreground mt-1">
                      {formatDateTime(event.createdAt)}
                    </p>
                  </div>
                </div>
              ))
            ) : (
              <p className="text-muted-foreground text-sm">No transaction history available</p>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Refunds */}
      {payment.refunds && payment.refunds.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Refunds</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {payment.refunds.map((refund) => (
                <div key={refund.id} className="flex items-center justify-between p-3 rounded-lg border">
                  <div>
                    <p className="font-medium">{refund.refundReference}</p>
                    <p className="text-sm text-muted-foreground">{formatDateTime(refund.createdAt)}</p>
                  </div>
                  <div className="text-right">
                    <p className="font-medium text-red-600">{formatCurrency(refund.amount, payment.currency)}</p>
                    {getStatusBadge(refund.status)}
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
