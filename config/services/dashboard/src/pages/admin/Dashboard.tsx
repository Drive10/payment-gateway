import { useState, useEffect } from 'react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  DollarSign,
  ArrowUpRight,
  ArrowDownRight,
  CreditCard,
  TrendingUp,
} from 'lucide-react'
import { getDashboardStats, getRecentTransactions } from '@/services/admin.service'
import type { DashboardStats, Transaction } from '@/services/mock-data'
import { Link } from 'react-router-dom'

export default function AdminDashboard() {
  const [stats, setStats] = useState<DashboardStats | null>(null)
  const [recentTransactions, setRecentTransactions] = useState<Transaction[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function loadData() {
      try {
        const [statsData, transactionsData] = await Promise.all([
          getDashboardStats(),
          getRecentTransactions(5),
        ])
        setStats(statsData)
        setRecentTransactions(transactionsData)
      } finally {
        setLoading(false)
      }
    }
    loadData()
  }, [])

  if (loading) {
    return <div className="animate-pulse space-y-4">
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {[...Array(4)].map((_, i) => (
          <div key={i} className="h-32 bg-muted rounded-lg" />
        ))}
      </div>
    </div>
  }

  const statCards = [
    {
      title: 'Total Revenue',
      value: `$${stats?.totalRevenue.toLocaleString() || '0'}`,
      change: '+12.5%',
      changeType: 'positive' as const,
      icon: DollarSign,
    },
    {
      title: 'Total Transactions',
      value: stats?.totalTransactions.toLocaleString() || '0',
      change: '+8.2%',
      changeType: 'positive' as const,
      icon: CreditCard,
    },
    {
      title: 'Success Rate',
      value: `${stats?.successRate.toFixed(1) || '0'}%`,
      change: '-0.4%',
      changeType: 'negative' as const,
      icon: TrendingUp,
    },
    {
      title: 'Avg. Transaction',
      value: `$${stats?.avgTransactionAmount.toFixed(2) || '0'}`,
      change: '+2.3%',
      changeType: 'positive' as const,
      icon: ArrowUpRight,
    },
  ]

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold tracking-tight">Dashboard</h2>
        <p className="text-muted-foreground">
          Welcome back! Here&apos;s an overview of your payment data.
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {statCards.map((stat) => (
          <Card key={stat.title}>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">{stat.title}</CardTitle>
              <stat.icon className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{stat.value}</div>
              <p className="text-xs text-muted-foreground flex items-center gap-1">
                {stat.changeType === 'positive' ? (
                  <ArrowUpRight className="h-3 w-3 text-green-500" />
                ) : (
                  <ArrowDownRight className="h-3 w-3 text-red-500" />
                )}
                <span className={stat.changeType === 'positive' ? 'text-green-500' : 'text-red-500'}>
                  {stat.change}
                </span>
                {' '}from last month
              </p>
            </CardContent>
          </Card>
        ))}
      </div>

      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Recent Transactions</CardTitle>
            <Link to="/admin/transactions" className="text-sm text-primary hover:underline">
              View all
            </Link>
          </div>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Transaction ID</TableHead>
                <TableHead>Customer</TableHead>
                <TableHead>Amount</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Date</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {recentTransactions.map((tx) => (
                <TableRow key={tx.id}>
                  <TableCell className="font-medium">
                    <Link to={`/admin/transactions/${tx.id}`} className="hover:underline">
                      {tx.id.slice(0, 8)}...
                    </Link>
                  </TableCell>
                  <TableCell>{tx.customerName}</TableCell>
                  <TableCell>${tx.amount.toFixed(2)}</TableCell>
                  <TableCell>
                    <Badge
                      variant={
                        tx.status === 'COMPLETED'
                          ? 'success'
                          : tx.status === 'PENDING'
                          ? 'warning'
                          : 'destructive'
                      }
                    >
                      {tx.status}
                    </Badge>
                  </TableCell>
                  <TableCell>{new Date(tx.createdAt).toLocaleDateString()}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  )
}
