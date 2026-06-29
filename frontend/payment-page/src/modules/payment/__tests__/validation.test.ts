import { describe, it, expect } from 'vitest';
import {
  isValidCardNumber,
  isValidExpiry,
  getCardType,
  cardFormSchema,
  upiIdSchema,
  amountSchema,
  paymentMethodSchema,
} from '../utils/validation';

describe('Validation Utilities', () => {
  describe('isValidCardNumber (Luhn)', () => {
    it('validates Visa test card', () => {
      expect(isValidCardNumber('4111111111111111')).toBe(true);
    });

    it('validates Mastercard test card', () => {
      expect(isValidCardNumber('5500000000000004')).toBe(true);
    });

    it('rejects invalid card number', () => {
      expect(isValidCardNumber('1234567890123456')).toBe(false);
    });

    it('rejects too short numbers', () => {
      expect(isValidCardNumber('123')).toBe(false);
      expect(isValidCardNumber('123456789012')).toBe(false);
    });

    it('rejects too long numbers', () => {
      expect(isValidCardNumber('123456789012345678901')).toBe(false);
    });

    it('handles empty string', () => {
      expect(isValidCardNumber('')).toBe(false);
    });
  });

  describe('isValidExpiry', () => {
    it('accepts valid future expiry', () => {
      expect(isValidExpiry('12/30')).toBe(true);
    });

    it('rejects invalid format', () => {
      expect(isValidExpiry('13/30')).toBe(false);
      expect(isValidExpiry('00/30')).toBe(false);
      expect(isValidExpiry('abcd')).toBe(false);
    });

    it('rejects past year', () => {
      expect(isValidExpiry('01/20')).toBe(false);
    });
  });

  describe('getCardType', () => {
    it('identifies Visa', () => {
      expect(getCardType('4111111111111111')).toBe('Visa');
    });

    it('identifies Mastercard', () => {
      expect(getCardType('5500000000000004')).toBe('Mastercard');
    });

    it('identifies Amex', () => {
      expect(getCardType('378282246310005')).toBe('Amex');
    });

    it('identifies Discover', () => {
      expect(getCardType('6011111111111117')).toBe('Discover');
    });

    it('returns Card for unknown', () => {
      expect(getCardType('9999999999999999')).toBe('Card');
    });
  });

  describe('cardFormSchema', () => {
    it('validates correct card data', () => {
      const result = cardFormSchema.safeParse({
        cardNumber: '4111111111111111',
        expiry: '12/30',
        cvv: '123',
        cardholder: 'John Doe',
      });
      expect(result.success).toBe(true);
    });

    it('rejects invalid card number', () => {
      const result = cardFormSchema.safeParse({
        cardNumber: 'abcd',
        expiry: '12/30',
        cvv: '123',
        cardholder: 'John Doe',
      });
      expect(result.success).toBe(false);
    });

    it('rejects missing fields', () => {
      const result = cardFormSchema.safeParse({
        cardNumber: '4111111111111111',
      });
      expect(result.success).toBe(false);
    });

    it('rejects short cardholder name', () => {
      const result = cardFormSchema.safeParse({
        cardNumber: '4111111111111111',
        expiry: '12/30',
        cvv: '123',
        cardholder: 'J',
      });
      expect(result.success).toBe(false);
    });
  });

  describe('upiIdSchema', () => {
    it('validates correct UPI ID', () => {
      const result = upiIdSchema.safeParse({ upiId: 'user@payflow' });
      expect(result.success).toBe(true);
    });

    it('validates UPI ID with dots', () => {
      const result = upiIdSchema.safeParse({ upiId: 'user.name@bank' });
      expect(result.success).toBe(true);
    });

    it('rejects empty UPI ID', () => {
      const result = upiIdSchema.safeParse({ upiId: '' });
      expect(result.success).toBe(false);
    });
  });

  describe('amountSchema', () => {
    it('validates positive amount', () => {
      const result = amountSchema.safeParse({ amount: '100.50' });
      expect(result.success).toBe(true);
    });

    it('rejects zero amount', () => {
      const result = amountSchema.safeParse({ amount: '0' });
      expect(result.success).toBe(false);
    });

    it('rejects negative amount', () => {
      const result = amountSchema.safeParse({ amount: '-50' });
      expect(result.success).toBe(false);
    });

    it('rejects empty amount', () => {
      const result = amountSchema.safeParse({ amount: '' });
      expect(result.success).toBe(false);
    });
  });

  describe('paymentMethodSchema', () => {
    it('validates card method', () => {
      expect(paymentMethodSchema.safeParse({ method: 'card' }).success).toBe(true);
    });

    it('validates upi method', () => {
      expect(paymentMethodSchema.safeParse({ method: 'upi' }).success).toBe(true);
    });

    it('rejects invalid method', () => {
      expect(paymentMethodSchema.safeParse({ method: 'bitcoin' }).success).toBe(false);
    });
  });
});
