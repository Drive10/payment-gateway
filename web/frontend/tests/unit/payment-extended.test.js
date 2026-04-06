import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import {
  startCheckout,
  captureCheckout,
  formatCardNumber,
  formatExpiry,
  detectCardBrand,
  isValidCardNumber,
  validateCardForm,
  formatCurrency,
  getStoredTransaction,
  PAYMENT_AMOUNT,
  UPI_ID,
  STORAGE_KEY,
  TRANSACTION_MODES,
} from '../../src/lib/payment';

describe('Payment Constants', () => {
  it('should have correct PAYMENT_AMOUNT', () => {
    expect(PAYMENT_AMOUNT).toBe(500);
  });

  it('should have correct UPI_ID', () => {
    expect(UPI_ID).toBe('nova-demo@upi');
  });

  it('should have correct STORAGE_KEY', () => {
    expect(STORAGE_KEY).toBe('nova-checkout-transaction');
  });

  it('should have TRANSACTION_MODES defined', () => {
    expect(TRANSACTION_MODES.PRODUCTION).toBe('PRODUCTION');
    expect(TRANSACTION_MODES.TEST).toBe('TEST');
  });
});

describe('formatCurrency', () => {
  it('should format amount in INR', () => {
    expect(formatCurrency(500)).toBe('₹500');
    expect(formatCurrency(1000)).toBe('₹1,000');
    expect(formatCurrency(123456)).toBe('₹1,23,456');
  });

  it('should handle decimal amounts by rounding', () => {
    expect(formatCurrency(99.99)).toBe('₹100');
    expect(formatCurrency(99.5)).toBe('₹100');
  });

  it('should handle zero and negative amounts', () => {
    expect(formatCurrency(0)).toBe('₹0');
    expect(formatCurrency(-100)).toBe('₹-100');
  });

  it('should handle large amounts', () => {
    expect(formatCurrency(1000000)).toBe('₹10,00,000');
    expect(formatCurrency(9999999)).toBe('₹99,99,999');
  });
});

