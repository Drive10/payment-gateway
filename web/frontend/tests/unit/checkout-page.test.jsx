import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import Checkout from '../../src/pages/Checkout';

describe('Checkout Page', () => {
  const defaultAmount = '100';
  
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // Mock useNavigate and useSearchParams from react-router-dom
  vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return {
      ...actual,
      useNavigate: vi.fn(() => vi.fn()),
      useSearchParams: vi.fn(() => [new URLSearchParams()]),
    };
  });

  // Mock import.meta.env for Vite environment variables
  vi.stubGlobal('import.meta.env', {
    VITE_API_URL: '/api',
    VITE_MERCHANT_ID: null,
  });

  it('should render with default values', () => {
    render(<Checkout />);

    // Check that amount input is present
    expect(screen.getByPlaceholderText('Enter amount')).toBeInTheDocument();
    
    // Check that card payment method is selected by default
    expect(screen.getByLabelText('Credit/Debit Card')).toHaveAttribute('aria-checked', 'true');
    
    // Check that test mode is selected by default
    expect(screen.getByLabelText('Test/Sandbox')).toHaveAttribute('aria-checked', 'true');
  });

  it('should validate amount input', async () => {
    render(<Checkout />);

    // Try to submit without entering amount
    const payButton = screen.getByRole('button', { name: /pay \$0/i });
    await fireEvent.click(payButton);
    
    // Should show error message
    expect(await screen.findByText(/Please enter a valid payment amount/)).toBeInTheDocument();
  });

  it('should accept valid amount and enable payment button', async () => {
    render(<Checkout />);

    // Enter valid amount
    const amountInput = screen.getByPlaceholderText('Enter amount');
    await fireEvent.change(amountInput, { target: { value: defaultAmount } });
    
    // Payment button should be enabled
    const payButton = screen.getByRole('button', { name: /pay \$100/i });
    expect(payButton).toBeEnabled();
  });

  it('should process card payment with valid data', async () => {
    // Mock successful payment response
    const mockCheckout = {
      token: 'mock-token',
      customer: { email: 'test@example.com', firstName: 'Test', lastName: 'User' },
      order: { id: 'order-123', merchantId: 'merchant-123' },
      payment: { id: 'payment-123', status: 'COMPLETED' },
      amount: 100,
      method: 'card',
      cardholder: 'Test User',
      correlationId: 'corr-123',
    };
    
    // Mock the startCheckout function to return our mock data
    const mockStartCheckout = vi.fn().mockResolvedValue(mockCheckout);
    
    // Mock the payment library
    vi.mock('../../src/lib/payment', () => ({
      ...vi.importActual('../../src/lib/payment'),
      startCheckout: mockStartCheckout,
      formatCurrency: (amount) => `$${amount}`,
      TRANSACTION_MODES: { TEST: 'TEST', PRODUCTION: 'PRODUCTION' },
      DEFAULT_PAYMENT_NOTE: 'Payment for order',
      UPI_ID: 'payflow@upi',
      STORAGE_KEY: 'payflow-checkout-transaction',
      STORAGE_KEY_AUTH: 'payflow-auth',
      persistCheckoutState: vi.fn(),
      persistAuth: vi.fn(),
      getStoredAuth: vi.fn(() => null),
      clearAuth: vi.fn(),
      getStoredTransaction: vi.fn(() => null),
      clearTransaction: vi.fn(),
      validateCardForm: vi.fn(() => ({})),
      formatCardNumber: vi.fn((value) => value),
      formatExpiry: vi.fn((value) => value),
      detectCardBrand: vi.fn(() => 'Visa'),
      isValidCardNumber: vi.fn(() => true),
      luhnCheck: vi.fn(() => true),
      apiRequest: vi.fn(),
      login: vi.fn(),
      register: vi.fn(),
      ensureAccessToken: vi.fn(),
      logout: vi.fn(),
      captureCheckout: vi.fn(),
      getPaymentStatus: vi.fn(),
      getOrderHistory: vi.fn(),
      getPaymentHistory: vi.fn(),
    }));
    
    render(<Checkout />);

    // Fill in the form
    const amountInput = screen.getByPlaceholderText('Enter amount');
    await fireEvent.change(amountInput, { target: { value: defaultAmount } });
    
    // Fill card details
    const cardNumberInput = screen.getByPlaceholderText('4242 4242 4242 4242');
    await fireEvent.change(cardNumberInput, { target: { value: '4111111111111111' } });
    
    const expiryInput = screen.getByPlaceholderText('MM/YY');
    await fireEvent.change(expiryInput, { target: { value: '12/28' } });
    
    const cvvInput = screen.getByPlaceholderText('•••');
    await fireEvent.change(cvvInput, { target: { value: '123' } });
    
    const cardholderInput = screen.getByPlaceholderText('Jordan Lee');
    await fireEvent.change(cardholderInput, { target: { value: 'Test User' } });
    
    // Submit payment
    const payButton = screen.getByRole('button', { name: /pay \$100/i });
    await fireEvent.click(payButton);
    
    // Verify startCheckout was called with correct parameters
    expect(mockStartCheckout).toHaveBeenCalledWith(
      expect.objectContaining({
        amount: 100,
        method: 'card',
        cardholder: 'Test User',
        transactionMode: 'TEST',
        description: 'Payment for order',
      })
    );
  });

  it('should handle UPI payment selection', async () => {
    render(<Checkout />);

    // Select UPI payment method
    const upiButton = screen.getByLabelText('UPI');
    await fireEvent.click(upiButton);
    
    // Verify UPI button is selected
    expect(upiButton).toHaveAttribute('aria-checked', 'true');
    
    // Verify card form is not visible
    expect(screen.queryByLabelText('Card Number')).not.toBeInTheDocument();
    
    // Verify UPI QR component is visible
    expect(screen.getByText(/Pay with UPI/i)).toBeInTheDocument();
  });

  it('should toggle between test and production modes', async () => {
    render(<Checkout />);

    // Initially test mode should be selected
    expect(screen.getByLabelText('Test/Sandbox')).toHaveAttribute('aria-checked', 'true');
    expect(screen.getByLabelText('Production')).not.toHaveAttribute('aria-checked', 'true');
    
    // Select production mode
    const productionButton = screen.getByLabelText('Production');
    await fireEvent.click(productionButton);
    
    // Verify production mode is selected
    expect(productionButton).toHaveAttribute('aria-checked', 'true');
    expect(screen.getByLabelText('Test/Sandbox')).not.toHaveAttribute('aria-checked', 'true');
  });

  it('should handle custom amount input', async () => {
    render(<Checkout />);

    // Click to show custom amount input
    const toggleButton = screen.getByRole('button', { name: /Enter custom amount/i });
    await fireEvent.click(toggleButton);
    
    // Custom amount input should appear
    const customAmountInput = screen.getByPlaceholderText('Enter amount');
    expect(customAmountInput).toBeInTheDocument();
    
    // Enter custom amount
    await fireEvent.change(customAmountInput, { target: { value: '250' } });
    
    // Payment button should reflect custom amount
    const payButton = screen.getByRole('button', { name: /pay \$250/i });
    expect(payButton).toBeEnabled();
  });
});