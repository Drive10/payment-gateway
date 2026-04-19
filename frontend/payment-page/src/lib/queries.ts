import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from './axios';
import { useAuthStore } from './stores';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL 
  ? `${import.meta.env.VITE_API_BASE_URL}/api/v1` 
  : 'http://localhost:8080/api/v1';

interface CreateOrderRequest {
  externalReference: string;
  amount: number;
  currency?: string;
  description?: string;
  customerEmail?: string;
  customerName?: string;
  userId?: string;
}

interface CreatePaymentRequest {
  orderId: string;
  merchantId?: string;
  method: string;
  provider: string;
  transactionMode: string;
  notes?: string;
}

interface Order {
  id: string;
  externalReference: string;
  amount: number;
  currency: string;
  status: string;
  customerEmail?: string;
  customerName?: string;
  createdAt: string;
}

interface Payment {
  id: string;
  orderId: string;
  amount: number;
  status: string;
  method: string;
  provider: string;
  transactionMode: string;
  providerOrderId?: string;
  providerPaymentId?: string;
  errorMessage?: string;
  errorCode?: string;
  createdAt: string;
}

const getAuthHeaders = () => {
  const token = useAuthStore.getState().token;
  return token ? { Authorization: `Bearer ${token}` } : {};
};

export const useCreateOrder = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (data: CreateOrderRequest) => {
      const response = await api.post('/orders', data, {
        headers: { ...getAuthHeaders(), 'X-Request-Id': `order-${Date.now()}` },
      });
      return response.data.data as Order;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders'] });
    },
  });
};

export const useCreatePayment = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (data: CreatePaymentRequest) => {
      const response = await api.post('/payments', data, {
        headers: {
          ...getAuthHeaders(),
          'Idempotency-Key': `pay-${Date.now()}-${Math.random().toString(36).slice(2)}`,
          'X-Request-Id': `payment-${Date.now()}`,
        },
      });
      return response.data.data as Payment;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payments'] });
    },
  });
};

export const useGetPayment = (paymentId: string, enabled = true) => {
  return useQuery({
    queryKey: ['payment', paymentId],
    queryFn: async () => {
      const response = await api.get(`/payments/${paymentId}`, {
        headers: getAuthHeaders(),
      });
      return response.data.data as Payment;
    },
    enabled: enabled && !!paymentId,
    refetchInterval: (query) => {
      const data = query.state.data;
      if (data?.status === 'PROCESSING' || data?.status === 'PENDING') {
        return 3000;
      }
      return false;
    },
  });
};

export const useGetOrder = (orderId: string, enabled = true) => {
  return useQuery({
    queryKey: ['order', orderId],
    queryFn: async () => {
      const response = await api.get(`/orders/${orderId}`, {
        headers: getAuthHeaders(),
      });
      return response.data.data as Order;
    },
    enabled: enabled && !!orderId,
  });
};

export const useGetPaymentHistory = (limit = 10, offset = 0) => {
  return useQuery({
    queryKey: ['payments', 'history', limit, offset],
    queryFn: async () => {
      const response = await api.get(`/payments?limit=${limit}&offset=${offset}`, {
        headers: getAuthHeaders(),
      });
      return response.data.data as Payment[];
    },
  });
};

export const useGetOrderHistory = (limit = 10, offset = 0) => {
  return useQuery({
    queryKey: ['orders', 'history', limit, offset],
    queryFn: async () => {
      const response = await api.get(`/orders?limit=${limit}&offset=${offset}`, {
        headers: getAuthHeaders(),
      });
      return response.data.data as Order[];
    },
  });
};

export const useCapturePayment = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ paymentId, correlationId }: { paymentId: string; correlationId: string }) => {
      const response = await api.post(`/payments/${paymentId}/capture`, {}, {
        headers: {
          ...getAuthHeaders(),
          'X-Request-Id': correlationId,
        },
      });
      return response.data.data as Payment;
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['payment', variables.paymentId] });
    },
  });
};

export const useLogin = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (credentials: { email: string; password: string }) => {
      const response = await api.post('/auth/login', credentials);
      return response.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['auth'] });
    },
  });
};

export const useRefreshToken = () => {
  return useMutation({
    mutationFn: async (refreshToken: string) => {
      const response = await api.post('/auth/refresh', { refreshToken });
      return response.data.data;
    },
  });
};

export { API_BASE_URL };
export type { Order, Payment, CreateOrderRequest, CreatePaymentRequest };