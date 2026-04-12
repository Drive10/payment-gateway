import { useState, useRef, useCallback } from 'react';
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

      const { transactionId, status } = response.data;

      if (status === 'PENDING_OTP') {
        setState({
          status: 'pending_otp',
          error: null,
          transactionId,
        });
      } else if (status === 'COMPLETED') {
        clearSensitiveData({ cardNumber: cardDetails.cardNumber, cvv: cardDetails.cvv });
        setState({
          status: 'success',
          error: null,
          transactionId,
        });
      } else {
        throw new Error('Payment failed');
      }

      return { transactionId, status };
    } catch (error: any) {
      const message = error.response?.data?.message || 'Payment failed. Please try again.';
      setState(prev => ({
        ...prev,
        status: 'failed',
        error: message,
      }));
      throw error;
    }
  }, [amount, currency, merchantId, generateToken, clearSensitiveData]);

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

      const { transactionId, status } = response.data;

      if (status === 'PENDING_OTP') {
        setState({
          status: 'pending_otp',
          error: null,
          transactionId,
        });
      } else if (status === 'COMPLETED') {
        setState({
          status: 'success',
          error: null,
          transactionId,
        });
      }

      return { transactionId, status };
    } catch (error: any) {
      const message = error.response?.data?.message || 'Payment failed. Please try again.';
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

      const { transactionId, status } = response.data;

      if (status === 'PENDING_OTP') {
        setState({
          status: 'pending_otp',
          error: null,
          transactionId,
        });
      } else if (status === 'COMPLETED') {
        setState({
          status: 'success',
          error: null,
          transactionId,
        });
      }

      return { transactionId, status };
    } catch (error: any) {
      const message = error.response?.data?.message || 'Payment failed. Please try again.';
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

    setState(prev => ({ ...prev, status: 'processing' }));

    try {
      const response = await api.post('/api/v1/payments/verify-otp', {
        transactionId: state.transactionId,
        otp,
      });

      if (response.data.status === 'COMPLETED') {
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

  const pollStatus = useCallback(async (maxPolls = 10, intervalMs = 2000) => {
    if (!state.transactionId) return null;

    for (let i = 0; i < maxPolls; i++) {
      try {
        const response = await api.get(`/api/v1/payments/${state.transactionId}/status`);
        
        if (response.data.status === 'COMPLETED') {
          setState(prev => ({
            ...prev,
            status: 'success',
          }));
          return response.data;
        } else if (response.data.status === 'FAILED') {
          setState(prev => ({
            ...prev,
            status: 'failed',
            error: 'Payment failed',
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
      error: 'Payment verification timed out',
    }));
    
    return null;
  }, [state.transactionId]);

  const reset = useCallback(() => {
    setState({
      status: 'idle',
      error: null,
      transactionId: null,
    });
    idempotencyKeyRef.current = `txn_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
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
