import { mockTransactions, type Transaction, type DashboardStats } from './mock-data'

const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

export async function getDashboardStats(): Promise<DashboardStats> {
  await delay(300)
  
  const completed = mockTransactions.filter((tx) => tx.status === 'COMPLETED')
  const totalRevenue = completed.reduce((sum, tx) => sum + tx.amount, 0)
  const totalTransactions = mockTransactions.length
  const successRate = (completed.length / totalTransactions) * 100
  const avgTransactionAmount = totalRevenue / completed.length

  return {
    totalRevenue,
    totalTransactions,
    successRate,
    avgTransactionAmount,
  }
}

export async function getRecentTransactions(limit: number = 5): Promise<Transaction[]> {
  await delay(200)
  return mockTransactions.slice(0, limit)
}

export async function getTransactions(): Promise<Transaction[]> {
  await delay(300)
  return mockTransactions
}

export async function getTransactionById(id: string): Promise<Transaction | null> {
  await delay(200)
  return mockTransactions.find((tx) => tx.id === id) || null
}

export async function getAnalyticsData() {
  await delay(400)
  
  const revenueByMonth = [
    { month: 'Oct', revenue: 45000 },
    { month: 'Nov', revenue: 52000 },
    { month: 'Dec', revenue: 48000 },
    { month: 'Jan', revenue: 61000 },
    { month: 'Feb', revenue: 55000 },
    { month: 'Mar', revenue: 67000 },
  ]

  const gatewayCounts = mockTransactions.reduce((acc, tx) => {
    acc[tx.gateway] = (acc[tx.gateway] || 0) + 1
    return acc
  }, {} as Record<string, number>)

  const transactionsByGateway = Object.entries(gatewayCounts).map(([name, value]) => ({
    name: name.charAt(0).toUpperCase() + name.slice(1),
    value,
  }))

  const statusCounts = mockTransactions.reduce((acc, tx) => {
    acc[tx.status] = (acc[tx.status] || 0) + 1
    return acc
  }, {} as Record<string, number>)

  const transactionsByStatus = Object.entries(statusCounts).map(([name, value]) => ({
    name,
    value,
  }))

  const customerSpending = mockTransactions.reduce((acc, tx) => {
    if (!acc[tx.customerName]) {
      acc[tx.customerName] = { totalSpent: 0, transactionCount: 0 }
    }
    if (tx.status === 'COMPLETED') {
      acc[tx.customerName].totalSpent += tx.amount
    }
    acc[tx.customerName].transactionCount += 1
    return acc
  }, {} as Record<string, { totalSpent: number; transactionCount: number }>)

  const topCustomers = Object.entries(customerSpending)
    .map(([name, data]) => ({ name, ...data }))
    .sort((a, b) => b.totalSpent - a.totalSpent)
    .slice(0, 5)

  return {
    revenueByMonth,
    transactionsByGateway,
    transactionsByStatus,
    topCustomers,
  }
}

export function exportToCSV(headers: string[], rows: string[][], filename: string) {
  const csvContent = [
    headers.join(','),
    ...rows.map((row) => row.map((cell) => `"${cell}"`).join(',')),
  ].join('\n')

  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = filename
  link.click()
}
