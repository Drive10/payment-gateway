import { api } from './api/client'
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

export const adminService = {
  async getDashboardStats(): Promise<DashboardStats> {
    const response = await api.get<{ data: DashboardStats }>('/admin/dashboard/stats')
    return response.data
  },

  async getRecentTransactions(limit: number): Promise<Transaction[]> {
    const response = await api.get<{ data: Transaction[] }>(`/transactions?limit=${limit}`)
    return response.data
  },

  async getTransactions(): Promise<Transaction[]> {
    const response = await api.get<{ data: Transaction[] }>('/payments')
    return response.data
  },

  async getTransactionDetail(id: string): Promise<TransactionDetail> {
    const response = await api.get<{ data: TransactionDetail }>(`/payments/${id}`)
    return response.data
  },

  async getAnalytics(): Promise<AnalyticsData> {
    const response = await api.get<{ data: AnalyticsData }>('/analytics')
    return response.data
  },

  async getOrders(): Promise<Order[]> {
    const response = await api.get<{ data: Order[] }>('/orders')
    return response.data
  },

  async getOrderAnalytics(): Promise<OrderAnalytics> {
    const response = await api.get<{ data: OrderAnalytics }>('/orders/analytics')
    return response.data
  },
}
