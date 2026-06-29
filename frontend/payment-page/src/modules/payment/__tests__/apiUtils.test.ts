import { describe, it, expect } from 'vitest';
import { createApiError, isApiError, getErrorMessage } from '../utils/api';

describe('API Error Utilities', () => {
  describe('createApiError', () => {
    it('creates error with status and code', () => {
      const error = createApiError('Not found', 404, 'NOT_FOUND');
      expect(error.message).toBe('Not found');
      expect(error.status).toBe(404);
      expect(error.code).toBe('NOT_FOUND');
      expect(error).toBeInstanceOf(Error);
    });
  });

  describe('isApiError', () => {
    it('returns true for API error objects', () => {
      const error = createApiError('Server error', 500, 'SERVER_ERROR');
      expect(isApiError(error)).toBe(true);
    });

    it('returns false for plain Error', () => {
      expect(isApiError(new Error('plain'))).toBe(false);
    });

    it('returns false for null', () => {
      expect(isApiError(null)).toBe(false);
    });

    it('returns false for strings', () => {
      expect(isApiError('error')).toBe(false);
    });
  });

  describe('getErrorMessage', () => {
    it('returns message for API errors', () => {
      const error = createApiError('Payment failed', 402, 'PAYMENT_FAILED');
      expect(getErrorMessage(error)).toBe('Payment failed');
    });

    it('returns message for regular errors', () => {
      expect(getErrorMessage(new Error('Something went wrong'))).toBe('Something went wrong');
    });

    it('returns fallback for unknown errors', () => {
      expect(getErrorMessage('just a string')).toBe('An unexpected error occurred');
      expect(getErrorMessage(42)).toBe('An unexpected error occurred');
      expect(getErrorMessage(undefined)).toBe('An unexpected error occurred');
    });
  });
});
