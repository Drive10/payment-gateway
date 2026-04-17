import { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';

interface OTPModalProps {
  isOpen: boolean;
  onClose: () => void;
  onVerify: (otp: string) => Promise<void>;
  onResendOtp: () => void;
  onCancel: () => void;
}

export function OTPModal({ isOpen, onClose, onVerify, onResendOtp, onCancel }: OTPModalProps) {
  const [otp, setOtp] = useState<string[]>(['', '', '', '', '', '']);
  const [isVerifying, setIsVerifying] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [countdown, setCountdown] = useState(300);
  const [canResend, setCanResend] = useState(false);
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

  useEffect(() => {
    if (isOpen) {
      setOtp(['', '', '', '', '', '']);
      setError(null);
      setCountdown(300);
      setCanResend(false);
      inputRefs.current[0]?.focus();
    }
  }, [isOpen]);

  useEffect(() => {
    if (countdown > 0) {
      const timer = setInterval(() => setCountdown(c => c - 1), 1000);
      return () => clearInterval(timer);
    } else {
      setCanResend(true);
    }
  }, [countdown, isOpen]);

  useEffect(() => {
    const filled = otp.every(d => d !== '');
    if (filled) {
      handleVerify();
    }
  }, [otp]);

  const handleChange = (index: number, value: string) => {
    if (!/^\d*$/.test(value)) return;
    
    const newOtp = [...otp];
    newOtp[index] = value.slice(-1);
    setOtp(newOtp);
    setError(null);

    if (value && index < 5) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (index: number, e: React.KeyboardEvent) => {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  const handleVerify = async () => {
    const otpString = otp.join('');
    if (otpString.length !== 6) return;

    setIsVerifying(true);
    setError(null);

    try {
      await onVerify(otpString);
    } catch (err: any) {
      setError(err.message || 'Invalid OTP');
      setOtp(['', '', '', '', '', '']);
      inputRefs.current[0]?.focus();
    } finally {
      setIsVerifying(false);
    }
  };

  const handleResend = () => {
    if (canResend) {
      onResendOtp();
      setCountdown(300);
      setCanResend(false);
      setOtp(['', '', '', '', '', '']);
      inputRefs.current[0]?.focus();
    }
  };

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
          onClick={onCancel}
        >
          <motion.div
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0.9, opacity: 0 }}
            className="bg-white rounded-2xl shadow-2xl max-w-md w-full p-6"
            onClick={e => e.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-labelledby="otp-title"
          >
            <div className="text-center mb-6">
              <div className="w-16 h-16 bg-indigo-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <svg className="w-8 h-8 text-indigo-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                </svg>
              </div>
              <h2 id="otp-title" className="text-xl font-semibold text-gray-900">
                Enter OTP
              </h2>
              <p className="text-gray-500 text-sm mt-1">
                A 6-digit OTP has been sent to your registered mobile number
              </p>
            </div>

            <div className="flex justify-center gap-2 mb-6">
              {otp.map((digit, index) => (
                <input
                  key={index}
                  ref={el => { inputRefs.current[index] = el; }}
                  type="text"
                  inputMode="numeric"
                  maxLength={1}
                  value={digit}
                  onChange={e => handleChange(index, e.target.value)}
                  onKeyDown={e => handleKeyDown(index, e)}
                  className={`w-12 h-12 text-center text-xl font-mono border-2 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all
                    ${error ? 'border-red-500 bg-red-50' : 'border-gray-300'}
                  `}
                  aria-label={`Digit ${index + 1}`}
                />
              ))}
            </div>

            {error && (
              <p className="text-red-500 text-sm text-center mb-4" role="alert">
                {error}
              </p>
            )}

            <div className="flex justify-between items-center text-sm text-gray-500 mb-6">
              <span>Time remaining: {formatTime(countdown)}</span>
              <button
                type="button"
                onClick={handleResend}
                disabled={!canResend}
                className={`${canResend ? 'text-indigo-600 hover:text-indigo-700' : 'text-gray-400 cursor-not-allowed'}`}
              >
                {canResend ? 'Resend OTP' : 'Resend in ' + formatTime(countdown)}
              </button>
            </div>

            <div className="flex gap-3">
              <button
                type="button"
                onClick={onCancel}
                className="flex-1 py-3 px-4 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleVerify}
                disabled={isVerifying || otp.join('').length !== 6}
                className="flex-1 py-3 px-4 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isVerifying ? 'Verifying...' : 'Verify'}
              </button>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
