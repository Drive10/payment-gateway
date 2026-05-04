/**
 * Payment State Machine
 * 
 * Implements a proper state machine for payment flows
 * with persistence and validation
 */

import { useCallback, useEffect, useRef, useState } from 'react';
import type {
  PaymentTransaction,
  PaymentState,
  PaymentMethodType,
  TransactionMode,
  CreateOrderRequest,
} from '../types/payment';
import { canTransitionTo, isTerminalState, isErrorState } from '../types/payment';
import * as api from '../api/paymentApi';

// Storage key
const STORAGE_KEY = 'payflow-payment-transaction';

/**
 * Create initial transaction
 */
function createInitialTransaction(
  amount: number,
  currency: string,
  method: PaymentMethodType,
  request?: Partial<CreateOrderRequest>
): PaymentTransaction {
  const now = Date.now();
  return {
    id: `pay_${now}_${Math.random().toString(36).slice(2, 10)}`,
    orderId: request?.orderId || `ORD-${now.toString(36).toUpperCase()}`,
    amount,
    currency: currency || 'INR',
    state: 'IDLE',
    method,
    mode: import.meta.env.MODE === 'production' ? 'PRODUCTION' : 'TEST',
    createdAt: now,
    description: request?.description,
    customerEmail: request?.customerEmail,
  };
}

/**
 * Persist transaction to storage
 */
function persistTransaction(transaction: PaymentTransaction | null): void {
  if (transaction) {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(transaction));
  } else {
    sessionStorage.removeItem(STORAGE_KEY);
  }
}

/**
 * Load transaction from storage
 */
function loadTransaction(): PaymentTransaction | null {
  try {
    const stored = sessionStorage.getItem(STORAGE_KEY);
    return stored ? JSON.parse(stored) : null;
  } catch {
    return null;
  }
}

/**
 * Payment State Machine Hook
 * 
 * Provides state management for complete payment lifecycle
 */
export function usePaymentMachine() {
  const [transaction, setTransaction] = useState<PaymentTransaction | null>(() => 
    loadTransaction()
  );
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const pollAbortRef = useRef<AbortController | null>(null);

  /**
   * Start new payment
   */
  const startPayment = useCallback(async (
    amount: number,
    method: PaymentMethodType = 'card',
    request?: Partial<CreateOrderRequest>
  ) => {
    setIsLoading(true);
    setError(null);
    
    try {
      // Create initial state
      const tx = createInitialTransaction(
        amount,
        request?.currency || 'INR',
        method,
        request
      );
      tx.state = 'CREATED';
      setTransaction(tx);
      persistTransaction(tx);
      
      // Call backend to create order
      const response = await api.createOrder({
        amount,
        currency: tx.currency,
        paymentMethod: method,
        description: request?.description,
        customerEmail: request?.customerEmail,
        ...request,
      });
      
      // Update with backend response
      const updatedTx: PaymentTransaction = {
        ...tx,
        id: response.paymentId,
        orderId: response.orderId,
        clientSecret: response.clientSecret,
        state: response.status,
        checkoutUrl: response.checkoutUrl,
      };
      
      setTransaction(updatedTx);
      persistTransaction(updatedTx);
      
      return updatedTx;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to start payment';
      setError(message);
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Capture payment (for card after OTP)
   */
  const capturePayment = useCallback(async () => {
    if (!transaction) {
      throw new Error('No payment in progress');
    }
    
    if (!canTransitionTo(transaction.state, 'CAPTURED')) {
      throw new Error(`Cannot capture payment from state: ${transaction.state}`);
    }
    
    setIsLoading(true);
    setError(null);
    
    try {
      setTransaction(tx => tx ? { ...tx, state: 'PROCESSING' } : null);
      
      const response = await api.capturePayment(transaction.id);
      
      const updatedTx: PaymentTransaction = {
        ...transaction,
        state: response.status,
        providerReference: response.providerReference,
        capturedAt: Date.now(),
      };
      
      setTransaction(updatedTx);
      persistTransaction(updatedTx);
      
      return updatedTx;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to capture payment';
      setError(message);
      
      // Mark as failed
      setTransaction(tx => tx ? { 
        ...tx, 
        state: 'FAILED',
        providerError: message,
      } : null);
      
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, [transaction]);

  /**
   * Verify OTP
   */
  const verifyOtp = useCallback(async (otp: string) => {
    if (!transaction) {
      throw new Error('No payment in progress');
    }
    
    if (transaction.method !== 'card') {
      throw new Error('OTP verification only for card payments');
    }
    
    setIsLoading(true);
    setError(null);
    
    try {
      const response = await api.verifyOtp(transaction.id, otp);
      
      if (!response.success) {
        throw new Error(response.error || 'OTP verification failed');
      }
      
      const updatedTx: PaymentTransaction = {
        ...transaction,
        state: response.status,
      };
      
      setTransaction(updatedTx);
      persistTransaction(updatedTx);
      
      return updatedTx;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Verification failed';
      setError(message);
      
      // Mark as failed
      if (transaction) {
        setTransaction(tx => tx ? { 
          ...tx, 
          state: 'FAILED',
          providerError: message,
        } : null);
      }
      
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, [transaction]);

  /**
   * Poll payment status
   */
  const pollStatus = useCallback(async (
    onStatusChange?: (state: PaymentState) => void,
    maxAttempts = 30,
    interval = 3000
  ) => {
    if (!transaction) {
      return;
    }
    
    pollAbortRef.current = new AbortController();
    
    for (let attempt = 0; attempt < maxAttempts; attempt++) {
      if (pollAbortRef.current.signal.aborted) {
        break;
      }
      
      try {
        const status = await api.getPaymentStatus(transaction.id);
        
        if (status.status !== transaction.state) {
          const updatedTx: PaymentTransaction = {
            ...transaction,
            state: status.status,
          };
          
          setTransaction(updatedTx);
          persistTransaction(updatedTx);
          
          onStatusChange?.(status.status);
          
          // Stop polling on terminal state
          if (isTerminalState(status.status)) {
            break;
          }
        }
      } catch {
        // Continue polling on error
      }
      
      await new Promise(r => setTimeout(r, interval));
    }
  }, [transaction]);

  /**
   * Stop polling
   */
  const stopPolling = useCallback(() => {
    pollAbortRef.current?.abort();
  }, []);

  /**
   * Reset/cancel payment
   */
  const reset = useCallback(() => {
    stopPolling();
    setTransaction(null);
    setError(null);
    persistTransaction(null);
  }, [stopPolling]);

  /**
   * Clear error
   */
  const clearError = useCallback(() => {
    setError(null);
  }, []);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      pollAbortRef.current?.abort();
    };
  }, []);

  return {
    // State
    transaction,
    isLoading,
    error,
    isActive: transaction !== null && !isTerminalState(transaction.state),
    isTerminal: transaction ? isTerminalState(transaction.state) : true,
    isError: transaction ? isErrorState(transaction.state) : false,
    
    // Actions
    startPayment,
    capturePayment,
    verifyOtp,
    pollStatus,
    stopPolling,
    reset,
    clearError,
    
    // Current state helpers
    state: transaction?.state || 'IDLE',
    method: transaction?.method || 'card',
    amount: transaction?.amount || 0,
  };
}