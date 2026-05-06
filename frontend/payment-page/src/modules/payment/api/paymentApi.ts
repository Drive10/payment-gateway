/**
 * Payment API Layer
 * 
 * Centralized API calls with proper error handling,
 * retries, and authentication
 */

import { createApiError } from '../utils/api';
import type {
  CreateOrderRequest,
  CreateOrderResponse,
  CapturePaymentResponse,
  VerifyOtpResponse,
  PaymentState,
} from '../types/payment';

// Configuration
const API_BASE_URL = window.__ENV__?.API_BASE_URL || 'http://localhost:3001';
const API_ROOT = `${API_BASE_URL}/api/v1`;
const IS_PRODUCTION = window.__ENV__?.IS_PRODUCTION === true;

// API Key for merchant
const getApiKey = (): string => {
  if (IS_PRODUCTION) {
    return window.__ENV__?.MERCHANT_API_KEY || '';
  }
  // Test key from env or fallback
  return window.__ENV__?.MERCHANT_API_KEY 
    || import.meta.env.VITE_MERCHANT_API_KEY 
    || 'sk_test_88573d07c94d45f58ead0e698918f420';
};

/**
 * Get auth token for requests
 */
async function getAuthToken(): Promise<string> {
  const stored = localStorage.getItem('payflow-auth');
  if (stored) {
    const auth = JSON.parse(stored);
    if (auth?.token) {
      return auth.token;
    }
  }
  // Return API key as fallback for server-side auth
  return getApiKey();
}

/**
 * Make authenticated API request
 */
async function apiRequest<T>(
  endpoint: string,
  options: RequestInit & { timeout?: number; retries?: number } = {}
): Promise<T> {
  const { timeout = 30000, retries = 2, ...fetchOptions } = options;
  
  let lastError: Error | null = null;
  const token = await getAuthToken();
  
  for (let attempt = 0; attempt <= retries; attempt++) {
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), timeout);
      
      const response = await fetch(`${API_ROOT}${endpoint}`, {
        ...fetchOptions,
        signal: controller.signal,
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
          'X-Request-Id': `req-${Date.now()}-${Math.random().toString(36).slice(2)}`,
          ...fetchOptions.headers,
        },
      });
      
      clearTimeout(timeoutId);
      
      const data = await response.json();
      
      if (!response.ok) {
        throw createApiError(
          data.message || 'Request failed',
          response.status,
          data.code
        );
      }
      
      return data as T;
    } catch (error) {
      lastError = error as Error;
      
      // Don't retry on client errors (4xx)
      if (error && typeof error === 'object' && 'status' in error) {
        const status = (error as { status: number }).status;
        if (status >= 400 && status < 500) {
          throw error;
        }
      }
      
      // Wait before retry
      if (attempt < retries) {
        await new Promise(r => setTimeout(r, 1000 * (attempt + 1)));
      }
    }
  }
  
  throw lastError;
}

/**
 * Create payment order
 */
export async function createOrder(request: CreateOrderRequest): Promise<CreateOrderResponse> {
  const response = await apiRequest<{ success: boolean; data: CreateOrderResponse }>('/payments/create-order', {
    method: 'POST',
    body: JSON.stringify({
      orderId: request.orderId || `ORD-${Date.now().toString(36).toUpperCase()}`,
      amount: request.amount,
      currency: request.currency || 'INR',
      description: request.description || 'Payment for order',
      customerEmail: request.customerEmail,
      paymentMethod: request.paymentMethod || 'card',
    }),
  });
  
  if (!response.success) {
    throw createApiError('Failed to create order', 500, 'CREATE_ORDER_FAILED');
  }
  
  return response.data;
}

/**
 * Get payment status by ID
 */
export async function getPaymentStatus(paymentId: string): Promise<{ status: PaymentState; error?: string }> {
  const response = await apiRequest<{ success: boolean; data: { status: PaymentState; statusMessage?: string } }>(
    `/payments/${paymentId}/status`
  );
  
  if (!response.success) {
    throw createApiError('Failed to get payment status', 500, 'GET_STATUS_FAILED');
  }
  
  return {
    status: response.data.status,
    error: response.data.statusMessage,
  };
}

/**
 * Capture authorized payment
 */
export async function capturePayment(paymentId: string): Promise<CapturePaymentResponse> {
  const response = await apiRequest<{ success: boolean; data: CapturePaymentResponse }>(
    `/payments/${paymentId}/capture`,
    { method: 'POST' }
  );
  
  if (!response.success) {
    throw createApiError('Failed to capture payment', 500, 'CAPTURE_FAILED');
  }
  
  return response.data;
}

/**
 * Verify OTP for card payments
 */
export async function verifyOtp(paymentId: string, otp: string): Promise<VerifyOtpResponse> {
  const response = await apiRequest<{ success: boolean; data: VerifyOtpResponse }>(
    `/payments/${paymentId}/verify-otp`,
    {
      method: 'POST',
      body: JSON.stringify({ otp }),
    }
  );
  
  return response.data;
}

/**
 * Confirm payment method (initiates payment)
 */
export async function confirmPaymentMethod(
  paymentId: string,
  method: string,
  details?: Record<string, unknown>
): Promise<CreateOrderResponse> {
  const response = await apiRequest<{ success: boolean; data: CreateOrderResponse }>(
    `/payments/intents/${paymentId}/confirm`,
    {
      method: 'POST',
      body: JSON.stringify({
        paymentMethod: method,
        ...details,
      }),
    }
  );
  
  if (!response.success) {
    throw createApiError('Failed to confirm payment method', 500, 'CONFIRM_FAILED');
  }
  
  return response.data;
}