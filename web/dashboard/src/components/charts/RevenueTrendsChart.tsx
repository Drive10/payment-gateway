import { useEffect, useState } from 'react'
import { TrendingUp } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { merchantPaymentService, type PaymentTrend } from '@/services/payment.service'
import { useAuth } from '@/store/auth-context'
import { formatCurrency } from '@/lib/utils'

export function RevenueTrendsChart() {
  const [trends, setTrends] = useState<PaymentTrend[]>([])
  const [loading, setLoading] = useState(true)
  const { user } = useAuth()

  useEffect(() => {
    if (user?.merchantId) {
      loadTrends()
    }
  }, [user?.merchantId])

  const loadTrends = async () => {
    if (!user?.merchantId) return
    try {
      const data = await merchantPaymentService.getPaymentTrends(user.merchantId, 7)
      setTrends(data)
    } catch (error) {
      console.error('Failed to load trends:', error)
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-sm">Revenue Trend (7 Days)</CardTitle>
        </CardHeader>
        <CardContent>
          <Skeleton className="h-40 w-full" />
        </CardContent>
      </Card>
    )
  }

  if (!trends.length) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-sm">Revenue Trend (7 Days)</CardTitle>
        </CardHeader>
        <CardContent className="h-40 flex items-center justify-center">
          <p className="text-muted-foreground text-sm">No data available</p>
        </CardContent>
      </Card>
    )
  }

  const maxRevenue = Math.max(...trends.map(t => t.revenue), 1)
  const heights = trends.map(t => (t.revenue / maxRevenue) * 100)

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between pb-2">
        <CardTitle className="text-sm font-medium">Revenue Trend (7 Days)</CardTitle>
        <TrendingUp className="h-4 w-4 text-muted-foreground" />
      </CardHeader>
      <CardContent>
        <div className="flex items-end justify-between h-32 gap-2">
          {trends.map((trend, i) => (
            <div key={trend.date} className="flex flex-col items-center flex-1">
              <div
                className="w-full bg-gradient-to-t from-cyan-600 to-cyan-400 rounded-t transition-all hover:from-cyan-700 hover:to-cyan-500"
                style={{ height: `${Math.max(heights[i], 4)}%` }}
                title={`${formatCurrency(trend.revenue)} - ${trend.count} payments`}
              />
              <span className="text-xs text-muted-foreground mt-2">
                {new Date(trend.date).toLocaleDateString('en-IN', { weekday: 'short' })}
              </span>
            </div>
          ))}
        </div>
        <div className="mt-4 flex justify-between text-xs text-muted-foreground">
          <span>7 days ago</span>
          <span>Today</span>
        </div>
      </CardContent>
    </Card>
  )
}
