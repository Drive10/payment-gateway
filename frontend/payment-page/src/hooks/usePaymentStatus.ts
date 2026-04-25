import { useState, useEffect, useRef } from 'react';
import { paymentService } from '../services/paymentService';
import { PaymentStatus, PaymentStatusResponse } from '../types/payment';

export function usePaymentStatus(orderId: string | null, interval = 3000) {
  const [status, setStatus] = useState<PaymentStatus | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [transactionId, setTransactionId] = useState<string | null>(null);
  const timerRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    if (!orderId) return;

    const pollStatus = async () => {
      try {
        const response: PaymentStatusResponse = await paymentService.getPaymentStatus(orderId);
        setStatus(response.status);
        if (response.transactionId) {
          setTransactionId(response.transactionId);
        }
        
        if (response.status === 'SUCCESS' || response.status === 'FAILURE' || response.status === 'EXPIRED') {
          if (timerRef.current) clearInterval(timerRef.current);
        }
      } catch (err: any) {
        setError(err.response?.data?.message || 'Failed to fetch payment status');
      }
    };

    pollStatus();
    timerRef.current = setInterval(pollStatus, interval);

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [orderId, interval]);

  return { status, error, transactionId };
}
