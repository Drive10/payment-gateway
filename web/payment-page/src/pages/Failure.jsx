import { motion } from "framer-motion";
import { useLocation } from "react-router-dom";
import { getStoredTransaction } from "../lib/payment";

export default function Failure() {
  const location = useLocation();
  const transaction = location.state?.transaction ?? getStoredTransaction();
  const errorMessage = location.state?.error || "Payment failed. Please try again.";

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
              {errorMessage}
            </p>
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

            <div className="mt-8">
              <p className="text-center text-sm text-slate-500">
                Please contact support with payment ID: {transaction?.id}
              </p>
            </div>
          </div>
        </div>
      </motion.section>
    </main>
  );
}
