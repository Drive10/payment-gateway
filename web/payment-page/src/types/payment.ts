export interface CardDetails {
  cardholderName: string;
  cardNumber: string;
  expiry: string;
  cvv: string;
  email: string;
  phone?: string;
}

export interface UPIDetails {
  upiId: string;
  email: string;
}

export interface NetBankingDetails {
  bankCode: string;
  email: string;
}

export type PaymentMethod = 'card' | 'upi' | 'netbanking';

export type PaymentStatus = 
  | 'idle'                    // No payment initiated
  | 'initiated'              // Payment created, awaiting user action
  | 'pending'                // UPI: waiting for user to pay in UPI app
  | 'processing'             // Payment being processed
  | 'pending_otp'            // Awaiting OTP/3D secure verification
  | 'authorizing'            // Verifying with bank/payment provider
  | 'authorized'             // Payment authorized, awaiting capture
  | 'success'                // Payment completed successfully
  | 'failed'                 // Payment failed
  | 'expired'                // Payment expired
  | 'refunded'               // Payment was refunded
  | 'cancelled';             // Payment cancelled by user

export interface OrderSummary {
  productName: string;
  quantity: number;
  subtotal: number;
  tax: number;
  total: number;
  currency: string;
}

export interface InitiatePaymentResponse {
  transactionId: string;
  status: string;
  checkoutUrl?: string;      // For UPI: deep link to UPI app
  redirectUrl?: string;
  message?: string;
  errorCode?: string;
}

export interface VerifyOtpResponse {
  status: 'COMPLETED' | 'FAILED';
  message?: string;
}

export interface PaymentState {
  status: PaymentStatus;
  error: string | null;
  transactionId: string | null;
}

export interface CardBrand {
  name: 'visa' | 'mastercard' | 'amex' | 'rupay' | 'unknown';
  logo: string;
}

export const INDIAN_BANKS = [
  { code: 'SBIN', name: 'State Bank of India' },
  { code: 'HDFC', name: 'HDFC Bank' },
  { code: 'ICICI', name: 'ICICI Bank' },
  { code: 'AXIS', name: 'Axis Bank' },
  { code: 'KOTAK', name: 'Kotak Mahindra Bank' },
  { code: 'INDUSIND', name: 'IndusInd Bank' },
  { code: 'YESBANK', name: 'Yes Bank' },
  { code: 'IDFC', name: 'IDFC First Bank' },
  { code: 'BANDHAN', name: 'Bandhan Bank' },
  { code: 'RBL', name: 'RBL Bank' },
] as const;

export const UPI_BANKS = ['oksbi', 'okhdfcbank', 'okicici', 'okaxis', 'okkotak', 'okindusind', 'okyesbank', 'okidfc'] as const;
