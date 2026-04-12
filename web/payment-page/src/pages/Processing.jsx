import { motion } from "framer-motion";
import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import {
  captureCheckout,
  formatCurrency,
  getStoredTransaction,
} from "../lib/payment";

export default function Processing() {
  const location = useLocation();
  const navigate = useNavigate();
  const checkout = location.state?.checkout ?? getStoredTransaction();
  const [error, setError] = useState(null);
  const [status, setStatus] = useState("processing");
  const [pollAttempt, setPollAttempt] = useState(0);

  useEffect(() => {
    if (!checkout?.payment?.id) {
      navigate("/", { replace: true });
      return;
    }

    const processPayment = async () => {
      try {
        setStatus("processing");
        const transaction = await captureCheckout(checkout, (currentStatus, attempt) => {
          setPollAttempt(attempt);
        });
        
        if (transaction.status === "FAILED") {
          setStatus("failed");
          setError(transaction.errorMessage || "Payment failed");
          setTimeout(() => {
            navigate("/failure", { replace: true, state: { transaction, error: transaction.errorMessage } });
          }, 1500);
        } else {
          navigate("/success", { replace: true, state: { transaction } });
        }
      } catch (err) {
        setStatus("failed");
        setError(err.message || "Payment failed. Please try again.");
        setTimeout(() => {
          navigate("/failure", { replace: true, state: { transaction: checkout, error: err.message } });
        }, 1500);
      }
    };

    processPayment();
  }, [checkout, navigate]);

  return (
    <main className="flex min-h-screen items-center justify-center px-4 py-10">
      <motion.section
        initial={{ opacity: 0, scale: 0.97, y: 12 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        className="w-full max-w-2xl overflow-hidden rounded-[2rem] border border-white/10 bg-slate-950 text-white shadow-[0_30px_120px_rgba(2,6,23,0.48)]"
      >
        <div className="grid gap-8 p-8 md:grid-cols-[0.9fr_1.1fr] md:p-10">
          <div className="rounded-[1.75rem] border border-cyan-400/15 bg-[linear-gradient(180deg,rgba(14,116,144,0.3),rgba(15,23,42,0.92))] p-6">
            {status === "failed" ? (
              <>
                <div className="mx-auto h-16 w-16 rounded-full border-4 border-red-400" />
                <h1 className="mt-6 text-2xl font-semibold tracking-tight text-red-400">
                  Payment Failed
                </h1>
                <p className="mt-3 text-sm leading-6 text-slate-300">
                  {error || "Redirecting..."}
                </p>
              </>
            ) : (
              <>
                <div className="mx-auto h-16 w-16 animate-spin rounded-full border-4 border-cyan-300 border-t-transparent" />
                <h1 className="mt-6 text-2xl font-semibold tracking-tight">
                  Processing payment
                </h1>
                <p className="mt-3 text-sm leading-6 text-slate-300">
                  {pollAttempt > 0 
                    ? `Verifying payment (attempt ${pollAttempt}/10)`
                    : "Confirming provider authorization, publishing the payment event, and waiting for ledger-safe completion."}
                </p>
              </>
            )}
          </div>

          <div className="space-y-4">
            <div className="rounded-[1.5rem] border border-white/10 bg-white/5 p-5">
              <p className="text-xs uppercase tracking-[0.24em] text-slate-400">
                Order reference
              </p>
              <p className="mt-2 text-lg font-semibold text-white">
                {checkout?.order?.externalReference ??
                  checkout?.order?.id ??
                  "Pending"}
              </p>
            </div>
            <div className="rounded-[1.5rem] border border-white/10 bg-white/5 p-5">
              <p className="text-xs uppercase tracking-[0.24em] text-slate-400">
                Amount
              </p>
              <p className="mt-2 text-lg font-semibold text-white">
                {formatCurrency(checkout?.amount ?? 0)}
              </p>
            </div>
            <div className="rounded-[1.5rem] border border-white/10 bg-white/5 p-5">
              <p className="text-xs uppercase tracking-[0.24em] text-slate-400">
                Correlation ID
              </p>
              <p className="mt-2 break-all text-sm font-medium text-cyan-200">
                {checkout?.correlationId ?? "Assigned by backend"}
              </p>
            </div>
          </div>
        </div>
      </motion.section>
    </main>
  );
}
