/**
 * Card Form Component
 * 
 * Presentational component for card input
 * Separated from business logic
 */

import { useState, useCallback } from 'react';
import type { CardFormValues } from '../utils/validation';
import { cardFormSchema, isValidCardNumber, isValidExpiry, getCardType } from '../utils/validation';

interface CardFormProps {
  onSubmit: (values: CardFormValues) => void;
  isLoading?: boolean;
  disabled?: boolean;
}

export function CardForm({ onSubmit, isLoading, disabled }: CardFormProps) {
  const [values, setValues] = useState<CardFormValues>({
    cardNumber: '',
    expiry: '',
    cvv: '',
    cardholder: '',
  });
  const [errors, setErrors] = useState<Partial<Record<keyof CardFormValues, string>>({});
  const [focused, setFocused] = useState<keyof CardFormValues | null>(null);
  
  // Format helpers
  const formatCardNumber = useCallback((value: string) => {
    const digits = value.replace(/\D/g, '').slice(0, 16);
    return digits.replace(/(\d{4})(?=\d)/g, '$1 ');
  }, []);
  
  const formatExpiry = useCallback((value: string) => {
    const digits = value.replace(/\D/g, '').slice(0, 4);
    if (digits.length >= 2) {
      return `${digits.slice(0, 2)}/${digits.slice(2)}`;
    }
    return digits;
  }, []);
  
  const handleChange = useCallback((field: keyof CardFormValues, value: string) => {
    let formatted = value;
    
    if (field === 'cardNumber') {
      formatted = formatCardNumber(value);
    } else if (field === 'expiry') {
      formatted = formatExpiry(value);
    } else if (field === 'cvv') {
      formatted = value.replace(/\D/g, '').slice(0, 4);
    } else {
      formatted = value;
    }
    
    setValues(prev => ({ ...prev, [field]: formatted }));
    setErrors(prev => ({ ...prev, [field]: undefined }));
  }, [formatCardNumber, formatExpiry]);
  
  const handleSubmit = useCallback((e: React.FormEvent) => {
    e.preventDefault();
    
    const result = cardFormSchema.safeParse(values);
    
    if (!result.success) {
      const fieldErrors: Partial<Record<keyof CardFormValues, string>> = {};
      result.error.errors.forEach(err => {
        const field = err.path[0] as keyof CardFormValues;
        fieldErrors[field] = err.message;
      });
      setErrors(fieldErrors);
      return;
    }
    
    // Additional validation
    if (!isValidCardNumber(values.cardNumber)) {
      setErrors(prev => ({ ...prev, cardNumber: 'Invalid card number' }));
      return;
    }
    
    if (!isValidExpiry(values.expiry)) {
      setErrors(prev => ({ ...prev, expiry: 'Card expired or invalid' }));
      return;
    }
    
    onSubmit(values);
  }, [values, onSubmit]);
  
  const cardType = getCardType(values.cardNumber);
  const showCardType = values.cardNumber.length >= 1;
  
  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {/* Card Number */}
      <div>
        <label className="block text-sm font-medium text-slate-700 mb-1">
          Card Number
        </label>
        <div className="relative">
          <input
            type="text"
            value={values.cardNumber}
            onChange={e => handleChange('cardNumber', e.target.value)}
            onFocus={() => setFocused('cardNumber')}
            onBlur={() => setFocused(null)}
            placeholder="1234 5678 9012 3456"
            disabled={disabled || isLoading}
            className={`
              w-full px-4 py-3 rounded-xl border-2 transition-all
              font-mono text-lg tracking-wider
              ${errors.cardNumber 
                ? 'border-red-300 focus:border-red-500' 
                : 'border-slate-200 focus:border-cyan-500'}
              ${focused === 'cardNumber' ? 'ring-2 ring-cyan-500/20' : ''}
              disabled:bg-slate-50 disabled:cursor-not-allowed
            `}
          />
          {showCardType && (
            <div className="absolute right-3 top-1/2 -translate-y-1/2">
              <span className={`
                px-2 py-1 rounded-md text-xs font-medium
                ${cardType === 'Visa' ? 'bg-blue-100 text-blue-700' :
                  cardType === 'Mastercard' ? 'bg-orange-100 text-orange-700' :
                  cardType === 'Amex' ? 'bg-green-100 text-green-700' :
                  'bg-slate-100 text-slate-600'}
              `}>
                {cardType}
              </span>
            </div>
          )}
        </div>
        {errors.cardNumber && (
          <p className="mt-1 text-sm text-red-600">{errors.cardNumber}</p>
        )}
      </div>
      
      {/* Expiry and CVV Row */}
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">
            Expiry
          </label>
          <input
            type="text"
            value={values.expiry}
            onChange={e => handleChange('expiry', e.target.value)}
            onFocus={() => setFocused('expiry')}
            onBlur={() => setFocused(null)}
            placeholder="MM/YY"
            disabled={disabled || isLoading}
            className={`
              w-full px-4 py-3 rounded-xl border-2 transition-all font-mono
              ${errors.expiry 
                ? 'border-red-300 focus:border-red-500' 
                : 'border-slate-200 focus:border-cyan-500'}
              ${focused === 'expiry' ? 'ring-2 ring-cyan-500/20' : ''}
              disabled:bg-slate-50 disabled:cursor-not-allowed
            `}
          />
          {errors.expiry && (
            <p className="mt-1 text-sm text-red-600">{errors.expiry}</p>
          )}
        </div>
        
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">
            CVV
          </label>
          <input
            type="password"
            value={values.cvv}
            onChange={e => handleChange('cvv', e.target.value)}
            placeholder="•••"
            disabled={disabled || isLoading}
            maxLength={4}
            className={`
              w-full px-4 py-3 rounded-xl border-2 transition-all font-mono
              ${errors.cvv 
                ? 'border-red-300 focus:border-red-500' 
                : 'border-slate-200 focus:border-cyan-500'}
              disabled:bg-slate-50 disabled:cursor-not-allowed
            `}
          />
          {errors.cvv && (
            <p className="mt-1 text-sm text-red-600">{errors.cvv}</p>
          )}
        </div>
      </div>
      
      {/* Cardholder Name */}
      <div>
        <label className="block text-sm font-medium text-slate-700 mb-1">
          Cardholder Name
        </label>
        <input
          type="text"
          value={values.cardholder}
          onChange={e => handleChange('cardholder', e.target.value)}
          placeholder="Name on card"
          disabled={disabled || isLoading}
          className={`
            w-full px-4 py-3 rounded-xl border-2 transition-all
            ${errors.cardholder 
              ? 'border-red-300 focus:border-red-500' 
              : 'border-slate-200 focus:border-cyan-500'}
            disabled:bg-slate-50 disabled:cursor-not-allowed
          `}
        />
        {errors.cardholder && (
          <p className="mt-1 text-sm text-red-600">{errors.cardholder}</p>
        )}
      </div>
      
      <button
        type="submit"
        disabled={disabled || isLoading}
        className={`
          w-full py-4 rounded-xl font-semibold text-lg
          bg-gradient-to-r from-cyan-600 to-teal-600 text-white
          shadow-lg shadow-cyan-500/30
          hover:shadow-xl hover:shadow-cyan-500/40
          transition-all
          disabled:opacity-50 disabled:cursor-not-allowed
          disabled:shadow-none
        `}
      >
        {isLoading ? (
          <span className="flex items-center justify-center gap-2">
            <svg className="h-5 w-5 animate-spin" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
            Processing...
          </span>
        ) : (
          'Pay securely'
        )}
      </button>
    </form>
  );
}