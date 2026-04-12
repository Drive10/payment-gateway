import { useCallback } from 'react';
import { signPayload } from '../lib/hmac';

interface PaymentSecurityOptions {
  merchantId: string;
}

export function usePaymentSecurity({ merchantId }: PaymentSecurityOptions) {
  const generateToken = useCallback(async (cardDetails: {
    cardNumber: string;
    expiry: string;
    cvv: string;
  }): Promise<string> => {
    const payload = {
      cardNumber: cardDetails.cardNumber.replace(/\s/g, '').slice(-4),
      expiry: cardDetails.expiry,
      merchantId,
      timestamp: Date.now(),
    };
    
    const signature = await signPayload(payload);
    
    const token = btoa(JSON.stringify({
      ...payload,
      signature,
    }));
    
    return token;
  }, [merchantId]);

  const clearSensitiveData = useCallback((data: {
    cardNumber?: string;
    cvv?: string;
  }) => {
    if (data.cardNumber) {
      data.cardNumber = '';
    }
    if (data.cvv) {
      data.cvv = '';
    }
  }, []);

  return {
    generateToken,
    clearSensitiveData,
  };
}
