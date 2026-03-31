import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '@/services/api/client'
import type { User } from '@/services/auth.service'

export const adminKeys = {
  all: ['admin'] as const,
  dashboardStats: () => [...adminKeys.all, 'dashboardStats'] as const,
  systemHealth: () => [...adminKeys.all, 'systemHealth'] as const,
  auditLogs: (page: number) => [...adminKeys.all, 'auditLogs', page] as const,
  users: (page: number, role?: string) => [...adminKeys.all, 'users', page, role] as const,
  user: (id: string) => [...adminKeys.all, 'user', id] as const,
}

export interface SystemHealth {
  status: 'UP' | 'DOWN' | 'DEGRADED'
  services: ServiceHealth[]
  timestamp: string
}

export interface ServiceHealth {
  name: string
  status: 'UP' | 'DOWN' | 'DEGRADED'
  latency: number
  uptime: number
}

export interface DashboardStats {
  totalRevenue: number
  revenueChange: number
  totalTransactions: number
  transactionChange: number
  successRate: number
  successRateChange: number
  failedTransactions: number
  failedChange: number
}

export interface AuditLog {
  id: string
  userId: string
  userEmail: string
  action: string
  resource: string
  resourceId?: string
  details?: Record<string, unknown>
  ipAddress?: string
  createdAt: string
}

export interface PaginatedResponse<T> {
  data: T[]
  total: number
  page: number
  limit: number
  totalPages: number
}

export function useDashboardStats() {
  return useQuery({
    queryKey: adminKeys.dashboardStats(),
    queryFn: async () => {
      const response = await api.get<{ data: DashboardStats }>('/admin/dashboard/stats')
      return response.data
    },
    refetchInterval: 30000,
  })
}

export function useSystemHealth() {
  return useQuery({
    queryKey: adminKeys.systemHealth(),
    queryFn: async () => {
      const response = await api.get<SystemHealth>('/admin/health')
      return response
    },
    refetchInterval: 10000,
  })
}

export function useAuditLogs(page = 1, limit = 20) {
  return useQuery({
    queryKey: adminKeys.auditLogs(page),
    queryFn: async () => {
      const response = await api.get<PaginatedResponse<AuditLog>>(`/admin/audit-logs?page=${page}&limit=${limit}`)
      return response
    },
  })
}

export function useUsers(page = 1, limit = 20, role?: string) {
  return useQuery({
    queryKey: adminKeys.users(page, role),
    queryFn: async () => {
      const params = new URLSearchParams({ page: String(page), limit: String(limit) })
      if (role) params.append('role', role)
      const response = await api.get<PaginatedResponse<User>>(`/admin/users?${params}`)
      return response
    },
  })
}

export function useUser(id: string) {
  return useQuery({
    queryKey: adminKeys.user(id),
    queryFn: async () => {
      const response = await api.get<User>(`/admin/users/${id}`)
      return response
    },
    enabled: !!id,
  })
}

export function useCreateUser() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: async (data: { email: string; password: string; firstName: string; lastName: string; role: string }) => {
      const response = await api.post<User>('/admin/users', data)
      return response
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.users(1) })
    },
  })
}

export function useUpdateUser() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: async ({ id, data }: { id: string; data: Partial<User> }) => {
      const response = await api.patch<User>(`/admin/users/${id}`, data)
      return response
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: adminKeys.users(1) })
      queryClient.setQueryData(adminKeys.user(data.id), data)
    },
  })
}

export function useDeleteUser() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/admin/users/${id}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.users(1) })
    },
  })
}

export function useEnableUser() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: async (id: string) => {
      const response = await api.post<User>(`/admin/users/${id}/enable`)
      return response
    },
    onSuccess: (data) => {
      queryClient.setQueryData(adminKeys.user(data.id), data)
    },
  })
}

export function useDisableUser() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: async (id: string) => {
      const response = await api.post<User>(`/admin/users/${id}/disable`)
      return response
    },
    onSuccess: (data) => {
      queryClient.setQueryData(adminKeys.user(data.id), data)
    },
  })
}
