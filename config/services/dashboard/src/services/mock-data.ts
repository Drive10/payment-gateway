export interface Transaction {
  id: string
  orderId: string
  amount: number
  currency: string
  status: 'PENDING' | 'COMPLETED' | 'FAILED'
  gateway: string
  customerId: string
  customerName: string
  customerEmail: string
  customerPhone?: string
  description?: string
  metadata?: Record<string, string | number | boolean>
  createdAt: string
  updatedAt: string
  errorMessage?: string
}

export interface DashboardStats {
  totalRevenue: number
  totalTransactions: number
  successRate: number
  avgTransactionAmount: number
}

export interface UserStats {
  totalSpent: number
  totalPayments: number
  successRate: number
}

const generateMockTransactions = (): Transaction[] => {
  const statuses: Transaction['status'][] = ['COMPLETED', 'COMPLETED', 'COMPLETED', 'PENDING', 'FAILED']
  const gateways = ['stripe', 'paypal', 'square']
  const customers = [
    { name: 'John Doe', email: 'john.doe@example.com', phone: '+1-555-0101' },
    { name: 'Jane Smith', email: 'jane.smith@example.com', phone: '+1-555-0102' },
    { name: 'Robert Johnson', email: 'r.johnson@example.com', phone: '+1-555-0103' },
    { name: 'Emily Davis', email: 'emily.d@example.com', phone: '+1-555-0104' },
    { name: 'Michael Wilson', email: 'm.wilson@example.com', phone: '+1-555-0105' },
    { name: 'Sarah Brown', email: 'sarah.b@example.com', phone: '+1-555-0106' },
    { name: 'David Miller', email: 'd.miller@example.com', phone: '+1-555-0107' },
    { name: 'Lisa Anderson', email: 'l.anderson@example.com', phone: '+1-555-0108' },
  ]

  const transactions: Transaction[] = []
  const now = Date.now()

  for (let i = 0; i < 50; i++) {
    const customer = customers[i % customers.length]
    const status = statuses[Math.floor(Math.random() * statuses.length)]
    const gateway = gateways[Math.floor(Math.random() * gateways.length)]
    const amount = Math.floor(Math.random() * 5000) + 10
    const createdAt = new Date(now - Math.floor(Math.random() * 30 * 24 * 60 * 60 * 1000))

    transactions.push({
      id: `txn_${Date.now()}_${i.toString(36)}${Math.random().toString(36).slice(2, 8)}`,
      orderId: `ORD-${Date.now().toString(36).slice(-8).toUpperCase()}`,
      amount,
      currency: 'USD',
      status,
      gateway,
      customerId: `cust_${i}`,
      customerName: customer.name,
      customerEmail: customer.email,
      customerPhone: customer.phone,
      description: `Payment for order`,
      metadata: {
        cardLast4: String(Math.floor(Math.random() * 10000)).padStart(4, '0'),
        cardBrand: ['visa', 'mastercard', 'amex'][Math.floor(Math.random() * 3)],
        isRecurring: Math.random() > 0.8,
      },
      createdAt: createdAt.toISOString(),
      updatedAt: new Date(createdAt.getTime() + Math.floor(Math.random() * 60000)).toISOString(),
      errorMessage: status === 'FAILED' ? 'Payment declined by issuer' : undefined,
    })
  }

  return transactions.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
}

export const mockTransactions = generateMockTransactions()
