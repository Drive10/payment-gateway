/**
 * Form Validation Schemas
 * 
 * Uses Zod for type-safe validation
 */

import { z } from 'zod';

/**
 * Card form validation schema
 */
export const cardFormSchema = z.object({
  cardNumber: z
    .string()
    .min(13, 'Card number must be at least 13 digits')
    .max(19, 'Card number too long')
    .regex(/^\d+$/, 'Card number must contain only digits'),
  expiry: z
    .string()
    .regex(/^(0[1-9]|1[0-2])\/\d{2}$/, 'Use MM/YY format'),
  cvv: z
    .string()
    .regex(/^\d{3,4}$/, 'CVV must be 3 or 4 digits'),
  cardholder: z
    .string()
    .min(2, 'Cardholder name required')
    .max(50, 'Name too long')
    .regex(/^[a-zA-Z\s]+$/, 'Name must contain only letters'),
});

export type CardFormValues = z.infer<typeof cardFormSchema>;

/**
 * Amount input validation
 */
export const amountSchema = z.object({
  amount: z
    .string()
    .min(1, 'Amount required')
    .refine(
      val => !isNaN(parseFloat(val)) && parseFloat(val) > 0,
      'Amount must be greater than 0'
    ),
});

export type AmountValues = z.infer<typeof amountSchema>;

/**
 * Payment method selection
 */
export const paymentMethodSchema = z.object({
  method: z.enum(['card', 'upi', 'netbanking', 'wallet']),
});

export type MethodValues = z.infer<typeof paymentMethodSchema>;

/**
 * UPI ID validation
 * Format: username@bankname
 */
export const upiIdSchema = z.object({
  upiId: z
    .string()
    .regex(/^[a-zA-Z0-9.@]+$/, 'Invalid UPI ID format')
    .regex(/^.+$/, 'UPI ID required')
    .max(50, 'UPI ID too long'),
});

export type UpiValues = z.infer<typeof upiIdSchema>;

/**
 * Full checkout form validation
 */
export const checkoutFormSchema = z.object({
  amount: amountSchema.shape.amount,
  method: paymentMethodSchema.shape.method,
  card: cardFormSchema.optional(),
  upi: upiIdSchema.optional(),
  netbanking: z.object({
    bankCode: z.string().min(1, 'Select a bank'),
  }).optional(),
  wallet: z.object({
    wallet: z.string().min(1, 'Select a wallet'),
  }).optional(),
});

export type CheckoutFormValues = z.infer<typeof checkoutFormSchema>;

/**
 * Helper to validate card number with Luhn
 */
export function isValidCardNumber(cardNumber: string): boolean {
  const digits = cardNumber.replace(/\D/g, '');
  
  if (digits.length < 13 || digits.length > 19) {
    return false;
  }
  
  // Luhn algorithm
  let sum = 0;
  let isEven = false;
  
  for (let i = digits.length - 1; i >= 0; i--) {
    let digit = parseInt(digits[i]);
    
    if (isEven) {
      digit *= 2;
      if (digit > 9) {
        digit -= 9;
      }
    }
    
    sum += digit;
    isEven = !isEven;
  }
  
  return sum % 10 === 0;
}

/**
 * Helper to validate expiry
 * Checks both format MM/YY and ensures not expired
 */
export function isValidExpiry(expiry: string): boolean {
  const match = expiry.match(/^(0[1-9]|1[0-2])\/(\d{2})$/);
  if (!match) return false;
  
  const month = parseInt(match[1]);
  const year = parseInt('20' + match[2]);
  const now = new Date();
  const currentYear = now.getFullYear();
  const currentMonth = now.getMonth() + 1;
  
  if (year < currentYear) return false;
  if (year === currentYear && month < currentMonth) return false;
  
  return true;
}

/**
 * Get card type from number
 */
export function getCardType(cardNumber: string): string {
  const digits = cardNumber.replace(/\D/g, '');
  
  if (/^4/.test(digits)) return 'Visa';
  if (/^5[1-5]/.test(digits)) return 'Mastercard';
  if (/^3[47]/.test(digits)) return 'Amex';
  if (/^6(?:011|5)/.test(digits)) return 'Discover';
  
  return 'Card';
}