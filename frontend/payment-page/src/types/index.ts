export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
}

export interface AuthData {
  token: string;
  refreshToken?: string;
  user: User;
  expiresAt: number;
}

export interface PaymentLinkData {
  amount?: number;
  currency?: string;
  merchantName?: string;
  description?: string;
  externalReference?: string;
}

export interface CardFormValues {
  cardNumber: string;
  expiry: string;
  cvv: string;
  cardholder: string;
}

export interface FormErrors {
  cardNumber?: string;
  expiry?: string;
  cvv?: string;
  cardholder?: string;
}

export interface Order {
  id: string;
  externalReference: string;
  amount: number;
  currency: string;
  status: string;
  customerEmail?: string;
  customerName?: string;
  createdAt: string;
}

export interface Payment {
  id: string;
  orderId: string;
  amount: number;
  status: PaymentStatus;
  method: PaymentMethod;
  provider: string;
  transactionMode: string;
  providerOrderId?: string;
  providerPaymentId?: string;
  errorMessage?: string;
  errorCode?: string;
  createdAt: string;
}

export type PaymentMethod = 'card' | 'upi' | 'netbanking' | 'wallet';
export type PaymentStatus = 'idle' | 'processing' | 'pending' | 'success' | 'failed' | 'PROCESSING' | 'PENDING' | 'COMPLETED' | 'FAILED';

export interface Transaction {
  id: string;
  orderId: string;
  orderReference?: string;
  providerOrderId?: string;
  providerPaymentId?: string;
  amount: number;
  amountLabel: string;
  method: string;
  methodLabel: string;
  status: PaymentStatus;
  transactionMode: string;
  simulated?: boolean;
  customerLabel?: string;
  environmentLabel?: string;
  createdAt: string;
  dateLabel?: string;
  correlationId?: string;
  errorMessage?: string;
  errorCode?: string;
}