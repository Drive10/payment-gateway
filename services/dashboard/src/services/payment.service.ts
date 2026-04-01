import { apiClient, PaginatedResponse } from '@/services/api-client'

export interface Payment {
  id: string
  orderId: string
  orderReference: string
  userId: string
  userEmail: string
  amount: number
  currency: string
  status: PaymentStatus
  provider: string
  method: string
  providerOrderId?: string
  providerPaymentId?: string
  checkoutUrl?: string
  errorMessage?: string
  metadata?: Record<string, unknown>
  createdAt: string
  updatedAt: string
}

export type PaymentStatus = 'CREATED' | 'PENDING' | 'AUTHORIZED' | 'CAPTURED' | 'FAILED' | 'REFUNDED' | 'PARTIALLY_REFUNDED' | 'CANCELLED'

export interface CreatePaymentRequest {
  orderId: string
  amount: number
  currency: string
  method: PaymentMethod
  provider?: string
  metadata?: Record<string, unknown>
}

export type PaymentMethod = 'CARD' | 'UPI' | 'WALLET' | 'NET_BANKING' | 'EMI'

export interface RefundRequest {
  amount?: number
  reason?: string
}

export interface Refund {
  id: string
  paymentId: string
  amount: number
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
  reason?: string
  createdAt: string
  processedAt?: string
}

export interface PaymentFilters {
  status?: PaymentStatus
  provider?: string
  method?: string
  userId?: string
  startDate?: string
  endDate?: string
  page?: number
  limit?: number
}

export interface PaymentAnalytics {
  totalVolume: number
  totalTransactions: number
  successRate: number
  averageTransaction: number
  totalRefunded: number
  failedCount: number
  pendingCount: number
}

export interface RevenueTrend {
  date: string
  revenue: number
  transactionCount: number
}

export interface ProviderStats {
  provider: string
  totalTransactions: number
  successRate: number
  totalVolume: number
}

// Payment Link types
export interface PaymentLink {
  paymentLinkId: string
  paymentLinkUrl: string
  amount: number
  currency: string
  status: string
  createdAt: string
  expiresAt: string
  description?: string
  customer?: {
    name?: string
    email?: string
    phone?: string
  }
}

export interface CreatePaymentLinkRequest {
  merchantId: string
  amount: number
  currency: string
  description?: string
  customerName?: string
  customerEmail?: string
  customerPhone?: string
  successUrl?: string
  cancelUrl?: string
}

class PaymentApi {
  async getPayments(filters?: PaymentFilters): Promise<PaginatedResponse<Payment>> {
    return apiClient.get<PaginatedResponse<Payment>>('/payments', filters as Record<string, unknown>)
  }

  async getPayment(id: string): Promise<Payment> {
    return apiClient.get<Payment>(`/payments/${id}`)
  }

  async getRecentPayments(limit = 10): Promise<Payment[]> {
    return apiClient.get<Payment[]>(`/payments?limit=${limit}&sort=createdAt,desc`)
  }

  async createPayment(request: CreatePaymentRequest): Promise<Payment> {
    return apiClient.post<Payment>('/payments', request)
  }

  // Payment Link methods
  async createPaymentLink(request: CreatePaymentLinkRequest): Promise<PaymentLink> {
    return apiClient.post<PaymentLink>('/payments/links', request)
  }

  async getPaymentLinks(merchantId: string): Promise<PaymentLink[]> {
    return apiClient.get<PaymentLink[]>(`/payments/links?merchantId=${merchantId}`)
  }

  async capturePayment(paymentId: string): Promise<Payment> {
    return apiClient.post<Payment>(`/payments/${paymentId}/capture`)
  }

  async cancelPayment(paymentId: string): Promise<Payment> {
    return apiClient.post<Payment>(`/payments/${paymentId}/cancel`)
  }

  async refundPayment(paymentId: string, request: RefundRequest): Promise<Refund> {
    return apiClient.post<Refund>(`/payments/${paymentId}/refunds`, request)
  }

  async getRefunds(paymentId: string): Promise<Refund[]> {
    return apiClient.get<Refund[]>(`/payments/${paymentId}/refunds`)
  }

  async getAnalytics(startDate?: string, endDate?: string): Promise<PaymentAnalytics> {
    const params = new URLSearchParams()
    if (startDate) params.append('startDate', startDate)
    if (endDate) params.append('endDate', endDate)
    const query = params.toString() ? `?${params.toString()}` : ''
    return apiClient.get<PaymentAnalytics>(`/payments/analytics${query}`)
  }

  async getRevenueTrends(days = 30): Promise<RevenueTrend[]> {
    return apiClient.get<RevenueTrend[]>(`/payments/revenue-trends?days=${days}`)
  }

  async getProviderStats(): Promise<ProviderStats[]> {
    return apiClient.get<ProviderStats[]>('/payments/provider-stats')
  }

  async retryPayment(paymentId: string): Promise<Payment> {
    return apiClient.post<Payment>(`/payments/${paymentId}/retry`)
  }
}

export const paymentApi = new PaymentApi()
