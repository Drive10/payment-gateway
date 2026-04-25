import axios from 'axios';
import { 
  CreateOrderRequest, 
  CreateOrderResponse, 
  PaymentStatusResponse 
} from '../types/payment';

const API_BASE_URL = import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const paymentService = {
  async createOrder(data: CreateOrderRequest): Promise<CreateOrderResponse> {
    const response = await api.post<CreateOrderResponse>('/api/payments/create-order', data);
    return response.data;
  },

  async getPaymentStatus(orderId: string): Promise<PaymentStatusResponse> {
    const response = await api.get<PaymentStatusResponse>(`/api/payments/status/${orderId}`);
    return response.data;
  },
};
