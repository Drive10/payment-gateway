import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from './axios';
import { useAuthStore } from './stores';

interface FrontendPaymentRequest {
  productId: string;
  customerEmail?: string;
}

interface OrderSnapshot {
  id: string;
  amount: number;
  currency: string;
}

interface Payment {
  id: string;
  status: string;
  checkoutUrl?: string;
  orderId?: string;
}

interface MerchantPaymentResponse {
  order: OrderSnapshot;
  payment: Payment;
  checkoutUrl?: string;
}

const API_BASE_URL = import.meta.env.VITE_API_GATEWAY_URL 
  ? `${import.meta.env.VITE_API_GATEWAY_URL}/api/v1` 
  : 'http://localhost:8080/api/v1';

const getAuthHeaders = () => {
  const token = useAuthStore.getState().token;
  return token ? { Authorization: `Bearer ${token}` } : {};
};

export const useCreatePayment = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (data: FrontendPaymentRequest) => {
      const response = await api.post('/api/v1/payments/create-order', data, {
        headers: { ...getAuthHeaders(), 'X-Request-Id': `pay-${Date.now()}` },
      });
      return response.data.data as MerchantPaymentResponse;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payments'] });
    },
  });
};

export const usePaymentStatus = (paymentId: string, enabled: boolean = false) => {
  return useQuery({
    queryKey: ['payment', paymentId],
    queryFn: async () => {
      const response = await api.get(`/api/v1/payments/${paymentId}`, {
        headers: getAuthHeaders(),
      });
      return response.data.data as Payment;
    },
    enabled: enabled && !!paymentId,
    refetchInterval: 3000,
  });
};

export const useLogin = () => {
  return useMutation({
    mutationFn: async (credentials: { email: string; password: string }) => {
      const response = await api.post('/api/v1/merchant/auth/login', credentials);
      return response.data.data;
    },
  });
};

export const useRegister = () => {
  return useMutation({
    mutationFn: async (data: { email: string; password: string; firstName?: string; lastName?: string }) => {
      const response = await api.post('/api/v1/merchant/auth/register', data);
      return response.data.data;
    },
  });
};

export const useRefreshToken = () => {
  return useMutation({
    mutationFn: async (refreshToken: string) => {
      const response = await api.post('/api/v1/merchant/auth/refresh', { refreshToken });
      return response.data.data;
    },
  });
};

export const useMerchantBalance = (merchantId: string) => {
  return useQuery({
    queryKey: ['balance', merchantId],
    queryFn: async () => {
      const response = await api.get(`/api/v1/payments/balance/${merchantId}`, {
        headers: getAuthHeaders(),
      });
      return response.data.data;
    },
    enabled: !!merchantId,
  });
};

export const usePaymentHistory = (limit = 10, offset = 0) => {
  return useQuery({
    queryKey: ['payments', limit, offset],
    queryFn: async () => {
      const response = await api.get(`/api/v1/payments/list?limit=${limit}&offset=${offset}`, {
        headers: getAuthHeaders(),
      });
      return response.data.data;
    },
  });
};