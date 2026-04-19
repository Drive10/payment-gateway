import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  token: string | null;
  refreshToken: string | null;
  user: User | null;
  expiresAt: number | null;
  setAuth: (auth: AuthData) => void;
  clearAuth: () => void;
  isAuthenticated: () => boolean;
}

interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
}

interface AuthData {
  token: string;
  refreshToken?: string;
  user: User;
  expiresAt: number;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      refreshToken: null,
      user: null,
      expiresAt: null,
      setAuth: (auth) => set({
        token: auth.token,
        refreshToken: auth.refreshToken ?? null,
        user: auth.user,
        expiresAt: auth.expiresAt,
      }),
      clearAuth: () => set({
        token: null,
        refreshToken: null,
        user: null,
        expiresAt: null,
      }),
      isAuthenticated: () => {
        const { token, expiresAt } = get();
        if (!token || !expiresAt) return false;
        return Date.now() < expiresAt;
      },
    }),
    { name: 'payflow-auth' }
  )
);

interface PaymentState {
  method: PaymentMethod;
  amount: number;
  orderId: string | null;
  paymentId: string | null;
  status: PaymentStatus;
  correlationId: string | null;
  setMethod: (method: PaymentMethod) => void;
  setAmount: (amount: number) => void;
  setOrderId: (orderId: string | null) => void;
  setPaymentId: (paymentId: string | null) => void;
  setStatus: (status: PaymentStatus) => void;
  setCorrelationId: (correlationId: string | null) => void;
  reset: () => void;
}

export type PaymentMethod = 'card' | 'upi' | 'netbanking' | 'wallet';
export type PaymentStatus = 'idle' | 'processing' | 'pending' | 'success' | 'failed';

export const usePaymentStore = create<PaymentState>()(
  persist(
    (set) => ({
      method: 'card',
      amount: 0,
      orderId: null,
      paymentId: null,
      status: 'idle',
      correlationId: null,
      setMethod: (method) => set({ method }),
      setAmount: (amount) => set({ amount }),
      setOrderId: (orderId) => set({ orderId }),
      setPaymentId: (paymentId) => set({ paymentId }),
      setStatus: (status) => set({ status }),
      setCorrelationId: (correlationId) => set({ correlationId }),
      reset: () => set({
        method: 'card',
        amount: 0,
        orderId: null,
        paymentId: null,
        status: 'idle',
        correlationId: null,
      }),
    }),
    { name: 'payflow-payment', partialize: (state) => ({ method: state.method, amount: state.amount }) }
  )
);

type CheckoutStatus = 'idle' | 'creating' | 'pending' | 'success' | 'failed';
type Transaction = {
  id: string;
  orderId: string;
  amount: number;
  method: PaymentMethod;
  status: PaymentStatus;
  correlationId: string;
  createdAt: string;
};

interface CheckoutState {
  status: CheckoutStatus;
  transaction: Transaction | null;
  error: string | null;
  setStatus: (status: CheckoutStatus) => void;
  setTransaction: (transaction: Transaction | null) => void;
  setError: (error: string | null) => void;
  reset: () => void;
}

export const useCheckoutStore = create<CheckoutState>()((set) => ({
  status: 'idle',
  transaction: null,
  error: null,
  setStatus: (status) => set({ status }),
  setTransaction: (transaction) => set({ transaction }),
  setError: (error) => set({ error }),
  reset: () => set({ status: 'idle', transaction: null, error: null }),
}));