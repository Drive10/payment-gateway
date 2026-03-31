import { mockData } from './mock-data'

export interface Transaction {
  id: string
  orderReference: string
  userEmail: string
  amount: number
  currency: string
  status: string
  provider: string
  method: string
  createdAt: string
}

export interface TransactionDetail extends Transaction {
  userName: string
  providerOrderId: string
  providerPaymentId: string | null
  checkoutUrl: string
  transactions: {
    id: string
    type: string
    status: string
    amount: number
    remarks: string
    createdAt: string
  }[]
  errorLogs?: { message: string; timestamp: string }[]
}

export interface DashboardStats {
  totalRevenue: string
  revenueChange: number
  totalTransactions: number
  transactionChange: number
  successRate: number
  successRateChange: number
  failedTransactions: number
  failedChange: number
}

export interface AnalyticsData {
  totalVolume: number
  averageTransaction: number
  successRate: number
  peakHour: number
  successCount: number
  failedCount: number
  pendingCount: number
  refundedCount: number
  revenueTrends: { date: string; revenue: number }[]
  transactionTrends: { date: string; count: number }[]
  providerStats: { name: string; successRate: number }[]
}

class AdminService {
  async getDashboardStats(): Promise<DashboardStats> {
    await new Promise((resolve) => setTimeout(resolve, 300))
    return mockData.dashboardStats
  }

  async getRecentTransactions(limit: number): Promise<Transaction[]> {
    await new Promise((resolve) => setTimeout(resolve, 300))
    return mockData.transactions.slice(0, limit)
  }

  async getTransactions(): Promise<Transaction[]> {
    await new Promise((resolve) => setTimeout(resolve, 500))
    return mockData.transactions
  }

  async getTransactionDetail(id: string): Promise<TransactionDetail> {
    await new Promise((resolve) => setTimeout(resolve, 300))
    const transaction = mockData.transactions.find((t) => t.id === id)
    if (!transaction) {
      throw new Error('Transaction not found')
    }
    return {
      ...transaction,
      userName: 'Test User',
      providerOrderId: `prov_order_${id.slice(0, 8)}`,
      providerPaymentId: transaction.status === 'CAPTURED' ? `prov_pay_${id.slice(0, 8)}` : null,
      checkoutUrl: `https://checkout.payflow.com/pay/${id}`,
      transactions: mockData.transactionHistory,
      errorLogs: transaction.status === 'FAILED' ? [
        { message: 'Payment declined by bank', timestamp: transaction.createdAt }
      ] : undefined
    }
  }

  async getAnalytics(): Promise<AnalyticsData> {
    await new Promise((resolve) => setTimeout(resolve, 500))
    return mockData.analytics
  }
}

export const adminService = new AdminService()
