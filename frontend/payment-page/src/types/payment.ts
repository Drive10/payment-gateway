export type Currency = 'INR' | 'USD' | 'EUR';

export type PaymentMethod = 'CARD' | 'UPI';

export interface OrderSummary {
  productName: string;
  amount: number;
  currency: Currency;
}

export interface CustomerDetails {
  name: string;
  email: string;
}

export interface CreateOrderRequest {
  amount: number;
  currency: Currency;
  customerEmail: string;
}

export interface CreateOrderResponse {
  orderId: string;
  paymentSessionId: string;
}

export type PaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILURE' | 'EXPIRED';

export interface PaymentStatusResponse {
  orderId: string;
  status: PaymentStatus;
  transactionId?: string;
  errorMessage?: string;
}

export interface CheckoutFormValues {
  customerName: string;
  customerEmail: string;
  paymentMethod: PaymentMethod;
  // Card details are not stored in state for PCI compliance, 
  // but we might need some for validation or if using a tokenization lib
}
