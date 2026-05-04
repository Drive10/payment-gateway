import { motion } from "framer-motion";
import { useLocation } from "react-router-dom";
import { getStoredTransaction } from "../lib/payment.ts";

const DECLINE_MESSAGES = {
  generic_decline: "Your card was declined by the issuing bank. Please try a different card.",
  insufficient_funds: "Insufficient funds available. Please add funds or use a different card.",
  expired_card: "Your card has expired. Please use a valid card.",
  invalid_card: "The card number appears to be invalid. Please check and try again.",
  card_lost: "This card has been reported as lost or stolen. Please use a different card.",
  risk_rejected: "This transaction was flagged by our security system. Please try again later.",
  timeout: "The bank did not respond in time. Please try again.",
  network_error: "A network error occurred. Please check your connection and try again.",
  caller_error: "A technical error occurred. Please contact the merchant.",
  unknown: "Payment failed. Please try again."
};

export default function Failure() {
  const location = useLocation();
  const transaction = location.state?.transaction ?? getStoredTransaction();
  const errorMessage = location.state?.error || "Payment failed. Please try again.";
  const declineCode = location.state?.declineCode;
  const declineReason = location.state?.declineReason;
  
  const displayMessage = declineCode 
    ? DECLINE_MESSAGES[declineCode] || DECLINE_MESSAGES.unknown 
    : errorMessage;

  return (
    <main className="flex min-h-screen items-center justify-center px-4 py-10">
      <motion.section
        initial={{ opacity: 0, y: 18 }}
        animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-3xl overflow-hidden rounded-[2rem] border border-red-200/60 bg-white/95 shadow-[0_28px_90px_rgba(15,23,42,0.16)]"
      >
        <div className="grid gap-0 md:grid-cols-[0.9fr_1.1fr]">
          <div className="bg-[linear-gradient(180deg,#450a0a,#7f1d1d)] p-8 text-white">
            <div className="flex h-16 w-16 items-center justify-center rounded-full bg-red-200 text-3xl font-bold text-red-900">
              X
            </div>
            <h1 className="mt-6 text-3xl font-semibold tracking-tight">
              Payment Failed
            </h1>
            <p className="mt-3 text-sm leading-6 text-red-50/90">
              {displayMessage}
            </p>
            {declineCode && (
              <div className="mt-4 rounded-lg bg-white/10 px-3 py-2 text-xs font-mono text-red-200">
                Error: {declineCode}
              </div>
            )}
            {transaction?.environmentLabel ? (
              <div className="mt-6 inline-flex rounded-full border border-white/15 bg-white/10 px-4 py-2 text-sm font-semibold text-red-50">
                {transaction.environmentLabel}
              </div>
            ) : null}
          </div>

          <div className="p-8">
            <div className="space-y-4">
              {[
                ["Amount", transaction?.amountLabel ?? "N/A"],
                ["Order", transaction?.orderReference ?? "N/A"],
                ["Payment ID", transaction?.id ?? "N/A"],
                ["Customer", transaction?.customerLabel ?? "N/A"],
                ...(declineReason ? [["Reason", declineReason]] : []),
              ].map(([label, value]) => (
                <div
                  key={label}
                  className="flex items-center justify-between gap-4 rounded-[1.25rem] border border-slate-200 bg-slate-50 px-4 py-4"
                >
                  <span className="text-sm font-medium text-slate-500">
                    {label}
                  </span>
                  <span className="text-right text-sm font-semibold text-slate-950">
                    {value}
                  </span>
                </div>
              ))}
            </div>

            <div className="mt-8 space-y-3">
              <p className="text-center text-sm text-slate-500">
                Please contact support with payment ID: {transaction?.id}
              </p>
              {declineCode && (
                <div className="text-center">
                  <button 
                    onClick={() => window.history.back()}
                    className="text-sm text-blue-600 hover:underline"
                  >
                    Try another payment method
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      </motion.section>
    </main>
  );
}
