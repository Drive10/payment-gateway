import { api } from './api/client'
import { mockData } from './mock-data'
import type { Order, Payment, UserDashboardData, UserPayment } from './api/types'

const USE_MOCK = false

export type { UserPayment, UserDashboardData }

class UserService {
  async getDashboard(): Promise<UserDashboardData> {
    if (USE_MOCK) {
      await new Promise((resolve) => setTimeout(resolve, 300))

      const userPayments = mockData.transactions.slice(0, 5)

      return {
        userName: mockData.userName,
        totalSpent: userPayments.reduce((sum, p) => sum + p.amount, 0),
        totalTransactions: userPayments.length,
        monthlySpent: userPayments[0]?.amount || 0,
        monthlyTransactions: 1,
        lastPayment: userPayments[0] ? {
          amount: userPayments[0].amount,
          status: userPayments[0].status,
          date: userPayments[0].createdAt
        } : null,
        recentPayments: userPayments.map(p => ({
          id: p.id,
          orderReference: p.orderReference,
          amount: p.amount,
          currency: p.currency,
          status: p.status,
          provider: p.provider,
          method: p.method,
          date: p.createdAt
        }))
      }
    }

    const response = await api.get<{ data: UserDashboardData }>('/users/dashboard')
    return response.data
  }

  async getPayments(): Promise<UserPayment[]> {
    if (USE_MOCK) {
      await new Promise((resolve) => setTimeout(resolve, 500))

      return mockData.transactions.map(p => ({
        id: p.id,
        orderReference: p.orderReference,
        amount: p.amount,
        currency: p.currency,
        status: p.status,
        provider: p.provider,
        method: p.method,
        date: p.createdAt
      }))
    }

    const response = await api.get<{ data: UserPayment[] }>('/users/payments')
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

    const response = await api.get<{ data: Order[] }>('/users/orders')
    return response.data
  }

  async retryPayment(paymentId: string): Promise<Payment> {
    if (USE_MOCK) {
      await new Promise((resolve) => setTimeout(resolve, 1000))
      return {
        id: paymentId,
        orderId: 'order-1',
        orderReference: 'ORD_RETRY_001',
        userId: 'user-1',
        userEmail: 'john.doe@example.com',
        amount: 5000,
        currency: 'INR',
        status: 'CAPTURED',
        provider: 'SIMULATOR',
        method: 'CARD',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }
    }

    const response = await api.post<{ data: Payment }>(`/payments/${paymentId}/retry`)
    return response.data
  }
}

export const userService = new UserService()
