/**
 * Payment State Machine Types
 * 
 * Defines the complete lifecycle of a payment transaction
 * following Stripe/Razorpay patterns
 */

export type PaymentState =
  | 'IDLE'              // Initial state - no payment started
  | 'CREATED'           // Order created, payment not initiated
  | 'PROCESSING'        // Payment being processed
  | 'AUTHORIZATION_PENDING'  // Awaiting OTP/biometric
  | 'CHALLENGE_REUIREDD'     // 3DS challenge
  | 'AUTHORIZED'       // Payment authorized (requires capture)
  | 'CAPTURED'         // Payment completed successfully
  | 'FAILED'           // Payment failed
  | 'EXPIRED';         // Payment session expired

export type PaymentMethodType =
  | 'card'
  | 'upi'
  | 'netbanking'
  | 'wallet';

export type TransactionMode =
  | 'TEST'
  | 'PRODUCTION';

/**
 * Core payment data structure
 * Single source of truth for payment state
 */
export interface PaymentTransaction {
  // Identifiers
  id: string;
  orderId: string;
  clientSecret?: string;
  
  // Financial
  amount: number;
  currency: string;
  
  // State
  state: PaymentState;
  method: PaymentMethodType;
  mode: TransactionMode;
  
  // Timestamps
  createdAt: number;
  expiresAt?: number;
  capturedAt?: number;
  
  // Provider data
  providerReference?: string;
  providerError?: string;
  
  // Metadata
  description?: string;
  customerEmail?: string;
  metadata?: Record<string, unknown>;
}

/**
 * API Request/Response types
 */
export interface CreateOrderRequest {
  orderId?: string;
  amount: number;
  currency: string;
  description?: string;
  customerEmail?: string;
  paymentMethod?: PaymentMethodType;
}

export interface CreateOrderResponse {
  orderId: string;
  paymentId: string;
  clientSecret: string;
  status: PaymentState;
  checkoutUrl?: string;
}

export interface CapturePaymentResponse {
  paymentId: string;
  status: PaymentState;
  providerReference?: string;
}

export interface VerifyOtpResponse {
  success: boolean;
  status: PaymentState;
  error?: string;
}

/**
 * Allowed state transitions
 * Defines valid moves from one state to another
 */
export const PAYMENT_TRANSITIONS: Record<PaymentState, PaymentState[]> = {
  IDLE: [],
  CREATED: ['PROCESSING', 'AUTHORIZATION_PENDING', 'FAILED', 'EXPIRED'],
  PROCESSING: ['AUTHORIZATION_PENDING', 'CHALLENGE_REUIREDD', 'AUTHORIZED', 'FAILED', 'EXPIRED'],
  AUTHORIZATION_PENDING: ['AUTHORIZED', 'CHALLENGE_REUIREDD', 'FAILED'],
  CHALLENGE_REUIREDD: ['AUTHORIZED', 'FAILED'],
  AUTHORIZED: ['CAPTURED', 'FAILED'],
  CAPTURED: [],
  FAILED: [],
  EXPIRED: [],
};

/**
 * Check if transition is valid
 */
export function canTransitionTo(from: PaymentState, to: PaymentState): boolean {
  return PAYMENT_TRANSITIONS[from]?.includes(to) ?? false;
}

/**
 * Check if state is terminal (no further action needed)
 */
export function isTerminalState(state: PaymentState): boolean {
  return state === 'CAPTURED' || state === 'FAILED' || state === 'EXPIRED';
}

/**
 * Check if state is error/failure
 */
export function isErrorState(state: PaymentState): boolean {
  return state === 'FAILED' || state === 'EXPIRED';
}