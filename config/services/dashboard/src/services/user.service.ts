import { mockTransactions, type Transaction, type UserStats } from './mock-data'

const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

export async function getUserDashboardStats(): Promise<UserStats> {
  await delay(300)
  
  const userTransactions = mockTransactions.filter((tx) => tx.customerId === 'cust_0')
  const completed = userTransactions.filter((tx) => tx.status === 'COMPLETED')
  const totalSpent = completed.reduce((sum, tx) => sum + tx.amount, 0)
  const totalPayments = userTransactions.length
  const successRate = (completed.length / totalPayments) * 100

  return {
    totalSpent,
    totalPayments,
    successRate,
  }
}

export async function getUserTransactions(limit?: number): Promise<Transaction[]> {
  await delay(300)
  const userTransactions = mockTransactions.filter((tx) => tx.customerId === 'cust_0')
  return limit ? userTransactions.slice(0, limit) : userTransactions
}

export async function getUserTransactionById(id: string): Promise<Transaction | null> {
  await delay(200)
  return mockTransactions.find((tx) => tx.id === id && tx.customerId === 'cust_0') || null
}
