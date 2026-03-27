import { motion } from "framer-motion";
import { useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { captureCheckout, getStoredTransaction } from "../lib/payment";

export default function Processing() {
  const location = useLocation();
  const navigate = useNavigate();
  const checkout = location.state?.checkout ?? getStoredTransaction();

  useEffect(() => {
    if (!checkout?.payment?.id) {
      navigate("/", { replace: true });
      return;
    }

    const timer = window.setTimeout(async () => {
      try {
        const transaction = await captureCheckout(checkout);
        navigate("/success", { replace: true, state: { transaction } });
      } catch {
        navigate("/", { replace: true });
      }
    }, 1800);

    return () => window.clearTimeout(timer);
  }, [checkout, navigate]);

  return (
    <main className="flex min-h-screen items-center justify-center px-4">
      <motion.section
        initial={{ opacity: 0, scale: 0.96 }}
        animate={{ opacity: 1, scale: 1 }}
        className="w-full max-w-md rounded-[2rem] border border-white/50 bg-white/85 p-10 text-center shadow-[0_20px_80px_rgba(15,23,42,0.14)] backdrop-blur"
      >
        <div className="mx-auto h-16 w-16 animate-spin rounded-full border-4 border-cyan-500 border-t-transparent" />
        <h1 className="mt-6 text-2xl font-semibold text-slate-950">
          Processing payment
        </h1>
        <p className="mt-3 text-sm leading-6 text-slate-500">
          Finalizing your secure transaction and preparing your receipt.
        </p>
      </motion.section>
    </main>
  );
}
