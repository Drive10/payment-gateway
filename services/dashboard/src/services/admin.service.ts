import { api } from './api/client'
import { mockData } from './mock-data'
import type {
  Transaction,
  TransactionDetail,
  DashboardStats,
  AnalyticsData,
  OrderAnalytics,
  Order,
} from './api/types'

export type {
  Transaction,
  TransactionDetail,
  DashboardStats,
  AnalyticsData,
  OrderAnalytics,
  Order,
}

const USE_MOCK = false

class AdminService {
  async getDashboardStats(): Promise<DashboardStats> {
    if (USE_MOCK) {
      await new Promise((resolve) => setTimeout(resolve, 300))
      return mockData.dashboardStats
    }

    const response = await api.get<{ data: DashboardStats }>('/admin/dashboard/stats')
    return response.data
  }

  async getRecentTransactions(limit: number): Promise<Transaction[]> {
    if (USE_MOCK) {
      await new Promise((resolve) => setTimeout(resolve, 300))
      return mockData.transactions.slice(0, limit)
    }

    const response = await api.get<{ data: Transaction[] }>(`/transactions?limit=${limit}`)
    return response.data
  }

  async getTransactions(): Promise<Transaction[]> {
    if (USE_MOCK) {
      await new Promise((resolve) => setTimeout(resolve, 500))
      return mockData.transactions
    }

    const response = await api.get<{ data: Transaction[] }>('/payments')
    return response.data
  }

  async getTransactionDetail(id: string): Promise<TransactionDetail> {
    if (USE_MOCK) {
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

    const response = await api.get<{ data: TransactionDetail }>(`/payments/${id}`)
    return response.data
  }

  async getAnalytics(): Promise<AnalyticsData> {
    if (USE_MOCK) {
      await new Promise((resolve) => setTimeout(resolve, 500))
      return mockData.analytics
    }

    const response = await api.get<{ data: AnalyticsData }>('/analytics')
    return response.data
  }

  async getOrders(): Promise<Order[]> {
    if (USE_MOCK) {
      await new Promise((resolve) => setTimeout(resolve, 500))
      return mockData.transactions.map((t) => ({
        id: t.id,
        orderReference: t.orderReference,
        userId: 'user-1',
        userEmail: t.userEmail,
        amount: t.amount,
        currency: t.currency,
        status: t.status === 'CAPTURED' ? 'PAID' : t.status === 'FAILED' ? 'FAILED' : 'PENDING',
        description: `Order ${t.orderReference}`,
        createdAt: t.createdAt,
        updatedAt: t.createdAt,
      }))
    }

    const response = await api.get<{ data: Order[] }>('/orders')
    return response.data
  }

  async getOrderAnalytics(): Promise<OrderAnalytics> {
    if (USE_MOCK) {
      await new Promise((resolve) => setTimeout(resolve, 300))
      return {
        totalOrders: mockData.transactions.length,
        pendingOrders: 12,
        completedOrders: mockData.transactions.filter(t => t.status === 'CAPTURED').length,
        failedOrders: mockData.transactions.filter(t => t.status === 'FAILED').length,
        orderTrends: Array.from({ length: 30 }, (_, i) => ({
          date: new Date(Date.now() - (29 - i) * 86400000).toISOString().split('T')[0],
          count: Math.floor(Math.random() * 20) + 5,
        })),
      }
    }

    const response = await api.get<{ data: OrderAnalytics }>('/orders/analytics')
    return response.data
  }
}

export const adminService = new AdminService()
