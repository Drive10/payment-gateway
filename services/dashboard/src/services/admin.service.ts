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
    return api.get<DashboardStats>('/admin/dashboard/stats')
  },

  async getRecentTransactions(limit: number): Promise<Transaction[]> {
    return api.get<Transaction[]>(`/transactions?limit=${limit}`)
  },

  async getTransactions(): Promise<Transaction[]> {
    return api.get<Transaction[]>('/payments')
  },

  async getTransactionDetail(id: string): Promise<TransactionDetail> {
    return api.get<TransactionDetail>(`/payments/${id}`)
  },

  async getAnalytics(): Promise<AnalyticsData> {
    return api.get<AnalyticsData>('/analytics')
  },

  async getOrders(): Promise<Order[]> {
    return api.get<Order[]>('/orders')
  },

  async getOrderAnalytics(): Promise<OrderAnalytics> {
    return api.get<OrderAnalytics>('/orders/analytics')
  },
}
