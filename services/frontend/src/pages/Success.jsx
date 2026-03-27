import { motion } from "framer-motion";
import { Link, useLocation } from "react-router-dom";
import { getStoredTransaction } from "../lib/payment";

export default function Success() {
  const location = useLocation();
  const transaction = location.state?.transaction ?? getStoredTransaction();

  return (
    <main className="flex min-h-screen items-center justify-center px-4">
      <motion.section
        initial={{ opacity: 0, y: 18 }}
        animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-md rounded-[2rem] border border-emerald-100 bg-white/90 p-10 text-center shadow-[0_20px_80px_rgba(15,23,42,0.14)]"
      >
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-emerald-100 text-3xl text-emerald-600">
          ✓
        </div>
        <h1 className="mt-6 text-3xl font-semibold text-slate-950">
          Payment successful
        </h1>
        <p className="mt-3 text-sm leading-6 text-slate-500">
          Your transaction was completed successfully
          {transaction ? ` for ${transaction.amountLabel}. Order ${transaction.orderReference} is now paid.` : "."}
        </p>

        <Link
          to="/receipt"
          state={{ transaction }}
          className="mt-8 inline-flex rounded-2xl bg-slate-950 px-6 py-3 font-semibold text-white transition hover:bg-slate-800"
        >
          View receipt
        </Link>
      </motion.section>
    </main>
  );
}
