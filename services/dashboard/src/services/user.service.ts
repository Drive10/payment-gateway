import { mockData } from './mock-data'

export interface UserPayment {
  id: string
  orderReference: string
  amount: number
  currency: string
  status: string
  provider: string
  method: string
  date: string
}

export interface UserDashboardData {
  userName: string
  totalSpent: number
  totalTransactions: number
  monthlySpent: number
  monthlyTransactions: number
  lastPayment: { amount: number; status: string; date: string } | null
  recentPayments: UserPayment[]
}

class UserService {
  async getDashboard(): Promise<UserDashboardData> {
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

  async getPayments(): Promise<UserPayment[]> {
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
}

export const userService = new UserService()
