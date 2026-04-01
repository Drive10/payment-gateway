export interface Order {
  id: string
  orderReference: string
  userId: string
  userEmail: string
  amount: number
  currency: string
  status: 'PENDING' | 'PAID' | 'FAILED' | 'CANCELLED'
  description?: string
  metadata?: Record<string, unknown>
  createdAt: string
  updatedAt: string
}

export interface Payment {
  id: string
  orderId: string
  orderReference: string
  userId: string
  userEmail: string
  amount: number
  currency: string
  status: 'CREATED' | 'PENDING' | 'CAPTURED' | 'FAILED' | 'REFUNDED' | 'PARTIALLY_REFUNDED'
  provider: string
  method: string
  providerOrderId?: string
  providerPaymentId?: string
  checkoutUrl?: string
  errorMessage?: string
  createdAt: string
  updatedAt: string
}

export interface Transaction {
  id: string
  orderReference: string
  userId?: string
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
  transactions: TransactionEvent[]
  errorLogs?: { message: string; timestamp: string }[]
  updatedAt?: string
}

export interface TransactionEvent {
  id: string
  type: string
  status: string
  amount: number
  remarks: string
  createdAt: string
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

export interface OrderAnalytics {
  totalOrders: number
  pendingOrders: number
  completedOrders: number
  failedOrders: number
  orderTrends: { date: string; count: number }[]
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

export interface MerchantBalance {
  availableBalance: number
  pendingBalance: number
  totalBalance: number
  currency: string
}

export interface MerchantAnalytics {
  totalRevenue: number
  totalFees: number
  totalRefunds: number
  netRevenue: number
  totalPayments: number
  successCount: number
  failedCount: number
  successRate: number
}

export interface PaymentTrend {
  date: string
  revenue: number
  count: number
}
