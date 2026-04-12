import { useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { formatCardNumber, formatExpiry, luhnCheck } from '../../lib/luhn';
import { useCardDetection } from '../../hooks/useCardDetection';
import type { CardDetails } from '../../types/payment';

interface CardInputFormProps {
  onSubmit: (data: CardDetails) => void;
  onCvvFocus: (focused: boolean) => void;
  isLoading: boolean;
}

const cardSchema = z.object({
  cardholderName: z.string()
    .min(3, 'Name must be at least 3 characters')
    .regex(/^[a-zA-Z\s]+$/, 'Name can only contain letters'),
  cardNumber: z.string()
    .min(13, 'Invalid card number')
    .max(19, 'Invalid card number')
    .refine((val) => luhnCheck(val), 'Invalid card number'),
  expiry: z.string()
    .regex(/^(0[1-9]|1[0-2])\/\d{2}$/, 'Invalid expiry (MM/YY)')
    .refine((val) => {
      const [month, year] = val.split('/').map(Number);
      const expiryDate = new Date(2000 + year, month - 1, 1);
      return expiryDate > new Date();
    }, 'Card has expired'),
  cvv: z.string()
    .regex(/^\d{3,4}$/, 'CVV must be 3 or 4 digits'),
  email: z.string()
    .email('Invalid email address'),
  phone: z.string()
    .regex(/^$|^[0-9\s+()-]{10,}$/, 'Invalid phone number')
    .optional(),
});

export function CardInputForm({ onSubmit, onCvvFocus, isLoading }: CardInputFormProps) {
  const [cvvFocused, setCvvFocused] = useState(false);
  
  const {
    register,
    handleSubmit,
    control,
    watch,
    formState: { errors },
  } = useForm<CardDetails>({
    resolver: zodResolver(cardSchema),
    defaultValues: {
      cardholderName: '',
      cardNumber: '',
      expiry: '',
      cvv: '',
      email: '',
      phone: '',
    },
  });

  const cardNumberValue = watch('cardNumber');
  const { brand } = useCardDetection(cardNumberValue);

  const handleCardNumberChange = (e: React.ChangeEvent<HTMLInputElement>, onChange: (value: string) => void) => {
    const formatted = formatCardNumber(e.target.value);
    onChange(formatted);
  };

  const handleExpiryChange = (e: React.ChangeEvent<HTMLInputElement>, onChange: (value: string) => void) => {
    const formatted = formatExpiry(e.target.value);
    onChange(formatted);
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
      <div>
        <label htmlFor="cardholderName" className="block text-sm font-medium text-gray-700 mb-1">
          Cardholder Name
        </label>
        <input
          id="cardholderName"
          type="text"
          autoComplete="cc-name"
          {...register('cardholderName')}
          className={`w-full px-4 py-3 rounded-lg border ${
            errors.cardholderName ? 'border-red-500' : 'border-gray-300'
          } focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all`}
          placeholder="John Doe"
          aria-label="Cardholder name"
          aria-invalid={!!errors.cardholderName}
        />
        {errors.cardholderName && (
          <p className="text-red-500 text-xs mt-1" role="alert">{errors.cardholderName.message}</p>
        )}
      </div>

      <div>
        <label htmlFor="cardNumber" className="block text-sm font-medium text-gray-700 mb-1">
          Card Number
        </label>
        <Controller
          name="cardNumber"
          control={control}
          render={({ field }) => (
            <input
              id="cardNumber"
              type="text"
              inputMode="numeric"
              autoComplete="cc-number"
              {...field}
              onChange={(e) => handleCardNumberChange(e, field.onChange)}
              className={`w-full px-4 py-3 rounded-lg border ${
                errors.cardNumber ? 'border-red-500' : 'border-gray-300'
              } focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all`}
              placeholder="1234 5678 9012 3456"
              maxLength={19}
              aria-label="Card number"
              aria-invalid={!!errors.cardNumber}
            />
          )}
        />
        {errors.cardNumber && (
          <p className="text-red-500 text-xs mt-1" role="alert">{errors.cardNumber.message}</p>
        )}
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label htmlFor="expiry" className="block text-sm font-medium text-gray-700 mb-1">
            Expiry Date
          </label>
          <Controller
            name="expiry"
            control={control}
            render={({ field }) => (
              <input
                id="expiry"
                type="text"
                inputMode="numeric"
                autoComplete="cc-exp"
                {...field}
                onChange={(e) => handleExpiryChange(e, field.onChange)}
                className={`w-full px-4 py-3 rounded-lg border ${
                  errors.expiry ? 'border-red-500' : 'border-gray-300'
                } focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all`}
                placeholder="MM/YY"
                maxLength={5}
                aria-label="Expiry date"
                aria-invalid={!!errors.expiry}
              />
            )}
          />
          {errors.expiry && (
            <p className="text-red-500 text-xs mt-1" role="alert">{errors.expiry.message}</p>
          )}
        </div>

        <div>
          <label htmlFor="cvv" className="block text-sm font-medium text-gray-700 mb-1">
            CVV
          </label>
          <Controller
            name="cvv"
            control={control}
            render={({ field }) => (
              <input
                id="cvv"
                type="password"
                inputMode="numeric"
                autoComplete="off"
                {...field}
                onFocus={() => {
                  setCvvFocused(true);
                  onCvvFocus(true);
                }}
                onBlur={() => {
                  setCvvFocused(false);
                  onCvvFocus(false);
                }}
                onPaste={(e) => e.preventDefault()}
                className={`w-full px-4 py-3 rounded-lg border ${
                  errors.cvv ? 'border-red-500' : 'border-gray-300'
                } focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all`}
                placeholder="•••"
                maxLength={brand === 'amex' ? 4 : 3}
                aria-label="CVV"
                aria-invalid={!!errors.cvv}
              />
            )}
          />
          {errors.cvv && (
            <p className="text-red-500 text-xs mt-1" role="alert">{errors.cvv.message}</p>
          )}
        </div>
      </div>

      <div>
        <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">
          Email Address
        </label>
        <input
          id="email"
          type="email"
          autoComplete="email"
          {...register('email')}
          className={`w-full px-4 py-3 rounded-lg border ${
            errors.email ? 'border-red-500' : 'border-gray-300'
          } focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all`}
          placeholder="john@example.com"
          aria-label="Email address"
          aria-invalid={!!errors.email}
        />
        {errors.email && (
          <p className="text-red-500 text-xs mt-1" role="alert">{errors.email.message}</p>
        )}
      </div>

      <div>
        <label htmlFor="phone" className="block text-sm font-medium text-gray-700 mb-1">
          Phone Number <span className="text-gray-400">(optional)</span>
        </label>
        <input
          id="phone"
          type="tel"
          autoComplete="tel"
          {...register('phone')}
          className={`w-full px-4 py-3 rounded-lg border ${
            errors.phone ? 'border-red-500' : 'border-gray-300'
          } focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all`}
          placeholder="+91 98765 43210"
          aria-label="Phone number (optional)"
          aria-invalid={!!errors.phone}
        />
        {errors.phone && (
          <p className="text-red-500 text-xs mt-1" role="alert">{errors.phone.message}</p>
        )}
      </div>
    </form>
  );
}
