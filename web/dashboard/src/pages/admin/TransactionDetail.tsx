import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft, Copy, CheckCircle2, XCircle, Clock, AlertTriangle } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Separator } from '@/components/ui/separator'
import { adminService, type TransactionDetail } from '@/services/admin.service'
import { formatCurrency, formatDateTime } from '@/lib/utils'
import { useToast } from '@/hooks/use-toast'

export function AdminTransactionDetail() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [transaction, setTransaction] = useState<TransactionDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const { toast } = useToast()

  useEffect(() => {
    if (id) {
      loadTransaction(id)
    }
  }, [id])

  const loadTransaction = async (transactionId: string) => {
    try {
      const data = await adminService.getTransactionDetail(transactionId)
      setTransaction(data)
    } catch (error) {
      console.error('Failed to load transaction:', error)
    } finally {
      setLoading(false)
    }
  }

  const copyToClipboard = (text: string, label: string) => {
    navigator.clipboard.writeText(text)
    toast({ title: `${label} copied to clipboard` })
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
      case 'COMPLETED':
        return <Badge variant="success">Success</Badge>
      case 'FAILED':
        return <Badge variant="error">Failed</Badge>
      case 'PENDING':
      case 'CREATED':
        return <Badge variant="warning">Pending</Badge>
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

  if (!transaction) {
    return (
      <div className="flex flex-col items-center justify-center py-12">
        <p className="text-muted-foreground">Transaction not found</p>
        <Button variant="outline" className="mt-4" onClick={() => navigate('/admin/transactions')}>
          Back to Transactions
        </Button>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={() => navigate('/admin/transactions')}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Transaction Details</h1>
          <p className="text-muted-foreground">{transaction.orderReference}</p>
        </div>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Payment Information</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex justify-between items-center">
              <span className="text-muted-foreground">Status</span>
              <div className="flex items-center gap-2">
                {getStatusIcon(transaction.status)}
                {getStatusBadge(transaction.status)}
              </div>
            </div>
            <Separator />
            <div className="flex justify-between items-center">
              <span className="text-muted-foreground">Amount</span>
              <span className="text-2xl font-bold">
                {formatCurrency(transaction.amount, transaction.currency)}
              </span>
            </div>
            <Separator />
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm text-muted-foreground">Provider</p>
                <p className="font-medium">{transaction.provider}</p>
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Method</p>
                <p className="font-medium">{transaction.method}</p>
              </div>
            </div>
            <Separator />
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm text-muted-foreground">Created</p>
                <p className="font-medium">{formatDateTime(transaction.createdAt)}</p>
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Updated</p>
                <p className="font-medium">{formatDateTime(transaction.updatedAt || transaction.createdAt)}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Identifiers</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <p className="text-sm text-muted-foreground">Transaction ID</p>
              <div className="flex items-center gap-2">
                <p className="font-mono text-sm">{transaction.id}</p>
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-6 w-6"
                  onClick={() => copyToClipboard(transaction.id, 'Transaction ID')}
                >
                  <Copy className="h-3 w-3" />
                </Button>
              </div>
            </div>
            <Separator />
            <div>
              <p className="text-sm text-muted-foreground">Provider Order ID</p>
              <div className="flex items-center gap-2">
                <p className="font-mono text-sm">{transaction.providerOrderId}</p>
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-6 w-6"
                  onClick={() => copyToClipboard(transaction.providerOrderId, 'Provider Order ID')}
                >
                  <Copy className="h-3 w-3" />
                </Button>
              </div>
            </div>
            {transaction.providerPaymentId && (
              <>
                <Separator />
                <div>
                  <p className="text-sm text-muted-foreground">Provider Payment ID</p>
                  <div className="flex items-center gap-2">
                    <p className="font-mono text-sm">{transaction.providerPaymentId}</p>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-6 w-6"
                      onClick={() => copyToClipboard(transaction.providerPaymentId || '', 'Provider Payment ID')}
                    >
                      <Copy className="h-3 w-3" />
                    </Button>
                  </div>
                </div>
              </>
            )}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Transaction History</CardTitle>
          <CardDescription>Timeline of all events for this transaction</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {transaction.transactions.map((tx, index) => (
              <div key={tx.id} className="flex gap-4">
                <div className="flex flex-col items-center">
                  <div className={`w-3 h-3 rounded-full ${
                    tx.status === 'SUCCESS' ? 'bg-green-500' : 
                    tx.status === 'FAILED' ? 'bg-red-500' : 'bg-yellow-500'
                  }`} />
                  {index < transaction.transactions.length - 1 && (
                    <div className="w-0.5 h-full bg-border mt-1" />
                  )}
                </div>
                <div className="flex-1 pb-4">
                  <p className="font-medium">{tx.type.replace('_', ' ')}</p>
                  <p className="text-sm text-muted-foreground">{tx.remarks}</p>
                  <p className="text-xs text-muted-foreground mt-1">
                    {formatDateTime(tx.createdAt)}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {transaction.errorLogs && transaction.errorLogs.length > 0 && (
        <Card className="border-red-200 dark:border-red-900">
          <CardHeader>
            <CardTitle className="text-red-600">Error Logs</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-2">
              {transaction.errorLogs.map((log, index) => (
                <div key={index} className="p-3 bg-red-50 dark:bg-red-900/20 rounded-lg">
                  <p className="font-mono text-sm">{log.message}</p>
                  <p className="text-xs text-muted-foreground mt-1">{formatDateTime(log.timestamp)}</p>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