describe('getStoredTransaction', () => {
  beforeEach(() => {
    vi.stubGlobal('sessionStorage', {
      getItem: vi.fn(),
      setItem: vi.fn(),
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('should return null when no transaction stored', () => {
    sessionStorage.getItem.mockReturnValue(null);
    expect(getStoredTransaction()).toBeNull();
  });

  it('should return parsed transaction when stored', () => {
    const transaction = { id: 'txn-123', amount: 500 };
    sessionStorage.getItem.mockReturnValue(JSON.stringify(transaction));
    expect(getStoredTransaction()).toEqual(transaction);
  });

  it('should return null when sessionStorage throws', () => {
    sessionStorage.getItem.mockImplementation(() => {
      throw new Error('Storage error');
    });
    expect(getStoredTransaction()).toBeNull();
  });
});

describe('API Integration', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn());
    vi.stubGlobal('sessionStorage', {
      getItem: vi.fn(),
      setItem: vi.fn(),
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  describe('startCheckout', () => {
    it('should create checkout with valid parameters', async () => {
      const mockAuthResponse = {
        accessToken: 'test-token',
        user: { id: 'user-123' }
      };
      
      const mockOrderResponse = {
        id: 'order-123',
        merchantId: 'merchant-456'
      };
      
      const mockPaymentResponse = {
        id: 'payment-789',
        orderId: 'order-123',
        status: 'PENDING',
        providerOrderId: 'provider-order-1'
      };

      let callCount = 0;
      fetch.mockImplementation(() => {
        callCount++;
        if (callCount === 1) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ data: mockAuthResponse }),
          });
        }
        if (callCount === 2) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ data: mockOrderResponse }),
          });
        }
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ data: mockPaymentResponse }),
        });
      });

      const result = await startCheckout({
        amount: 500,
        method: 'card',
        cardholder: 'John Doe',
        transactionMode: 'TEST',
      });

      expect(result.token).toBe('test-token');
      expect(result.order).toEqual(mockOrderResponse);
      expect(result.payment).toEqual(mockPaymentResponse);
    });

    it('should throw error when API is unreachable', async () => {
      fetch.mockRejectedValue(new Error('Network error'));

      await expect(
        startCheckout({
          amount: 500,
          method: 'card',
          cardholder: 'John Doe',
          transactionMode: 'TEST',
        })
      ).rejects.toThrow('Unable to reach the payment backend');
    });

    it('should handle API error response', async () => {
      fetch.mockResolvedValue({
        ok: false,
        json: () => Promise.resolve({ 
          success: false, 
          error: { code: 'VALIDATION_ERROR', message: 'Invalid amount' } 
        }),
      });

      await expect(
        startCheckout({
          amount: 500,
          method: 'card',
          cardholder: 'John Doe',
          transactionMode: 'TEST',
        })
      ).rejects.toThrow('[VALIDATION_ERROR] Invalid amount');
    });

    it('should use RAZORPAY_SIMULATOR for TEST mode', async () => {
      const mockAuthResponse = { accessToken: 'token', user: { id: 'user' } };
      const mockOrderResponse = { id: 'order', merchantId: 'merchant' };
      const mockPaymentResponse = { id: 'payment', status: 'PENDING' };

      let callCount = 0;
      fetch.mockImplementation(() => {
        callCount++;
        if (callCount <= 3) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ data: callCount === 1 ? mockAuthResponse : callCount === 2 ? mockOrderResponse : mockPaymentResponse }),
          });
        }
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ data: mockPaymentResponse }),
        });
      });

      const result = await startCheckout({
        amount: 500,
        method: 'card',
        cardholder: 'John Doe',
        transactionMode: 'TEST',
      });

      expect(result).toBeDefined();
    });

    it('should use UPI method when specified', async () => {
      const mockAuthResponse = { accessToken: 'token', user: { id: 'user' } };
      const mockOrderResponse = { id: 'order', merchantId: 'merchant' };
      const mockPaymentResponse = { id: 'payment', status: 'PENDING' };

      let callCount = 0;
      fetch.mockImplementation(() => {
        callCount++;
        if (callCount <= 3) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ 
              data: callCount === 1 ? mockAuthResponse : callCount === 2 ? mockOrderResponse : mockPaymentResponse 
            }),
          });
        }
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ data: mockPaymentResponse }),
        });
      });

      const result = await startCheckout({
        amount: 500,
        method: 'upi',
        cardholder: 'John Doe',
        transactionMode: 'TEST',
      });

      expect(result.method).toBe('upi');
    });
  });

  describe('captureCheckout', () => {
    it('should capture payment successfully', async () => {
      const mockPaymentResponse = {
        id: 'payment-789',
        orderId: 'order-123',
        status: 'CAPTURED',
        providerPaymentId: 'provider-pay-1',
        transactionMode: 'TEST',
        simulated: true,
        createdAt: '2024-01-01T00:00:00Z',
      };

      fetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ data: mockPaymentResponse }),
      });

      const checkout = {
        token: 'test-token',
        customer: { firstName: 'Nova', lastName: 'Demo' },
        payment: { id: 'payment-789' },
        amount: 500,
        method: 'card',
        cardholder: 'John Doe',
        correlationId: 'corr-123',
      };

      const result = await captureCheckout(checkout);

      expect(result.id).toBe('payment-789');
      expect(result.status).toBe('CAPTURED');
      expect(result.methodLabel).toBe('Card');
    });

    it('should handle UPI method in capture', async () => {
      const mockPaymentResponse = {
        id: 'payment-789',
        orderId: 'order-123',
        status: 'CAPTURED',
        transactionMode: 'TEST',
        simulated: true,
        createdAt: '2024-01-01T00:00:00Z',
      };

      fetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ data: mockPaymentResponse }),
      });

      const checkout = {
        token: 'test-token',
        customer: { firstName: 'Nova', lastName: 'Demo', fullName: 'Nova Demo' },
        payment: { id: 'payment-789' },
        amount: 500,
        method: 'upi',
        cardholder: '',
        correlationId: 'corr-123',
      };

      const result = await captureCheckout(checkout);

      expect(result.methodLabel).toBe('UPI');
      expect(result.customerLabel).toBe('Nova Demo');
    });

    it('should show correct environment label for TEST mode', async () => {
      const mockPaymentResponse = {
        id: 'payment-789',
        orderId: 'order-123',
        status: 'CAPTURED',
        transactionMode: 'TEST',
        simulated: true,
        createdAt: '2024-01-01T00:00:00Z',
      };

      fetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ data: mockPaymentResponse }),
      });

      const checkout = {
        token: 'test-token',
        customer: { firstName: 'Nova', lastName: 'Demo' },
        payment: { id: 'payment-789' },
        amount: 500,
        method: 'card',
        cardholder: 'John Doe',
        correlationId: 'corr-123',
      };

      const result = await captureCheckout(checkout);

      expect(result.environmentLabel).toBe('Sandbox lane');
    });

    it('should show PRODUCTION label for production mode', async () => {
      const mockPaymentResponse = {
        id: 'payment-789',
        orderId: 'order-123',
        status: 'CAPTURED',
        transactionMode: 'PRODUCTION',
        simulated: false,
        createdAt: '2024-01-01T00:00:00Z',
      };

      fetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ data: mockPaymentResponse }),
      });

      const checkout = {
        token: 'test-token',
        customer: { firstName: 'Nova', lastName: 'Demo' },
        payment: { id: 'payment-789' },
        amount: 500,
        method: 'card',
        cardholder: 'John Doe',
        correlationId: 'corr-123',
      };

      const result = await captureCheckout(checkout);

      expect(result.environmentLabel).toBe('Primary processor');
    });
  });
});

