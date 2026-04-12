import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { CardInputForm } from './CardInputForm';
import { CardPreview } from './CardPreview';
import { PayButton } from './PayButton';
import { TabSwitcher } from './TabSwitcher';
import { TrustBadges } from './TrustBadges';
import { UPIForm } from './UPIForm';
import { NetBankingForm } from './NetBankingForm';
import { OTPModal } from './OTPModal';
import { usePayment } from '../../hooks/usePayment';
import type { PaymentMethod, CardDetails, OrderSummary } from '../../types/payment';

interface PaymentPageProps {
  amount: number;
  currency?: string;
  merchantId?: string;
  orderSummary: OrderSummary;
}

export function PaymentPage({
  amount,
  currency = 'INR',
  merchantId = 'MERCHANT_001',
  orderSummary,
}: PaymentPageProps) {
  const [activeTab, setActiveTab] = useState<PaymentMethod>('card');
  const [cvvFocused, setCvvFocused] = useState(false);
  const [showOtpModal, setShowOtpModal] = useState(false);
  const [toast, setToast] = useState<{ message: string; type: 'error' | 'success' } | null>(null);

  const {
    status,
    error,
    transactionId,
    submitCardPayment,
    submitUPIPayment,
    submitNetBankingPayment,
    verifyOtp,
    pollStatus,
    reset,
  } = usePayment({ amount, currency, merchantId });

  useEffect(() => {
    if (status === 'pending_otp') {
      setShowOtpModal(true);
    }
  }, [status]);

  useEffect(() => {
    if (status === 'failed' && error) {
      setToast({ message: error, type: 'error' });
      setTimeout(() => setToast(null), 4000);
    }
  }, [status, error]);

  useEffect(() => {
    if (status === 'success') {
      setToast({ message: 'Payment successful!', type: 'success' });
      setTimeout(() => {
        setToast(null);
        reset();
        setShowOtpModal(false);
      }, 3000);
    }
  }, [status, reset]);

  const handleCardSubmit = async (data: CardDetails) => {
    try {
      await submitCardPayment(data);
    } catch {
      // Error handled by usePayment hook
    }
  };

  const handleUpiSubmit = async (data: { upiId: string; email: string }) => {
    try {
      await submitUPIPayment(data);
    } catch {
      // Error handled by usePayment hook
    }
  };

  const handleNetBankingSubmit = async (data: { bankCode: string; email: string }) => {
    try {
      await submitNetBankingPayment(data);
    } catch {
      // Error handled by usePayment hook
    }
  };

  const handleOtpVerify = async (otp: string) => {
    await verifyOtp(otp);
    await pollStatus();
  };

  const handleOtpResend = () => {
    console.log('OTP resent');
  };

  const handleCloseOtpModal = () => {
    setShowOtpModal(false);
    reset();
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: currency,
      minimumFractionDigits: 0,
    }).format(value);
  };

  return (
    <div className="min-h-screen bg-gray-50 py-8 px-4 sm:px-6 lg:px-8">
      {toast && (
        <motion.div
          initial={{ opacity: 0, y: -50 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -50 }}
          className={`fixed top-4 right-4 z-50 px-6 py-3 rounded-lg shadow-lg ${
            toast.type === 'error' ? 'bg-red-500' : 'bg-green-500'
          } text-white`}
          role="alert"
        >
          {toast.message}
        </motion.div>
      )}

      <div className="max-w-5xl mx-auto">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-gray-900">Complete Your Payment</h1>
          <p className="text-gray-500 mt-2">Secure checkout powered by PayFlow</p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            className="bg-white rounded-2xl shadow-lg p-6"
          >
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Order Summary</h2>
            
            <div className="space-y-4">
              <div className="flex justify-between items-center pb-4 border-b border-gray-100">
                <div>
                  <p className="font-medium text-gray-900">{orderSummary.productName}</p>
                  <p className="text-sm text-gray-500">Qty: {orderSummary.quantity}</p>
                </div>
                <p className="font-medium text-gray-900">{formatCurrency(orderSummary.subtotal)}</p>
              </div>

              <div className="flex justify-between items-center text-sm">
                <span className="text-gray-500">Subtotal</span>
                <span className="text-gray-900">{formatCurrency(orderSummary.subtotal)}</span>
              </div>

              <div className="flex justify-between items-center text-sm">
                <span className="text-gray-500">Tax</span>
                <span className="text-gray-900">{formatCurrency(orderSummary.tax)}</span>
              </div>

              <div className="flex justify-between items-center pt-4 border-t border-gray-100">
                <span className="text-lg font-semibold text-gray-900">Total</span>
                <span className="text-lg font-bold text-indigo-600">{formatCurrency(orderSummary.total)}</span>
              </div>
            </div>

            <TrustBadges />
          </motion.div>

          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            className="bg-white rounded-2xl shadow-lg p-6"
          >
            <TabSwitcher activeTab={activeTab} onTabChange={setActiveTab} />

            {activeTab === 'card' && (
              <>
                <CardPreview
                  cardholderName=""
                  cardNumber=""
                  expiry=""
                  cvv=""
                  isCvvFocused={cvvFocused}
                />
                <CardInputForm
                  onSubmit={handleCardSubmit}
                  onCvvFocus={setCvvFocused}
                  isLoading={status === 'processing'}
                 />
                 <div className="mt-6">
                   <PayButton
                     amount={amount}
                     currency={currency}
                     status={status === 'processing' ? 'processing' : status === 'success' ? 'success' : status === 'failed' ? 'failed' : 'idle'}
                     error={error}
                     onClick={handleCardSubmit}
                     disabled={status === 'processing'}
                   />
                 </div>
              </>
            )}

            {activeTab === 'upi' && (
              <>
                <div className="mb-6 text-center">
                  <div className="w-20 h-20 bg-indigo-100 rounded-full flex items-center justify-center mx-auto mb-4">
                    <svg className="w-10 h-10 text-indigo-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
                    </svg>
                  </div>
                  <p className="text-gray-600">Pay using your UPI app</p>
                </div>
                <UPIForm onSubmit={handleUpiSubmit} isLoading={status === 'processing'} />
                <div className="mt-6">
                  <PayButton
                    amount={amount}
                    currency={currency}
                    status={status === 'processing' ? 'processing' : status === 'success' ? 'success' : status === 'failed' ? 'failed' : 'idle'}
                    error={error}
                    onClick={() => {}}
                    disabled={status === 'processing'}
                  />
                </div>
              </>
            )}

            {activeTab === 'netbanking' && (
              <>
                <div className="mb-6 text-center">
                  <div className="w-20 h-20 bg-indigo-100 rounded-full flex items-center justify-center mx-auto mb-4">
                    <svg className="w-10 h-10 text-indigo-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h-2M7 19h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2 2v10a2 2 0 002 2zM9 9h6v6H9V9z" />
                    </svg>
                  </div>
                  <p className="text-gray-600">Pay directly from your bank account</p>
                </div>
                <NetBankingForm onSubmit={handleNetBankingSubmit} isLoading={status === 'processing'} />
                <div className="mt-6">
                  <PayButton
                    amount={amount}
                    currency={currency}
                    status={status === 'processing' ? 'processing' : status === 'success' ? 'success' : status === 'failed' ? 'failed' : 'idle'}
                    error={error}
                    onClick={() => {}}
                    disabled={status === 'processing'}
                  />
                </div>
              </>
            )}
          </motion.div>
        </div>
      </div>

      <OTPModal
        isOpen={showOtpModal}
        onClose={handleCloseOtpModal}
        onVerify={handleOtpVerify}
        onResendOtp={handleOtpResend}
        onCancel={handleCloseOtpModal}
      />
    </div>
  );
}
