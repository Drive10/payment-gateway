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
  CreditCard,
  TrendingUp,
} from 'lucide-react'
import { getUserDashboardStats, getUserTransactions } from '@/services/user.service'
import type { UserStats, Transaction } from '@/services/mock-data'

export default function UserDashboard() {
  const [stats, setStats] = useState<UserStats | null>(null)
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function loadData() {
      try {
        const [statsData, txData] = await Promise.all([
          getUserDashboardStats(),
          getUserTransactions(5),
        ])
        setStats(statsData)
        setTransactions(txData)
      } finally {
        setLoading(false)
      }
    }
    loadData()
  }, [])

  if (loading) {
    return <div className="animate-pulse space-y-4">
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {[...Array(3)].map((_, i) => (
          <div key={i} className="h-32 bg-muted rounded-lg" />
        ))}
      </div>
    </div>
  }

  const statCards = [
    {
      title: 'Total Spent',
      value: `$${stats?.totalSpent.toLocaleString() || '0'}`,
      icon: DollarSign,
    },
    {
      title: 'Total Payments',
      value: stats?.totalPayments.toLocaleString() || '0',
      icon: CreditCard,
    },
    {
      title: 'Success Rate',
      value: `${stats?.successRate.toFixed(1) || '0'}%`,
      icon: TrendingUp,
    },
  ]

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold tracking-tight">My Overview</h2>
        <p className="text-muted-foreground">
          Track your payment history and spending.
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {statCards.map((stat) => (
          <Card key={stat.title}>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">{stat.title}</CardTitle>
              <stat.icon className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{stat.value}</div>
            </CardContent>
          </Card>
        ))}
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Recent Payments</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ID</TableHead>
                <TableHead>Amount</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Date</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {transactions.map((tx) => (
                <TableRow key={tx.id}>
                  <TableCell className="font-medium">{tx.id.slice(0, 8)}...</TableCell>
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