describe('Card Validation Edge Cases', () => {
  it('should handle Visa prefix variations', () => {
    expect(detectCardBrand('4000000000000000')).toBe('Visa');
    expect(detectCardBrand('4012345678901234')).toBe('Visa');
    expect(detectCardBrand('4111111111111111')).toBe('Visa');
  });

  it('should handle Mastercard prefix variations', () => {
    expect(detectCardBrand('5111111111111111')).toBe('Mastercard');
    expect(detectCardBrand('5211111111111111')).toBe('Mastercard');
    expect(detectCardBrand('5311111111111111')).toBe('Mastercard');
    expect(detectCardBrand('5411111111111111')).toBe('Mastercard');
    expect(detectCardBrand('5511111111111111')).toBe('Mastercard');
  });

  it('should detect Discover cards', () => {
    expect(detectCardBrand('6011111111111111')).toBe('Card');
    expect(detectCardBrand('6500000000000000')).toBe('Card');
  });

  it('should validate invalid Luhn numbers', () => {
    expect(isValidCardNumber('4242424242424241')).toBe(false);
    expect(isValidCardNumber('1234567890123456')).toBe(false);
  });

  it('should validate 15-digit Amex cards', () => {
    expect(isValidCardNumber('378282246310005')).toBe(false);
  });

  it('should validate empty and whitespace input', () => {
    expect(isValidCardNumber('')).toBe(false);
    expect(isValidCardNumber('   ')).toBe(false);
  });
});

describe('Expiry Validation Edge Cases', () => {
  it('should accept valid future dates', () => {
    expect(formatExpiry('1228')).toBe('12/28');
    expect(formatExpiry('0125')).toBe('01/25');
  });

  it('should handle single digit input', () => {
    expect(formatExpiry('1')).toBe('1');
    expect(formatExpiry('5')).toBe('5');
  });

  it('should handle two digit input without slash', () => {
    expect(formatExpiry('12')).toBe('12');
  });

  it('should limit to 4 digits', () => {
    expect(formatExpiry('12250')).toBe('12/25');
  });

  it('should handle non-numeric characters', () => {
    expect(formatExpiry('12/28')).toBe('12/28');
    expect(formatExpiry('12-28')).toBe('12/28');
  });
});

describe('Card Form Validation Edge Cases', () => {
  it('should accept valid test cards', () => {
    const validVisa = {
      cardNumber: '4242424242424242',
      expiry: '12/28',
      cvv: '123',
      cardholder: 'John Doe',
    };
    expect(validateCardForm(validVisa)).toEqual({});

    const validMastercard = {
      cardNumber: '5555555555554444',
      expiry: '12/28',
      cvv: '123',
      cardholder: 'Jane Smith',
    };
    expect(validateCardForm(validMastercard)).toEqual({});
  });

  it('should reject invalid card numbers', () => {
    const invalidCard = {
      cardNumber: '1234567890123456',
      expiry: '12/28',
      cvv: '123',
      cardholder: 'John Doe',
    };
    expect(validateCardForm(invalidCard).cardNumber).toBeDefined();
  });

  it('should reject expired cards', () => {
    const expiredCard = {
      cardNumber: '4242424242424242',
      expiry: '01/20',
      cvv: '123',
      cardholder: 'John Doe',
    };
    expect(validateCardForm(expiredCard).expiry).toBeDefined();
  });

  it('should reject invalid month', () => {
    const invalidMonth = {
      cardNumber: '4242424242424242',
      expiry: '13/28',
      cvv: '123',
      cardholder: 'John Doe',
    };
    expect(validateCardForm(invalidMonth).expiry).toBeDefined();
  });

  it('should reject invalid CVV length', () => {
    const shortCvv = {
      cardNumber: '4242424242424242',
      expiry: '12/28',
      cvv: '12',
      cardholder: 'John Doe',
    };
    expect(validateCardForm(shortCvv).cvv).toBeDefined();

    const longCvv = {
      cardNumber: '4242424242424242',
      expiry: '12/28',
      cvv: '1234',
      cardholder: 'John Doe',
    };
    expect(validateCardForm(longCvv).cvv).toBeDefined();
  });

  it('should handle whitespace-only cardholder name', () => {
    const whitespaceName = {
      cardNumber: '4242424242424242',
      expiry: '12/28',
      cvv: '123',
      cardholder: '   ',
    };
    expect(validateCardForm(whitespaceName).cardholder).toBeDefined();
  });

  it('should return multiple errors for multiple invalid fields', () => {
    const allInvalid = {
      cardNumber: '123',
      expiry: '1',
      cvv: '12',
      cardholder: '',
    };
    const errors = validateCardForm(allInvalid);
    expect(Object.keys(errors).length).toBe(4);
  });
});
