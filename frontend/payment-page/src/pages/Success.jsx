import { motion } from "framer-motion";
import { useLocation } from "react-router-dom";
import { getStoredTransaction } from "../lib/payment.ts";

export default function Success() {
  const location = useLocation();
  const transaction = location.state?.transaction ?? getStoredTransaction();

  return (
    <main className="flex min-h-screen items-center justify-center px-4 py-10">
      <motion.section
        initial={{ opacity: 0, y: 18 }}
        animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-3xl overflow-hidden rounded-[2rem] border border-emerald-200/60 bg-white/95 shadow-[0_28px_90px_rgba(15,23,42,0.16)]"
      >
        <div className="grid gap-0 md:grid-cols-[0.9fr_1.1fr]">
          <div className="bg-[linear-gradient(180deg,#022c22,#064e3b)] p-8 text-white">
            <div className="flex h-16 w-16 items-center justify-center rounded-full bg-emerald-200 text-3xl font-bold text-emerald-900">
              OK
            </div>
            <h1 className="mt-6 text-3xl font-semibold tracking-tight">
              Payment successful
            </h1>
            <p className="mt-3 text-sm leading-6 text-emerald-50/90">
              The payment was captured successfully and is ready for downstream
              settlement and reconciliation checks.
            </p>
            {transaction?.environmentLabel ? (
              <div className="mt-6 inline-flex rounded-full border border-white/15 bg-white/10 px-4 py-2 text-sm font-semibold text-emerald-50">
                {transaction.environmentLabel}
              </div>
            ) : null}
          </div>

          <div className="p-8">
            <div className="space-y-4">
              {[
                ["Amount", transaction?.amountLabel ?? "Pending"],
                ["Order", transaction?.orderReference ?? "Pending"],
                ["Payment ID", transaction?.id ?? "Pending"],
                ["Customer", transaction?.customerLabel ?? "Pending"],
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
                Please save your payment ID for reference: {transaction?.id}
              </p>
            </div>
          </div>
        </div>
      </motion.section>
    </main>
  );
}
