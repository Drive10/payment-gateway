import { useMemo } from 'react';
import { detectCardBrand } from '../lib/luhn';

interface UseCardDetectionResult {
  brand: 'visa' | 'mastercard' | 'amex' | 'rupay' | 'unknown';
  isValidLength: boolean;
  isValid: boolean;
}

export function useCardDetection(cardNumber: string): UseCardDetectionResult {
  const digits = cardNumber.replace(/\D/g, '');
  
  const result = useMemo(() => {
    const brand = detectCardBrand(cardNumber);
    
    let isValidLength = false;
    switch (brand) {
      case 'amex':
        isValidLength = digits.length === 15;
        break;
      case 'visa':
      case 'mastercard':
      case 'rupay':
        isValidLength = digits.length >= 13 && digits.length <= 19;
        break;
      default:
        isValidLength = digits.length >= 13 && digits.length <= 19;
    }
    
    const isValid = isValidLength && digits.length > 0;
    
    return { brand, isValidLength, isValid };
  }, [cardNumber, digits]);
  
  return result;
}
