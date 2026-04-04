import { apiClient, PaginatedResponse } from '@/services/api-client'

export interface Order {
  id: string
  orderReference: string
  userId: string
  userEmail: string
  amount: number
  currency: string
  status: OrderStatus
  description?: string
  items?: OrderItem[]
  metadata?: Record<string, unknown>
  createdAt: string
  updatedAt: string
}

export type OrderStatus = 'PENDING' | 'PAID' | 'FAILED' | 'CANCELLED' | 'REFUNDED'

export interface OrderItem {
  id: string
  name: string
  quantity: number
  price: number
}

export interface CreateOrderRequest {
  amount: number
  currency: string
  description?: string
  items?: Omit<OrderItem, 'id'>[]
  metadata?: Record<string, unknown>
}

export interface OrderFilters {
  status?: OrderStatus
  userId?: string
  startDate?: string
  endDate?: string
  page?: number
  limit?: number
}

export interface OrderAnalytics {
  totalOrders: number
  pendingOrders: number
  completedOrders: number
  failedOrders: number
  totalVolume: number
  averageOrderValue: number
}

export interface OrderTrend {
  date: string
  count: number
  volume: number
}

class OrderApi {
  async getOrders(filters?: OrderFilters): Promise<PaginatedResponse<Order>> {
    return apiClient.get<PaginatedResponse<Order>>('/orders', filters as Record<string, unknown>)
  }

  async getOrder(id: string): Promise<Order> {
    return apiClient.get<Order>(`/orders/${id}`)
  }

  async getRecentOrders(limit = 10): Promise<Order[]> {
    return apiClient.get<Order[]>(`/orders?limit=${limit}&sort=createdAt,desc`)
  }

  async createOrder(request: CreateOrderRequest): Promise<Order> {
    return apiClient.post<Order>('/orders', request)
  }

  async updateOrder(id: string, data: Partial<Order>): Promise<Order> {
    return apiClient.patch<Order>(`/orders/${id}`, data)
  }

  async cancelOrder(id: string): Promise<Order> {
    return apiClient.post<Order>(`/orders/${id}/cancel`)
  }

  async getOrderAnalytics(): Promise<OrderAnalytics> {
    return apiClient.get<OrderAnalytics>('/orders/analytics')
  }

  async getOrderTrends(days = 30): Promise<OrderTrend[]> {
    return apiClient.get<OrderTrend[]>(`/orders/trends?days=${days}`)
  }
}

export const orderApi = new OrderApi()
