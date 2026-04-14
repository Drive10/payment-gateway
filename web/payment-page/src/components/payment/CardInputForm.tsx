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
  email: z.string()
    .email('Invalid email address'),
  phone: z.string()
    .regex(/^$|^[0-9\s+()-]{10,}$/, 'Invalid phone number')
    .optional(),
});

export function CardInputForm({ onSubmit, onCvvFocus, isLoading }: CardInputFormProps) {
  const [tokenizedCard, setTokenizedCard] = useState<string | null>(null);
  const [tokenizing, setTokenizing] = useState(false);
  
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

  const handleTokenizeAndSubmit = async (data: CardDetails) => {
    if (!data.cardNumber || !data.expiry || !data.cvv) {
      return;
    }

    setTokenizing(true);
    try {
      const token = await tokenizeCard({
        cardNumber: data.cardNumber.replace(/\s/g, ''),
        expiry: data.expiry,
        cvv: data.cvv,
      });
      
      setTokenizedCard(token);
      
      onSubmit({
        ...data,
        cardNumber: `tok_${token.slice(0, 16)}XXXX`,
        cvv: '***',
      });
    } catch (error) {
      console.error('Card tokenization failed:', error);
    } finally {
      setTokenizing(false);
    }
  };

  const tokenizeCard = async (card: { cardNumber: string; expiry: string; cvv: string }): Promise<string> => {
    const payload = {
      card_number: card.cardNumber,
      expiry_month: card.expiry.split('/')[0],
      expiry_year: '20' + card.expiry.split('/')[1],
    };
    
    const response = await fetch('/api/v1/payments/tokenize', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    
    if (!response.ok) {
      throw new Error('Tokenization failed');
    }
    
    const result = await response.json();
    return result.token;
  };

  const showCardForm = !tokenizedCard;

  return (
    <form onSubmit={handleSubmit(handleTokenizeAndSubmit)} className="space-y-4" noValidate>
      {showCardForm ? (
        <>
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 mb-4">
            <p className="text-sm text-blue-700">
              🔒 Your card details are securely tokenized. We never see or store your full card number.
            </p>
          </div>

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
                />
              )}
            />
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
                  />
                )}
              />
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
                    onFocus={() => onCvvFocus(true)}
                    onBlur={() => onCvvFocus(false)}
                    onPaste={(e) => e.preventDefault()}
                    className={`w-full px-4 py-3 rounded-lg border ${
                      errors.cvv ? 'border-red-500' : 'border-gray-300'
                    } focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all`}
                    placeholder="•••"
                    maxLength={brand === 'amex' ? 4 : 3}
                    aria-label="CVV"
                  />
                )}
              />
            </div>
          </div>
        </>
      ) : (
        <div className="bg-green-50 border border-green-200 rounded-lg p-4">
          <div className="flex items-center gap-2">
            <svg className="w-5 h-5 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
            </svg>
            <span className="text-green-700 font-medium">Card tokenized successfully</span>
          </div>
          <p className="text-sm text-green-600 mt-1">
            Your card ending in {tokenizedCard?.slice(-4)} is ready for payment.
          </p>
          <button
            type="button"
            onClick={() => setTokenizedCard(null)}
            className="text-sm text-green-700 underline mt-2"
          >
            Use different card
          </button>
        </div>
      )}

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

      {showCardForm && (
        <button
          type="submit"
          disabled={isLoading || tokenizing}
          className="w-full py-4 px-6 rounded-xl font-semibold text-lg bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-50"
        >
          {tokenizing ? 'Tokenizing...' : 'Continue to Payment'}
        </button>
      )}
    </form>
  );
}
