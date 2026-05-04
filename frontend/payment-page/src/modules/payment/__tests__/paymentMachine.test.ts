/**
 * Payment State Machine Tests
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { 
  canTransitionTo, 
  isTerminalState, 
  isErrorState,
  PAYMENT_TRANSITIONS 
} from '../modules/payment/types/payment';

describe('Payment State Machine', () => {
  describe('canTransitionTo', () => {
    it('allows CREATED to PROCESSING', () => {
      expect(canTransitionTo('CREATED', 'PROCESSING')).toBe(true);
    });
    
    it('allows CREATED to AUTHORIZATION_PENDING', () => {
      expect(canTransitionTo('CREATED', 'AUTHORIZATION_PENDING')).toBe(true);
    });
    
    it('allows CREATED to FAILED', () => {
      expect(canTransitionTo('CREATED', 'FAILED')).toBe(true);
    });
    
    it('allows AUTHORIZATION_PENDING to AUTHORIZED', () => {
      expect(canTransitionTo('AUTHORIZATION_PENDING', 'AUTHORIZED')).toBe(true);
    });
    
    it('allows AUTHORIZATION_PENDING to CHALLENGE_REUIREDD', () => {
      expect(canTransitionTo('AUTHORIZATION_PENDING', 'CHALLENGE_REUIREDD')).toBe(true);
    });
    
    it('allows AUTHORIZED to CAPTURED', () => {
      expect(canTransitionTo('AUTHORIZED', 'CAPTURED')).toBe(true);
    });
    
    it('blocks IDLE to any state', () => {
      expect(canTransitionTo('IDLE', 'CREATED')).toBe(false);
      expect(canTransitionTo('IDLE', 'PROCESSING')).toBe(false);
    });
    
    it('blocks backward transitions from CAPTURED', () => {
      expect(canTransitionTo('CAPTURED', 'PROCESSING')).toBe(false);
      expect(canTransitionTo('CAPTURED', 'CREATED')).toBe(false);
    });
    
    it('blocks FAILED to any state', () => {
      expect(canTransitionTo('FAILED', 'PROCESSING')).toBe(false);
      expect(canTransitionTo('FAILED', 'CAPTURED')).toBe(false);
    });
  });
  
  describe('isTerminalState', () => {
    it('returns true for CAPTURED', () => {
      expect(isTerminalState('CAPTURED')).toBe(true);
    });
    
    it('returns true for FAILED', () => {
      expect(isTerminalState('FAILED')).toBe(true);
    });
    
    it('returns true for EXPIRED', () => {
      expect(isTerminalState('EXPIRED')).toBe(true);
    });
    
    it('returns false for PROCESSING', () => {
      expect(isTerminalState('PROCESSING')).toBe(false);
    });
    
    it('returns false for AUTHORIZATION_PENDING', () => {
      expect(isTerminalState('AUTHORIZATION_PENDING')).toBe(false);
    });
  });
  
  describe('isErrorState', () => {
    it('returns true for FAILED', () => {
      expect(isErrorState('FAILED')).toBe(true);
    });
    
    it('returns true for EXPIRED', () => {
      expect(isErrorState('EXPIRED')).toBe(true);
    });
    
    it('returns false for PROCESSING', () => {
      expect(isErrorState('PROCESSING')).toBe(false);
    });
    
    it('returns false for CAPTURED', () => {
      expect(isErrorState('CAPTURED')).toBe(false);
    });
  });
});

describe('Payment Validation', () => {
  beforeEach(() => {
    vi.stubGlobal('import.meta', {
      env: { MODE: 'development' }
    });
  });
  
  it('validates correct card number (Luhn)', () => {
    const { isValidCardNumber } = require('../modules/payment/utils/validation');
    
    // Valid test cards
    expect(isValidCardNumber('4111111111111111')).toBe(true);
    expect(isValidCardNumber('5500000000000004')).toBe(true);
    expect(isValidCardNumber('340000000000009')).toBe(true);
  });
  
  it('rejects invalid card number', () => {
    const { isValidCardNumber } = require('../modules/payment/utils/validation');
    
    expect(isValidCardNumber('1234567890123456')).toBe(false);
    expect(isValidCardNumber('abcdefghijklmnop')).toBe(false);
    expect(isValidCardNumber('123')).toBe(false);
  });
  
  it('validates expiry format and value', () => {
    const { isValidExpiry } = require('../modules/payment/utils/validation');
    
    // Future expiry should pass
    const futureDate = new Date();
    futureDate.setFullYear(futureDate.getFullYear() + 1);
    const futureMonth = String(futureDate.getMonth() + 1).padStart(2, '0');
    const futureYear = String(futureDate.getFullYear()).slice(-2);
    
    expect(isValidExpiry(`${futureMonth}/${futureYear}`)).toBe(true);
    
    // Invalid format
    expect(isValidExpiry('13/25')).toBe(false);
    expect(isValidExpiry('00/25')).toBe(false);
  });
  
  it('detects card type', () => {
    const { getCardType } = require('../modules/payment/utils/validation');
    
    expect(getCardType('4111111111111111')).toBe('Visa');
    expect(getCardType('5500000000000004')).toBe('Mastercard');
    expect(getCardType('340000000000009')).toBe('Amex');
    expect(getCardType('6011000000000004')).toBe('Discover');
  });
});