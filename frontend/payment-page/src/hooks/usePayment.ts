import { useState, useRef, useCallback, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
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

const TXN_PARAM = 'txn';
const ERROR_PARAM = 'error';

const BACKEND_STATUS_TO_FRONTEND: Record<string, PaymentStatus> = {
  'PENDING': 'initiated',
  'CREATED': 'initiated',
  'AWAITING_UPI_PAYMENT': 'pending',  // UPI: waiting for user to pay
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
  const [searchParams, setSearchParams] = useSearchParams();
  
  const initialTxnId = searchParams.get(TXN_PARAM);
  const initialError = searchParams.get(ERROR_PARAM);
  
  const [state, setState] = useState<PaymentState>(() => ({
    status: initialTxnId ? 'initiated' : 'idle',
    error: initialError,
    transactionId: initialTxnId,
  }));

  const pollTimerRef = useRef<NodeJS.Timeout | null>(null);
  const idempotencyKeyRef = useRef<string>(
    initialTxnId || `txn_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
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

      const payload = response.data?.data ?? response.data;
      const { transactionId, paymentId, status: backendStatus, errorCode, message } = payload;
      const resolvedTxnId = transactionId || paymentId;
      const frontendStatus = mapBackendStatus(backendStatus);

      setState({
        status: frontendStatus,
        error: frontendStatus === 'failed' ? (message || 'Payment failed') : null,
        transactionId: resolvedTxnId,
      });

      if (resolvedTxnId && frontendStatus !== 'failed') {
        setSearchParams({ [TXN_PARAM]: resolvedTxnId });
      }

      return { transactionId: resolvedTxnId, status: frontendStatus };
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

      const payload = response.data?.data ?? response.data;
      const { transactionId, paymentId, status: backendStatus, checkoutUrl, message } = payload;
      const resolvedTxnId = transactionId || paymentId;
      const frontendStatus = mapBackendStatus(backendStatus);

      // For UPI payments, redirect to UPI app if deep link provided
      if (checkoutUrl && checkoutUrl.startsWith('upi://')) {
        window.location.href = checkoutUrl;
      }

      setState({
        status: frontendStatus,
        error: frontendStatus === 'failed' ? (message || 'Payment failed') : null,
        transactionId: resolvedTxnId,
      });

      // For UPI, automatically start polling if in awaiting state
      if (frontendStatus === 'initiated' || frontendStatus === 'pending') {
        // Return transaction ID and let caller handle navigation to processing page
        // The Processing page will handle polling
      }

      return { transactionId: resolvedTxnId, status: frontendStatus, checkoutUrl };
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

      const payload = response.data?.data ?? response.data;
      const { transactionId, paymentId, status: backendStatus, message } = payload;
      const resolvedTxnId = transactionId || paymentId;
      const frontendStatus = mapBackendStatus(backendStatus);

      setState({
        status: frontendStatus,
        error: frontendStatus === 'failed' ? (message || 'Payment failed') : null,
        transactionId: resolvedTxnId,
      });

      return { transactionId: resolvedTxnId, status: frontendStatus };
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

      const backendStatus = (response.data?.data ?? response.data).status;
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

      return response.data?.data ?? response.data;
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
        const payload = response.data?.data ?? response.data;
        const backendStatus = payload.status;
        const frontendStatus = mapBackendStatus(backendStatus);
        
        if (frontendStatus === 'success') {
          setState(prev => ({
            ...prev,
            status: 'success',
          }));
          return payload;
        } else if (frontendStatus === 'failed') {
          setState(prev => ({
            ...prev,
            status: 'failed',
            error: 'Payment failed',
          }));
          return payload;
        } else if (frontendStatus === 'expired') {
          setState(prev => ({
            ...prev,
            status: 'expired',
            error: 'Payment expired',
          }));
          return payload;
        } else if (frontendStatus === 'pending_otp') {
          setState(prev => ({
            ...prev,
            status: 'pending_otp',
          }));
          return payload;
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
    setSearchParams({});
    idempotencyKeyRef.current = `txn_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }, [setSearchParams]);

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
