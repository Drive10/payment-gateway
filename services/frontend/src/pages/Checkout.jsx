import { motion } from "framer-motion";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import CardForm from "../components/CardForm";
import UpiQR from "../components/UpiQR";
import {
  PAYMENT_AMOUNT,
  PAYMENT_NOTE,
  TRANSACTION_MODES,
  formatCurrency,
  startCheckout,
  validateCardForm,
} from "../lib/payment";

const initialForm = {
  cardNumber: "",
  expiry: "",
  cvv: "",
  cardholder: "",
};

const trustSignals = [
  "Idempotent payment creation",
  "Ledger-backed settlement trail",
  "Request tracing across gateway and Kafka",
];

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
    <main className="relative min-h-screen overflow-hidden px-4 py-10 sm:px-6 lg:px-10">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(34,211,238,0.16),transparent_28%),radial-gradient(circle_at_top_right,rgba(16,185,129,0.12),transparent_20%),radial-gradient(circle_at_bottom,rgba(249,115,22,0.1),transparent_30%)]" />

      <div className="relative mx-auto grid w-full max-w-7xl gap-8 xl:grid-cols-[1.1fr_0.9fr]">
        <motion.section
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.45, ease: "easeOut" }}
          className="overflow-hidden rounded-[2rem] border border-white/10 bg-slate-950 text-white shadow-[0_28px_120px_rgba(2,6,23,0.5)]"
        >
          <div className="border-b border-white/10 px-6 py-5 sm:px-8">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div>
                <p className="text-xs uppercase tracking-[0.34em] text-cyan-300">
                  Nova Commerce
                </p>
                <h1 className="mt-3 max-w-2xl text-4xl font-semibold tracking-tight text-white sm:text-5xl">
                  Checkout designed like a real payment rail, not a form demo.
                </h1>
              </div>
              <div className="rounded-[1.5rem] border border-white/10 bg-white/5 px-5 py-4 text-right">
                <p className="text-[11px] uppercase tracking-[0.24em] text-slate-400">
                  Total due
                </p>
                <p className="mt-2 text-3xl font-semibold text-white">
                  {formatCurrency(PAYMENT_AMOUNT)}
                </p>
              </div>
            </div>
          </div>

          <div className="grid gap-8 px-6 py-8 sm:px-8 lg:grid-cols-[1.15fr_0.85fr]">
            <div>
              <p className="max-w-xl text-base leading-7 text-slate-300">
                A production-style checkout connected to your Spring Boot payment
                platform with gateway auth, actor-scoped idempotency,
                outbox-backed event publishing, and ledger verification.
              </p>

              <div className="mt-8 grid gap-4 sm:grid-cols-3">
                <div className="rounded-[1.5rem] border border-white/10 bg-white/5 p-4">
                  <p className="text-xs uppercase tracking-[0.2em] text-slate-400">
                    Merchant
                  </p>
                  <p className="mt-2 text-lg font-semibold text-white">
                    Nova Commerce
                  </p>
                </div>
                <div className="rounded-[1.5rem] border border-white/10 bg-white/5 p-4">
                  <p className="text-xs uppercase tracking-[0.2em] text-slate-400">
                    Use case
                  </p>
                  <p className="mt-2 text-lg font-semibold text-white">
                    Checkout capture
                  </p>
                </div>
                <div className="rounded-[1.5rem] border border-white/10 bg-white/5 p-4">
                  <p className="text-xs uppercase tracking-[0.2em] text-slate-400">
                    Network
                  </p>
                  <p className="mt-2 text-lg font-semibold text-white">
                    India domestic
                  </p>
                </div>
              </div>

              <div className="mt-8 rounded-[1.75rem] border border-cyan-400/15 bg-cyan-400/5 p-5">
                <p className="text-xs uppercase tracking-[0.28em] text-cyan-300">
                  Why this flow is safer
                </p>
                <div className="mt-4 grid gap-3">
                  {trustSignals.map((signal, index) => (
                    <div
                      key={signal}
                      className="flex items-center gap-3 rounded-2xl border border-white/5 bg-white/5 px-4 py-3"
                    >
                      <span className="inline-flex h-8 w-8 items-center justify-center rounded-full bg-cyan-400/10 text-cyan-300">
                        {`0${index + 1}`}
                      </span>
                      <p className="text-sm font-medium text-slate-100">
                        {signal}
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            <aside className="space-y-4 rounded-[1.75rem] border border-white/10 bg-white/5 p-5">
              <p className="text-xs uppercase tracking-[0.28em] text-slate-400">
                Payment summary
              </p>
              <div className="rounded-[1.5rem] border border-white/10 bg-slate-900/70 p-5">
                <div className="flex items-center justify-between gap-4">
                  <span className="text-sm text-slate-400">
                    Subscription renewal
                  </span>
                  <span className="text-sm font-semibold text-white">
                    {formatCurrency(PAYMENT_AMOUNT)}
                  </span>
                </div>
                <div className="mt-4 border-t border-white/10 pt-4">
                  <div className="flex items-center justify-between gap-4">
                    <span className="text-sm text-slate-400">Taxes and fees</span>
                    <span className="text-sm font-semibold text-white">
                      Included
                    </span>
                  </div>
                </div>
                <div className="mt-4 border-t border-white/10 pt-4">
                  <div className="flex items-center justify-between gap-4">
                    <span className="text-sm text-slate-400">Settlement rail</span>
                    <span className="text-sm font-semibold text-white">
                      {transactionMode === TRANSACTION_MODES.TEST
                        ? "Sandbox processor"
                        : "Primary processor"}
                    </span>
                  </div>
                </div>
              </div>

              <div className="rounded-[1.5rem] border border-white/10 bg-slate-900/70 p-5">
                <p className="text-sm font-semibold text-white">Operator note</p>
                <p className="mt-2 text-sm leading-6 text-slate-300">
                  {PAYMENT_NOTE}
                </p>
              </div>
            </aside>
          </div>
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.45, delay: 0.08, ease: "easeOut" }}
          className="rounded-[2rem] border border-slate-200/80 bg-white/90 p-6 shadow-[0_28px_90px_rgba(15,23,42,0.16)] backdrop-blur sm:p-8"
        >
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.28em] text-cyan-700">
                Secure checkout
              </p>
              <h2 className="mt-3 text-3xl font-semibold tracking-tight text-slate-950">
                Complete payment intent
              </h2>
              <p className="mt-3 text-sm leading-6 text-slate-500">
                Choose a payment rail, confirm the merchant details, and create
                a tracked payment request.
              </p>
            </div>
            <div className="rounded-[1.25rem] bg-slate-100 px-4 py-3 text-right">
              <p className="text-[11px] uppercase tracking-[0.2em] text-slate-500">
                Payable
              </p>
              <p className="mt-1 text-2xl font-semibold text-slate-950">
                {formatCurrency(PAYMENT_AMOUNT)}
              </p>
            </div>
          </div>

          <div className="mt-6 grid grid-cols-2 gap-3 rounded-[1.5rem] bg-slate-100 p-2">
            {["card", "upi"].map((option) => {
              const active = method === option;
              return (
                <button
                  key={option}
                  type="button"
                  onClick={() => setMethod(option)}
                  className={`rounded-[1.1rem] px-4 py-3 text-sm font-semibold transition ${
                    active
                      ? "bg-slate-950 text-white shadow-sm"
                      : "text-slate-500 hover:text-slate-900"
                  }`}
                >
                  {option === "card" ? "Card" : "UPI"}
                </button>
              );
            })}
          </div>

          <div className="mt-6">
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-cyan-700">
              Processing lane
            </p>
            <div className="mt-3 grid grid-cols-2 gap-3 rounded-[1.5rem] bg-slate-100 p-2">
              {[TRANSACTION_MODES.PRODUCTION, TRANSACTION_MODES.TEST].map(
                (option) => {
                  const active = transactionMode === option;
                  return (
                    <button
                      key={option}
                      type="button"
                      onClick={() => setTransactionMode(option)}
                      className={`rounded-[1.1rem] px-4 py-3 text-sm font-semibold transition ${
                        active
                          ? "bg-white text-slate-950 shadow-sm"
                          : "text-slate-500 hover:text-slate-900"
                      }`}
                    >
                      {option === TRANSACTION_MODES.TEST
                        ? "Sandbox"
                        : "Primary"}
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
              <UpiQR />
            )}
          </div>

          <div className="mt-6 rounded-[1.5rem] border border-amber-200 bg-amber-50 px-4 py-4 text-sm leading-6 text-amber-900">
            Payments are created with an idempotency key, traced with a request
            ID, and routed through the gateway before capture.
          </div>

          {submitError ? (
            <div className="mt-4 rounded-[1.5rem] border border-rose-200 bg-rose-50 px-4 py-4 text-sm text-rose-700">
              {submitError}
            </div>
          ) : null}

          <button
            type="button"
            onClick={pay}
            disabled={disabled}
            className="mt-6 w-full rounded-[1.25rem] bg-slate-950 px-5 py-4 text-base font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-300"
          >
            {submitting
              ? "Creating payment intent..."
              : `Pay ${formatCurrency(PAYMENT_AMOUNT)}`}
          </button>
        </motion.section>
      </div>
    </main>
  );
}
