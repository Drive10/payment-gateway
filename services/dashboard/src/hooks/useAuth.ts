import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'

import { authApi, type LoginRequest, type RegisterRequest, type AuthResponse } from '../services/auth.service'

export const authKeys = {
  all: ['auth'] as const,
  currentUser: () => [...authKeys.all, 'currentUser'] as const,
}

export function useCurrentUser() {
  return useQuery({
    queryKey: authKeys.currentUser(),
    queryFn: () => authApi.getCurrentUser(),
    retry: false,
    staleTime: 5 * 60 * 1000,
  })
}

export function useLogin() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (request: LoginRequest) => authApi.login(request),
    onSuccess: (data: AuthResponse) => {
      localStorage.setItem('payflow_auth', JSON.stringify({
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
      }))
      queryClient.setQueryData(authKeys.currentUser(), data.user)
    },
  })
}

export function useRegister() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (request: RegisterRequest) => authApi.register(request),
    onSuccess: (data: AuthResponse) => {
      localStorage.setItem('payflow_auth', JSON.stringify({
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
      }))
      queryClient.setQueryData(authKeys.currentUser(), data.user)
    },
  })
}

export function useLogout() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: () => authApi.logout(),
    onSettled: () => {
      localStorage.removeItem('payflow_auth')
      queryClient.clear()
    },
  })
}

export function useUpdateProfile() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (data: Parameters<typeof authApi.updateProfile>[0]) => authApi.updateProfile(data),
    onSuccess: (data) => {
      queryClient.setQueryData(authKeys.currentUser(), data)
    },
  })
}

export function useChangePassword() {
  return useMutation({
    mutationFn: ({ currentPassword, newPassword }: { currentPassword: string; newPassword: string }) =>
      authApi.changePassword(currentPassword, newPassword),
  })
}
