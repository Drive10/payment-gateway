import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { getTransactionById } from '@/services/admin.service'
import type { Transaction } from '@/services/mock-data'
import { ArrowLeft, CheckCircle, XCircle, Clock, AlertCircle } from 'lucide-react'

export default function TransactionDetail() {
  const { id } = useParams<{ id: string }>()
  const [transaction, setTransaction] = useState<Transaction | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function loadTransaction() {
      if (!id) return
      try {
        const data = await getTransactionById(id)
        setTransaction(data)
      } finally {
        setLoading(false)
      }
    }
    loadTransaction()
  }, [id])

  if (loading) {
    return (
      <div className="space-y-4">
        <div className="animate-pulse h-8 w-48 bg-muted rounded" />
        <div className="animate-pulse h-64 bg-muted rounded-lg" />
      </div>
    )
  }

  if (!transaction) {
    return (
      <div className="text-center py-12">
        <AlertCircle className="mx-auto h-12 w-12 text-muted-foreground" />
        <h3 className="mt-4 text-lg font-semibold">Transaction not found</h3>
        <p className="text-muted-foreground">The transaction you&apos;re looking for doesn&apos;t exist.</p>
        <Link to="/admin/transactions">
          <Button className="mt-4">Back to Transactions</Button>
        </Link>
      </div>
    )
  }

  const statusConfig = {
    COMPLETED: { icon: CheckCircle, color: 'text-green-500', bg: 'bg-green-500/10' },
    PENDING: { icon: Clock, color: 'text-yellow-500', bg: 'bg-yellow-500/10' },
    FAILED: { icon: XCircle, color: 'text-red-500', bg: 'bg-red-500/10' },
  }

  const statusInfo = statusConfig[transaction.status as keyof typeof statusConfig]
  const StatusIcon = statusInfo.icon

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Link to="/admin/transactions">
          <Button variant="ghost" size="icon">
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </Link>
        <div>
          <h2 className="text-2xl font-bold tracking-tight">Transaction Details</h2>
          <p className="text-muted-foreground text-sm">{transaction.id}</p>
        </div>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Payment Information</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-muted-foreground">Amount</span>
              <span className="text-2xl font-bold">${transaction.amount.toFixed(2)}</span>
            </div>
            <Separator />
            <div className="flex items-center justify-between">
              <span className="text-muted-foreground">Currency</span>
              <span className="font-medium">{transaction.currency}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-muted-foreground">Gateway</span>
              <span className="font-medium capitalize">{transaction.gateway}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-muted-foreground">Order ID</span>
              <span className="font-medium">{transaction.orderId}</span>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Status</CardTitle>
          </CardHeader>
          <CardContent>
            <div className={`flex items-center gap-3 p-4 rounded-lg ${statusInfo.bg}`}>
              <StatusIcon className={`h-8 w-8 ${statusInfo.color}`} />
              <div>
                <p className="font-semibold text-lg">{transaction.status}</p>
                <p className="text-sm text-muted-foreground">
                  Last updated: {new Date(transaction.updatedAt).toLocaleString()}
                </p>
              </div>
            </div>
            {transaction.errorMessage && (
              <div className="mt-4 p-3 bg-red-500/10 border border-red-500/20 rounded-lg">
                <p className="text-sm font-medium text-red-600 dark:text-red-400">
                  Error: {transaction.errorMessage}
                </p>
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Customer Information</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-muted-foreground">Name</span>
              <span className="font-medium">{transaction.customerName}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-muted-foreground">Email</span>
              <span className="font-medium">{transaction.customerEmail}</span>
            </div>
            {transaction.customerPhone && (
              <div className="flex items-center justify-between">
                <span className="text-muted-foreground">Phone</span>
                <span className="font-medium">{transaction.customerPhone}</span>
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Timeline</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <div className="flex gap-3">
                <div className="flex flex-col items-center">
                  <div className="h-3 w-3 rounded-full bg-green-500" />
                  <div className="h-full w-px bg-border" />
                </div>
                <div>
                  <p className="font-medium">Created</p>
                  <p className="text-sm text-muted-foreground">
                    {new Date(transaction.createdAt).toLocaleString()}
                  </p>
                </div>
              </div>
              {transaction.status !== 'PENDING' && (
                <div className="flex gap-3">
                  <div className={`flex flex-col items-center`}>
                    <div className={`h-3 w-3 rounded-full ${transaction.status === 'COMPLETED' ? 'bg-green-500' : 'bg-red-500'}`} />
                  </div>
                  <div>
                    <p className="font-medium">{transaction.status === 'COMPLETED' ? 'Completed' : 'Failed'}</p>
                    <p className="text-sm text-muted-foreground">
                      {new Date(transaction.updatedAt).toLocaleString()}
                    </p>
                  </div>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Metadata</CardTitle>
          <CardDescription>Additional transaction details</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-2">
            {Object.entries(transaction.metadata || {}).map(([key, value]) => (
              <div key={key} className="flex items-center justify-between">
                <span className="text-muted-foreground capitalize">{key.replace(/([A-Z])/g, ' $1').trim()}</span>
                <span className="font-medium">{String(value)}</span>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
