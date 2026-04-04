import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'

import { orderApi, type Order, type OrderFilters, type CreateOrderRequest } from '../services/order.service'

export const orderKeys = {
  all: ['orders'] as const,
  lists: () => [...orderKeys.all, 'list'] as const,
  list: (filters: OrderFilters) => [...orderKeys.lists(), filters] as const,
  recent: (limit: number) => [...orderKeys.all, 'recent', limit] as const,
  details: () => [...orderKeys.all, 'detail'] as const,
  detail: (id: string) => [...orderKeys.details(), id] as const,
  analytics: () => [...orderKeys.all, 'analytics'] as const,
  trends: (days: number) => [...orderKeys.all, 'trends', days] as const,
}

export function useOrders(filters?: OrderFilters) {
  return useQuery({
    queryKey: orderKeys.list(filters || {}),
    queryFn: () => orderApi.getOrders(filters),
  })
}

export function useRecentOrders(limit = 10) {
  return useQuery({
    queryKey: orderKeys.recent(limit),
    queryFn: () => orderApi.getRecentOrders(limit),
  })
}

export function useOrder(id: string) {
  return useQuery({
    queryKey: orderKeys.detail(id),
    queryFn: () => orderApi.getOrder(id),
    enabled: !!id,
  })
}

export function useCreateOrder() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (request: CreateOrderRequest) => orderApi.createOrder(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: orderKeys.all })
    },
  })
}

export function useUpdateOrder() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<Order> }) =>
      orderApi.updateOrder(id, data),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: orderKeys.all })
      queryClient.setQueryData(orderKeys.detail(data.id), data)
    },
  })
}

export function useCancelOrder() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (id: string) => orderApi.cancelOrder(id),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: orderKeys.all })
      queryClient.setQueryData(orderKeys.detail(data.id), data)
    },
  })
}

export function useOrderAnalytics() {
  return useQuery({
    queryKey: orderKeys.analytics(),
    queryFn: () => orderApi.getOrderAnalytics(),
  })
}

export function useOrderTrends(days = 30) {
  return useQuery({
    queryKey: orderKeys.trends(days),
    queryFn: () => orderApi.getOrderTrends(days),
  })
}
