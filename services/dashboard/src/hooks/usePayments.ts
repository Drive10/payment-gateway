import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'

import { paymentApi, type PaymentFilters, type RefundRequest, type CreatePaymentRequest } from '../services/payment.service'

export const paymentKeys = {
  all: ['payments'] as const,
  lists: () => [...paymentKeys.all, 'list'] as const,
  list: (filters: PaymentFilters) => [...paymentKeys.lists(), filters] as const,
  recent: (limit: number) => [...paymentKeys.all, 'recent', limit] as const,
  details: () => [...paymentKeys.all, 'detail'] as const,
  detail: (id: string) => [...paymentKeys.details(), id] as const,
  analytics: () => [...paymentKeys.all, 'analytics'] as const,
  revenueTrends: (days: number) => [...paymentKeys.all, 'revenueTrends', days] as const,
  providerStats: () => [...paymentKeys.all, 'providerStats'] as const,
}

export function usePayments(filters?: PaymentFilters) {
  return useQuery({
    queryKey: paymentKeys.list(filters || {}),
    queryFn: () => paymentApi.getPayments(filters),
  })
}

export function useRecentPayments(limit = 10) {
  return useQuery({
    queryKey: paymentKeys.recent(limit),
    queryFn: () => paymentApi.getRecentPayments(limit),
  })
}

export function usePayment(id: string) {
  return useQuery({
    queryKey: paymentKeys.detail(id),
    queryFn: () => paymentApi.getPayment(id),
    enabled: !!id,
  })
}

export function useCreatePayment() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (request: CreatePaymentRequest) => paymentApi.createPayment(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: paymentKeys.all })
    },
  })
}

export function useCapturePayment() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (paymentId: string) => paymentApi.capturePayment(paymentId),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: paymentKeys.all })
      queryClient.setQueryData(paymentKeys.detail(data.id), data)
    },
  })
}

export function useCancelPayment() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (paymentId: string) => paymentApi.cancelPayment(paymentId),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: paymentKeys.all })
      queryClient.setQueryData(paymentKeys.detail(data.id), data)
    },
  })
}

export function useRefundPayment() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: ({ paymentId, request }: { paymentId: string; request: RefundRequest }) =>
      paymentApi.refundPayment(paymentId, request),
    onSuccess: (_, { paymentId }) => {
      queryClient.invalidateQueries({ queryKey: paymentKeys.all })
      queryClient.invalidateQueries({ queryKey: paymentKeys.detail(paymentId) })
    },
  })
}

export function usePaymentAnalytics(startDate?: string, endDate?: string) {
  return useQuery({
    queryKey: [...paymentKeys.analytics(), startDate, endDate],
    queryFn: () => paymentApi.getAnalytics(startDate, endDate),
  })
}

export function useRevenueTrends(days = 30) {
  return useQuery({
    queryKey: paymentKeys.revenueTrends(days),
    queryFn: () => paymentApi.getRevenueTrends(days),
  })
}

export function useProviderStats() {
  return useQuery({
    queryKey: paymentKeys.providerStats(),
    queryFn: () => paymentApi.getProviderStats(),
  })
}

export function useRetryPayment() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (paymentId: string) => paymentApi.retryPayment(paymentId),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: paymentKeys.all })
      queryClient.setQueryData(paymentKeys.detail(data.id), data)
    },
  })
}
