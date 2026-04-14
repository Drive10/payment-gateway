import { useState, useRef, useCallback, useEffect } from 'react';
import api from '../lib/axios';
import { usePaymentSecurity } from './usePaymentSecurity';
import type { 
  PaymentState, 
  PaymentStatus, 
  InitiatePaymentResponse,
  CardDetails,
  UPIDetails,
  NetBankingDetails,
  PaymentMethod 
} from '../types/payment';

const BACKEND_STATUS_TO_FRONTEND: Record<string, PaymentStatus> = {
  'PENDING': 'initiated',
  'CREATED': 'initiated',
  'AUTHORIZATION_PENDING': 'pending_otp',
  'AUTHORIZED': 'authorized',
  'PROCESSING': 'processing',
  'CAPTURED': 'success',
  'PARTIALLY_REFUNDED': 'refunded',
  'REFUNDED': 'refunded',
  'FAILED': 'failed',
  'EXPIRED': 'expired',
};

function mapBackendStatus(backendStatus: string): PaymentStatus {
  return BACKEND_STATUS_TO_FRONTEND[backendStatus] || 'idle';
}

interface UsePaymentOptions {
  amount: number;
  currency?: string;
  merchantId?: string;
}

export function usePayment({ 
  amount, 
  currency = 'INR',
  merchantId = 'MERCHANT_001' 
}: UsePaymentOptions) {
  const [state, setState] = useState<PaymentState>({
    status: 'idle',
    error: null,
    transactionId: null,
  });

  const pollTimerRef = useRef<NodeJS.Timeout | null>(null);
  const idempotencyKeyRef = useRef<string>(
    `txn_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
  );

  const { generateToken, clearSensitiveData } = usePaymentSecurity({ merchantId });

  const submitCardPayment = useCallback(async (cardDetails: CardDetails) => {
    setState(prev => ({ ...prev, status: 'processing', error: null }));

    try {
      const cardToken = await generateToken({
        cardNumber: cardDetails.cardNumber,
        expiry: cardDetails.expiry,
        cvv: cardDetails.cvv,
      });

      const response = await api.post<InitiatePaymentResponse>('/api/v1/payments/initiate', {
        cardToken,
        amount,
        currency,
        merchantId,
        idempotencyKey: idempotencyKeyRef.current,
      });

      const { transactionId, status: backendStatus, errorCode, message } = response.data;
      const frontendStatus = mapBackendStatus(backendStatus);

      setState({
        status: frontendStatus,
        error: frontendStatus === 'failed' ? (message || 'Payment failed') : null,
        transactionId,
      });

      return { transactionId, status: frontendStatus };
    } catch (error: any) {
      const message = error.response?.data?.error?.message || error.response?.data?.message || 'Payment failed. Please try again.';
      setState(prev => ({
        ...prev,
        status: 'failed',
        error: message,
      }));
      throw error;
    }
  }, [amount, currency, merchantId, generateToken]);

  const submitUPIPayment = useCallback(async (upiDetails: UPIDetails) => {
    setState(prev => ({ ...prev, status: 'processing', error: null }));

    try {
      const response = await api.post<InitiatePaymentResponse>('/api/v1/payments/initiate', {
        upiId: upiDetails.upiId,
        paymentMethod: 'UPI',
        amount,
        currency,
        merchantId,
        idempotencyKey: idempotencyKeyRef.current,
        email: upiDetails.email,
      });

      const { transactionId, status: backendStatus, message } = response.data;
      const frontendStatus = mapBackendStatus(backendStatus);

      setState({
        status: frontendStatus,
        error: frontendStatus === 'failed' ? (message || 'Payment failed') : null,
        transactionId,
      });

      return { transactionId, status: frontendStatus };
    } catch (error: any) {
      const message = error.response?.data?.error?.message || error.response?.data?.message || 'Payment failed. Please try again.';
      setState(prev => ({
        ...prev,
        status: 'failed',
        error: message,
      }));
      throw error;
    }
  }, [amount, currency, merchantId]);

  const submitNetBankingPayment = useCallback(async (netBankingDetails: NetBankingDetails) => {
    setState(prev => ({ ...prev, status: 'processing', error: null }));

    try {
      const response = await api.post<InitiatePaymentResponse>('/api/v1/payments/initiate', {
        bankCode: netBankingDetails.bankCode,
        paymentMethod: 'NETBANKING',
        amount,
        currency,
        merchantId,
        idempotencyKey: idempotencyKeyRef.current,
        email: netBankingDetails.email,
      });

      const { transactionId, status: backendStatus, message } = response.data;
      const frontendStatus = mapBackendStatus(backendStatus);

      setState({
        status: frontendStatus,
        error: frontendStatus === 'failed' ? (message || 'Payment failed') : null,
        transactionId,
      });

      return { transactionId, status: frontendStatus };
    } catch (error: any) {
      const message = error.response?.data?.error?.message || error.response?.data?.message || 'Payment failed. Please try again.';
      setState(prev => ({
        ...prev,
        status: 'failed',
        error: message,
      }));
      throw error;
    }
  }, [amount, currency, merchantId]);

  const verifyOtp = useCallback(async (otp: string) => {
    if (!state.transactionId) {
      throw new Error('No transaction ID');
    }

    setState(prev => ({ ...prev, status: 'authorizing' }));

    try {
      const response = await api.post('/api/v1/payments/verify-otp', {
        transactionId: state.transactionId,
        otp,
      });

      const backendStatus = response.data.status;
      const frontendStatus = mapBackendStatus(backendStatus);

      if (frontendStatus === 'success') {
        setState(prev => ({
          ...prev,
          status: 'success',
          error: null,
        }));
      } else {
        throw new Error('OTP verification failed');
      }

      return response.data;
    } catch (error: any) {
      const message = error.response?.data?.message || 'OTP verification failed';
      setState(prev => ({
        ...prev,
        status: 'failed',
        error: message,
      }));
      throw error;
    }
  }, [state.transactionId]);

  const pollStatus = useCallback(async (maxPolls = 15, intervalMs = 2000) => {
    if (!state.transactionId) return null;

    for (let i = 0; i < maxPolls; i++) {
      try {
        const response = await api.get(`/api/v1/payments/${state.transactionId}/status`);
        const backendStatus = response.data.status;
        const frontendStatus = mapBackendStatus(backendStatus);
        
        if (frontendStatus === 'success') {
          setState(prev => ({
            ...prev,
            status: 'success',
          }));
          return response.data;
        } else if (frontendStatus === 'failed') {
          setState(prev => ({
            ...prev,
            status: 'failed',
            error: 'Payment failed',
          }));
          return response.data;
        } else if (frontendStatus === 'expired') {
          setState(prev => ({
            ...prev,
            status: 'expired',
            error: 'Payment expired',
          }));
          return response.data;
        } else if (frontendStatus === 'pending_otp') {
          setState(prev => ({
            ...prev,
            status: 'pending_otp',
          }));
          return response.data;
        }

        await new Promise(resolve => setTimeout(resolve, intervalMs));
      } catch {
        await new Promise(resolve => setTimeout(resolve, intervalMs));
      }
    }

    setState(prev => ({
      ...prev,
      status: 'failed',
      error: 'Payment verification timed out. Please check your payment status.',
    }));
    
    return null;
  }, [state.transactionId]);

  const reset = useCallback(() => {
    if (pollTimerRef.current) {
      clearInterval(pollTimerRef.current);
      pollTimerRef.current = null;
    }
    setState({
      status: 'idle',
      error: null,
      transactionId: null,
    });
    idempotencyKeyRef.current = `txn_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }, []);

  useEffect(() => {
    return () => {
      if (pollTimerRef.current) {
        clearInterval(pollTimerRef.current);
      }
    };
  }, []);

  return {
    ...state,
    submitCardPayment,
    submitUPIPayment,
    submitNetBankingPayment,
    verifyOtp,
    pollStatus,
    reset,
  };
}
