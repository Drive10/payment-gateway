import { motion, AnimatePresence } from 'framer-motion';

interface PayButtonProps {
  amount: number;
  currency?: string;
  status: 'idle' | 'initiated' | 'pending' | 'processing' | 'pending_otp' | 'authorizing' | 'success' | 'failed' | 'expired';
  error?: string | null;
  onClick: () => void;
  disabled?: boolean;
}

export function PayButton({ 
  amount, 
  currency = 'INR',
  status,
  error,
  onClick,
  disabled 
}: PayButtonProps) {
  const formatAmount = (value: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: currency,
      minimumFractionDigits: 0,
    }).format(value);
  };

  const isDisabled = disabled || status === 'processing' || status === 'authorizing';

  const getButtonContent = () => {
    switch (status) {
      case 'processing':
        return (
          <div className="flex items-center justify-center gap-2">
            <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
            Processing...
          </div>
        );
      case 'initiated':
        return 'Payment initiated';
      case 'pending':
        return 'Waiting for payment...';
      case 'pending_otp':
        return 'Verify OTP';
      case 'authorizing':
        return (
          <div className="flex items-center justify-center gap-2">
            <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
            Verifying...
          </div>
        );
      case 'success':
        return (
          <div className="flex items-center justify-center gap-2">
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <motion.path
                initial={{ pathLength: 0 }}
                animate={{ pathLength: 1 }}
                transition={{ duration: 0.3 }}
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M5 13l4 4L19 7"
              />
            </svg>
            Payment successful
          </div>
        );
      case 'failed':
        return (
          <div className="flex items-center justify-center gap-2">
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
            Try again
          </div>
        );
      case 'expired':
        return 'Payment expired';
      default:
        return `Pay ${formatAmount(amount)}`;
    }
  };
