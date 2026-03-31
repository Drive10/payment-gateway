import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { CreditCard, DollarSign, Clock } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { userService, type UserDashboardData } from '@/services/user.service'
import { formatCurrency, formatDate } from '@/lib/utils'

export function UserDashboard() {
  const [data, setData] = useState<UserDashboardData | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    try {
      const dashboardData = await userService.getDashboard()
      setData(dashboardData)
    } catch (error) {
      console.error('Failed to load dashboard:', error)
    } finally {
      setLoading(false)
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
      default:
        return <Badge variant="secondary">{status}</Badge>
    }
  }

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-32 w-full" />
        <div className="grid gap-4 md:grid-cols-3">
          {[...Array(3)].map((_, i) => (
            <Skeleton key={i} className="h-32" />
          ))}
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Welcome back, {data?.userName}</h1>
        <p className="text-muted-foreground">Here's an overview of your payment activity</p>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">Total Spent</CardTitle>
            <DollarSign className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{formatCurrency(data?.totalSpent || 0)}</div>
            <p className="text-xs text-muted-foreground">Across {data?.totalTransactions || 0} transactions</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">This Month</CardTitle>
            <CreditCard className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{formatCurrency(data?.monthlySpent || 0)}</div>
            <p className="text-xs text-muted-foreground">{data?.monthlyTransactions || 0} transactions</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">Last Payment</CardTitle>
            <Clock className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            {data?.lastPayment ? (
              <>
                <div className="text-2xl font-bold">{formatCurrency(data.lastPayment.amount)}</div>
                <div className="flex items-center gap-2 mt-1">
                  {getStatusBadge(data.lastPayment.status)}
                  <span className="text-xs text-muted-foreground">{formatDate(data.lastPayment.date)}</span>
                </div>
              </>
            ) : (
              <p className="text-muted-foreground">No payments yet</p>
            )}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle>Recent Payments</CardTitle>
          <Button variant="outline" size="sm" asChild>
            <Link to="/user/payments">View all</Link>
          </Button>
        </CardHeader>
        <CardContent>
          {data?.recentPayments && data.recentPayments.length > 0 ? (
            <div className="space-y-4">
              {data.recentPayments.map((payment) => (
                <div key={payment.id} className="flex items-center justify-between p-3 rounded-lg border">
                  <div className="flex items-center gap-4">
                    <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center">
                      <CreditCard className="h-5 w-5 text-primary" />
                    </div>
                    <div>
                      <p className="font-medium">{payment.orderReference}</p>
                      <p className="text-sm text-muted-foreground">{formatDate(payment.date)}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-4">
                    <div className="text-right">
                      <p className="font-medium">{formatCurrency(payment.amount)}</p>
                      {getStatusBadge(payment.status)}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-8">
              <CreditCard className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
              <p className="text-muted-foreground">No payments yet</p>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
