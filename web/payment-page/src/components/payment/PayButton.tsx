import { motion, AnimatePresence } from 'framer-motion';

interface PayButtonProps {
  amount: number;
  currency?: string;
  status: 'idle' | 'processing' | 'success' | 'failed';
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

  const isDisabled = disabled || status === 'processing';

  return (
    <div className="w-full">
      <motion.button
        onClick={onClick}
        disabled={isDisabled}
        className={`w-full py-4 px-6 rounded-xl font-semibold text-lg transition-all duration-200 
          ${status === 'success' 
            ? 'bg-green-500 text-white hover:bg-green-600' 
            : status === 'failed'
              ? 'bg-red-500 text-white hover:bg-red-600'
              : 'bg-indigo-600 text-white hover:bg-indigo-700'
          }
          ${isDisabled ? 'opacity-70 cursor-not-allowed' : 'shadow-lg hover:shadow-xl'}
        `}
        whileTap={{ scale: isDisabled ? 1 : 0.98 }}
      >
        <AnimatePresence mode="wait">
          {status === 'processing' && (
            <motion.div
              key="processing"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="flex items-center justify-center gap-2"
            >
              <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
              </svg>
              Processing...
            </motion.div>
          )}
          
          {status === 'success' && (
            <motion.div
              key="success"
              initial={{ opacity: 0, scale: 0.8 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0 }}
              className="flex items-center justify-center gap-2"
            >
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
            </motion.div>
          )}
          
          {status === 'failed' && (
            <motion.div
              key="failed"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="flex items-center justify-center gap-2"
            >
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
              Try again
            </motion.div>
          )}
          
          {status === 'idle' && (
            <motion.div
              key="idle"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
            >
              Pay {formatAmount(amount)}
            </motion.div>
          )}
        </AnimatePresence>
      </motion.button>
      
      <AnimatePresence>
        {error && status === 'failed' && (
          <motion.p
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            className="text-red-500 text-sm text-center mt-2"
            role="alert"
          >
            {error}
          </motion.p>
        )}
      </AnimatePresence>
    </div>
  );
}
