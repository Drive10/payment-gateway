import { describe, it, expect } from 'vitest';
import {
  formatCardNumber,
  formatExpiry,
  detectCardBrand,
  isValidCardNumber,
  validateCardForm,
  formatCurrency,
} from '../../src/lib/payment';

describe('formatCardNumber', () => {
  it('should format card number with spaces every 4 digits', () => {
    expect(formatCardNumber('4242424242424242')).toBe('4242 4242 4242 4242');
  });

  it('should remove non-digit characters', () => {
    expect(formatCardNumber('4242-4242-4242-4242')).toBe('4242 4242 4242 4242');
  });

  it('should limit to 16 digits', () => {
    expect(formatCardNumber('42424242424242421234')).toBe('4242 4242 4242 4242');
  });

  it('should handle empty input', () => {
    expect(formatCardNumber('')).toBe('');
  });

  it('should handle partial card numbers', () => {
    expect(formatCardNumber('4242')).toBe('4242');
    expect(formatCardNumber('424242')).toBe('4242 42');
  });
});

describe('formatExpiry', () => {
  it('should format expiry as MM/YY', () => {
    expect(formatExpiry('1225')).toBe('12/25');
  });

  it('should limit to 4 digits', () => {
    expect(formatExpiry('12250')).toBe('12/25');
  });

  it('should remove non-digit characters', () => {
    expect(formatExpiry('12/25')).toBe('12/25');
  });

  it('should handle empty input', () => {
    expect(formatExpiry('')).toBe('');
  });

  it('should handle partial input', () => {
    expect(formatExpiry('1')).toBe('1');
    expect(formatExpiry('12')).toBe('12');
    expect(formatExpiry('123')).toBe('12/3');
  });
});

describe('detectCardBrand', () => {
  it('should detect Visa cards', () => {
    expect(detectCardBrand('4111111111111111')).toBe('Visa');
    expect(detectCardBrand('4')).toBe('Visa');
  });

  it('should detect Mastercard', () => {
    expect(detectCardBrand('5111111111111111')).toBe('Mastercard');
    expect(detectCardBrand('5211111111111111')).toBe('Mastercard');
    expect(detectCardBrand('5511111111111111')).toBe('Mastercard');
  });

  it('should detect Amex', () => {
    expect(detectCardBrand('371111111111111')).toBe('Amex');
    expect(detectCardBrand('341111111111111')).toBe('Amex');
  });

  it('should detect RuPay', () => {
    expect(detectCardBrand('6111111111111111')).toBe('RuPay');
    expect(detectCardBrand('6')).toBe('RuPay');
  });

  it('should return Card for unknown brands', () => {
    expect(detectCardBrand('9111111111111111')).toBe('Card');
    expect(detectCardBrand('')).toBe('Card');
  });
});

describe('isValidCardNumber', () => {
  it('should validate correct Visa card number (Luhn algorithm)', () => {
    expect(isValidCardNumber('4242424242424242')).toBe(true);
    expect(isValidCardNumber('4111111111111111')).toBe(true);
  });

  it('should validate correct Mastercard number', () => {
    expect(isValidCardNumber('5555555555554444')).toBe(true);
  });

  it('should invalidate incorrect card numbers', () => {
    expect(isValidCardNumber('4242424242424243')).toBe(false);
    expect(isValidCardNumber('1234567890123456')).toBe(false);
  });

  it('should invalidate card numbers with wrong length', () => {
    expect(isValidCardNumber('424242424242424')).toBe(false);
    expect(isValidCardNumber('42424242424242421')).toBe(false);
  });

  it('should handle empty input', () => {
    expect(isValidCardNumber('')).toBe(false);
  });

  it('should handle non-digit characters', () => {
    expect(isValidCardNumber('4242 4242 4242 4242')).toBe(true);
    expect(isValidCardNumber('4242-4242-4242-4242')).toBe(true);
  });
});

describe('validateCardForm', () => {
  it('should return empty errors for valid form', () => {
    const values = {
      cardNumber: '4242424242424242',
      expiry: '12/25',
      cvv: '123',
      cardholder: 'John Doe',
    };
    expect(validateCardForm(values)).toEqual({});
  });

  it('should return error for invalid card number', () => {
    const values = {
      cardNumber: '1234',
      expiry: '12/25',
      cvv: '123',
      cardholder: 'John Doe',
    };
    const errors = validateCardForm(values);
    expect(errors.cardNumber).toBeDefined();
  });

  it('should return error for invalid expiry', () => {
    const values = {
      cardNumber: '4242424242424242',
      expiry: '13/25',
      cvv: '123',
      cardholder: 'John Doe',
    };
    const errors = validateCardForm(values);
    expect(errors.expiry).toBeDefined();
  });

  it('should return error for invalid expiry format', () => {
    const values = {
      cardNumber: '4242424242424242',
      expiry: '122025',
      cvv: '123',
      cardholder: 'John Doe',
    };
    const errors = validateCardForm(values);
    expect(errors.expiry).toBeDefined();
  });

  it('should return error for invalid CVV', () => {
    const values = {
      cardNumber: '4242424242424242',
      expiry: '12/25',
      cvv: '12',
      cardholder: 'John Doe',
    };
    const errors = validateCardForm(values);
    expect(errors.cvv).toBeDefined();
  });

  it('should return error for empty cardholder name', () => {
    const values = {
      cardNumber: '4242424242424242',
      expiry: '12/25',
      cvv: '123',
      cardholder: '   ',
    };
    const errors = validateCardForm(values);
    expect(errors.cardholder).toBeDefined();
  });

  it('should return multiple errors for multiple invalid fields', () => {
    const values = {
      cardNumber: '',
      expiry: '',
      cvv: '',
      cardholder: '',
    };
    const errors = validateCardForm(values);
    expect(Object.keys(errors).length).toBe(4);
  });
});

describe('formatCurrency', () => {
  it('should format amount in INR', () => {
    expect(formatCurrency(500)).toBe('₹500');
    expect(formatCurrency(1000)).toBe('₹1,000');
    expect(formatCurrency(123456)).toBe('₹1,23,456');
  });

  it('should handle decimal amounts', () => {
    expect(formatCurrency(99.99)).toBe('₹100');
  });
});
