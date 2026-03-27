import { motion } from "framer-motion";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import CardForm from "../components/CardForm";
import UPIQR from "../components/UpiQR";
import {
  PAYMENT_AMOUNT,
  PAYMENT_NOTE,
  startCheckout,
  TRANSACTION_MODES,
  validateCardForm,
} from "../lib/payment";

const initialForm = {
  cardNumber: "",
  expiry: "",
  cvv: "",
  cardholder: "",
};

export default function Checkout() {
  const [method, setMethod] = useState("card");
  const [values, setValues] = useState(initialForm);
  const [transactionMode, setTransactionMode] = useState(
    TRANSACTION_MODES.PRODUCTION,
  );
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState("");
  const navigate = useNavigate();

  const handleChange = (field, value) => {
    setValues((current) => ({ ...current, [field]: value }));
    setErrors((current) => ({ ...current, [field]: undefined }));
  };

  const pay = async () => {
    if (method === "card") {
      const nextErrors = validateCardForm(values);
      if (Object.keys(nextErrors).length > 0) {
        setErrors(nextErrors);
        return;
      }
    }

    try {
      setSubmitting(true);
      setSubmitError("");
      const checkout = await startCheckout({
        amount: PAYMENT_AMOUNT,
        method,
        cardholder: values.cardholder,
        transactionMode,
      });
      navigate("/processing", { state: { checkout } });
    } catch (error) {
      setSubmitError(error.message || "Unable to start payment.");
    } finally {
      setSubmitting(false);
    }
  };

  const disabled =
    submitting ||
    (method === "card" &&
    (!values.cardNumber ||
      !values.expiry ||
      !values.cvv ||
      !values.cardholder.trim()));

  return (
    <main className="min-h-screen px-4 py-10 sm:px-6">
      <div className="mx-auto grid w-full max-w-6xl gap-8 lg:grid-cols-[1.1fr_0.9fr]">
        <motion.section
          initial={{ opacity: 0, y: 28 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.45, ease: "easeOut" }}
          className="overflow-hidden rounded-[2rem] border border-white/60 bg-slate-950 px-6 py-8 text-white shadow-[0_24px_100px_rgba(15,23,42,0.28)] sm:px-8"
        >
          <p className="text-sm uppercase tracking-[0.3em] text-cyan-300">
            Nova Checkout
          </p>
          <h1 className="mt-4 max-w-lg text-4xl font-semibold tracking-tight sm:text-5xl">
            A payment UI that feels premium before you even hit pay.
          </h1>
          <p className="mt-4 max-w-xl text-sm leading-6 text-slate-300 sm:text-base">
            A polished React + Vite demo with card validation, UPI support,
            motion, and PDF receipt generation baked in.
          </p>

          <div className="mt-8 grid gap-4 sm:grid-cols-3">
            <div className="rounded-3xl border border-white/10 bg-white/5 p-4">
              <p className="text-xs uppercase tracking-[0.2em] text-slate-400">
                Amount
              </p>
              <p className="mt-2 text-2xl font-semibold">₹{PAYMENT_AMOUNT}</p>
            </div>
            <div className="rounded-3xl border border-white/10 bg-white/5 p-4">
              <p className="text-xs uppercase tracking-[0.2em] text-slate-400">
                Merchant
              </p>
              <p className="mt-2 text-lg font-semibold">Nova Commerce</p>
            </div>
            <div className="rounded-3xl border border-white/10 bg-white/5 p-4">
              <p className="text-xs uppercase tracking-[0.2em] text-slate-400">
                Includes
              </p>
              <p className="mt-2 text-lg font-semibold">Card + UPI</p>
            </div>
          </div>
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.45, delay: 0.08, ease: "easeOut" }}
          className="rounded-[2rem] border border-slate-200 bg-white/90 p-6 shadow-[0_20px_80px_rgba(15,23,42,0.12)] backdrop-blur sm:p-8"
        >
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="text-sm font-medium uppercase tracking-[0.2em] text-cyan-700">
                Secure Checkout
              </p>
              <h2 className="mt-2 text-3xl font-semibold tracking-tight text-slate-950">
                Complete your payment
              </h2>
            </div>
            <div className="rounded-2xl bg-slate-100 px-4 py-3 text-right">
              <p className="text-xs uppercase tracking-[0.2em] text-slate-500">
                Payable
              </p>
              <p className="mt-1 text-2xl font-semibold text-slate-950">
                ₹{PAYMENT_AMOUNT}
              </p>
            </div>
          </div>

          <div className="mt-6 grid grid-cols-2 gap-3 rounded-3xl bg-slate-100 p-2">
            {["card", "upi"].map((option) => {
              const active = method === option;
              return (
                <button
                  key={option}
                  type="button"
                  onClick={() => setMethod(option)}
                  className={`rounded-[1.2rem] px-4 py-3 text-sm font-semibold capitalize transition ${
                    active
                      ? "bg-white text-slate-950 shadow-sm"
                      : "text-slate-500 hover:text-slate-900"
                  }`}
                >
                  {option === "card" ? "Card payment" : "UPI payment"}
                </button>
              );
            })}
          </div>

          <div className="mt-6">
            <p className="text-sm font-medium uppercase tracking-[0.2em] text-cyan-700">
              Transaction Mode
            </p>
            <div className="mt-3 grid grid-cols-2 gap-3 rounded-3xl bg-slate-100 p-2">
              {[TRANSACTION_MODES.PRODUCTION, TRANSACTION_MODES.TEST].map(
                (option) => {
                  const active = transactionMode === option;
                  return (
                    <button
                      key={option}
                      type="button"
                      onClick={() => setTransactionMode(option)}
                      className={`rounded-[1.2rem] px-4 py-3 text-sm font-semibold transition ${
                        active
                          ? "bg-white text-slate-950 shadow-sm"
                          : "text-slate-500 hover:text-slate-900"
                      }`}
                    >
                      {option === TRANSACTION_MODES.TEST
                        ? "Test / Fake"
                        : "Production-like"}
                    </button>
                  );
                },
              )}
            </div>
          </div>

          <div className="mt-6">
            {method === "card" ? (
              <CardForm
                values={values}
                errors={errors}
                onChange={handleChange}
              />
            ) : (
              <UPIQR />
            )}
          </div>

          <div className="mt-6 rounded-3xl border border-amber-100 bg-amber-50 px-4 py-3 text-sm text-amber-900">
            Demo note: {PAYMENT_NOTE} Current mode:{" "}
            {transactionMode === TRANSACTION_MODES.TEST
              ? "Test simulator"
              : "Production-like transaction"}.
          </div>

          {submitError ? (
            <div className="mt-4 rounded-3xl border border-rose-100 bg-rose-50 px-4 py-3 text-sm text-rose-700">
              {submitError}
            </div>
          ) : null}

          <button
            type="button"
            onClick={pay}
            disabled={disabled}
            className="mt-6 w-full rounded-2xl bg-slate-950 px-5 py-4 text-base font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-300"
          >
            {submitting ? "Connecting to backend..." : `Pay ₹${PAYMENT_AMOUNT}`}
          </button>
        </motion.section>
      </div>
    </main>
  );
}
