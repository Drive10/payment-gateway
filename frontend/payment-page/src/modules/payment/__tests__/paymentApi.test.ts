import { describe, it, expect, vi, beforeEach } from 'vitest';
import { createOrder, getPaymentStatus, capturePayment, verifyOtp, confirmPaymentMethod } from '../api/paymentApi';

const mockFetch = vi.fn();
global.fetch = mockFetch;

beforeEach(() => {
  mockFetch.mockReset();
});

function mockResponse(ok: boolean, data: unknown, status = 200) {
  return Promise.resolve({
    ok,
    status,
    json: () => Promise.resolve(data),
  });
}

describe('Payment API', () => {
  describe('createOrder', () => {
    it('creates order successfully', async () => {
      const responseData = {
        success: true,
        data: {
          orderId: 'ORD-123',
          paymentId: 'pay_abc',
          clientSecret: 'sec_123',
          status: 'CREATED' as const,
        },
      };

      mockFetch.mockResolvedValue(mockResponse(true, responseData));

      const result = await createOrder({ amount: 1000, currency: 'INR' });
      expect(result.orderId).toBe('ORD-123');
      expect(result.paymentId).toBe('pay_abc');
    });

    it('throws on API failure', async () => {
      mockFetch.mockResolvedValue(
        mockResponse(false, { message: 'Failed to create order' }, 500)
      );

      await expect(createOrder({ amount: 1000, currency: 'INR' })).rejects.toThrow();
    });
  });

  describe('getPaymentStatus', () => {
    it('returns payment status', async () => {
      mockFetch.mockResolvedValue(
        mockResponse(true, {
          success: true,
          data: { status: 'AUTHORIZED' },
        })
      );

      const result = await getPaymentStatus('pay_123');
      expect(result.status).toBe('AUTHORIZED');
    });
  });

  describe('capturePayment', () => {
    it('captures payment successfully', async () => {
      mockFetch.mockResolvedValue(
        mockResponse(true, {
          success: true,
          data: { paymentId: 'pay_123', status: 'CAPTURED' },
        })
      );

      const result = await capturePayment('pay_123');
      expect(result.status).toBe('CAPTURED');
    });
  });

  describe('verifyOtp', () => {
    it('verifies OTP successfully', async () => {
      mockFetch.mockResolvedValue(
        mockResponse(true, {
          data: { success: true, status: 'AUTHORIZED' },
        })
      );

      const result = await verifyOtp('pay_123', '123456');
      expect(result.status).toBe('AUTHORIZED');
    });
  });

  describe('confirmPaymentMethod', () => {
    it('confirms payment method', async () => {
      mockFetch.mockResolvedValue(
        mockResponse(true, {
          success: true,
          data: {
            orderId: 'ORD-1',
            paymentId: 'pay_123',
            clientSecret: 'sec_123',
            status: 'PROCESSING',
          },
        })
      );

      const result = await confirmPaymentMethod('pay_123', 'card', {
        cardNumber: '4111111111111111',
      });
      expect(result.status).toBe('PROCESSING');
    });
  });

  describe('network retry', () => {
    it('retries on network failure', async () => {
      const responseData = {
        success: true,
        data: { paymentId: 'pay_123', status: 'CAPTURED' },
      };

      mockFetch
        .mockRejectedValueOnce(new Error('Network error'))
        .mockResolvedValueOnce(mockResponse(true, responseData));

      const result = await capturePayment('pay_123');
      expect(result.status).toBe('CAPTURED');
      expect(mockFetch).toHaveBeenCalledTimes(2);
    });
  });
});
