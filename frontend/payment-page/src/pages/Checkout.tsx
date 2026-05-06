import { motion } from "framer-motion";
import { useState, useEffect, useMemo, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import CardForm from "../components/CardForm";
import UpiQR from "../components/UpiQR";
import {
  TRANSACTION_MODES,
  formatCurrency,
  startCheckout,
  validateCardForm,
  persistCardDetails,
} from "../lib/payment";

const API_BASE_URL = window.__ENV__?.API_BASE_URL || "http://localhost:3001";
const API_ROOT = API_BASE_URL.endsWith("/api/v1") ? API_BASE_URL : `${API_BASE_URL}/api/v1`;
const IS_PRODUCTION = window.__ENV__?.IS_PRODUCTION === true;

const initialForm = {
  cardNumber: "",
  expiry: "",
  cvv: "",
  cardholder: "",
};

const paymentMethods = [
  {
    id: "card",
    name: "Card",
    desc: "Credit / Debit Card",
    icon: (
      <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
      </svg>
    ),
    accent: "indigo",
  },
  {
    id: "upi",
    name: "UPI",
    desc: "Instant Payment",
    icon: (
      <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
      </svg>
    ),
    accent: "emerald",
  },
];

const MIN_AMOUNT = 10;
const MAX_AMOUNT = 100000;

export default function Checkout() {
  const [searchParams] = useSearchParams();
  const [method, setMethod] = useState("card");
  const [values, setValues] = useState(initialForm);
  const [transactionMode, setTransactionMode] = useState(TRANSACTION_MODES.TEST);
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState("");
  const [paymentLinkData, setPaymentLinkData] = useState(null);
  const [loadingLink, setLoadingLink] = useState(false);
  const [paymentLinkChecked, setPaymentLinkChecked] = useState(false);
  const [amountInput, setAmountInput] = useState("");
  const [showAmountInput, setShowAmountInput] = useState(false);
  const navigate = useNavigate();

  const { orderId, today } = useMemo(() => {
    const now = new Date();
    return {
      orderId: `ORD-${now.getTime().toString(36).substring(2, 10).toUpperCase()}`,
      today: now.toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" }),
    };
  }, []);

  useEffect(() => {
    if (!IS_PRODUCTION && !amountInput) {
      setAmountInput("100");
    }
    const amount = searchParams.get("amount");
    if (amount && !isNaN(parseFloat(amount)) && parseFloat(amount) > 0) {
      setAmountInput(amount);
    }
    const referenceId = searchParams.get("ref");
    if (referenceId) {
      loadPaymentLink(referenceId);
    } else {
      setPaymentLinkChecked(true);
    }
  }, [searchParams]);

  const loadPaymentLink = async (referenceId) => {
    setLoadingLink(true);
    try {
      const response = await fetch(`${API_ROOT}/payments/link/${referenceId}`);
      const data = await response.json();
      if (data.success && data.data) {
        setPaymentLinkData(data.data);
        if (data.data.amount) {
          setAmountInput(data.data.amount.toString());
        }
      } else {
        setSubmitError("Invalid or expired payment link");
      }
} catch (err) {
        setLoadingLink(false);
      }
  };

  if (loadingLink) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  if (searchParams.get("ref") && paymentLinkChecked && !paymentLinkData) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-slate-800 mb-2">Invalid Payment Link</h1>
          <p className="text-slate-600">This payment link is invalid or has expired.</p>
        </div>
      </div>
    );
  }

  const handleChange = (field, value) => {
    setValues((current) => ({ ...current, [field]: value }));
    setErrors((current) => ({ ...current, [field]: undefined }));
  };

  const handleAmountChange = (e) => {
    const value = e.target.value.replace(/[^0-9.]/g, "");
    setAmountInput(value);
  };

  const sanitizeInput = (value: string) => {
    return value.replace(/[<>'";&]/g, "");
  };

  const handlePay = async () => {
    const amount = parseFloat(amountInput);
    
    if (!amountInput || amountInput.trim() === "") {
      setSubmitError("Please enter a payment amount");
      return;
    }

    if (isNaN(amount) || amount <= 0) {
      setSubmitError("Please enter a valid payment amount");
      return;
    }

    if (amount < MIN_AMOUNT) {
      setSubmitError(`Minimum payment amount is ₹${MIN_AMOUNT}`);
      return;
    }

    if (amount > MAX_AMOUNT) {
      setSubmitError(`Maximum payment amount is ₹${MAX_AMOUNT.toLocaleString()}`);
      return;
    }

    if (method === "card") {
      const nextErrors = validateCardForm(values);
      if (Object.keys(nextErrors).length > 0) {
        setErrors(nextErrors);
        return;
      }
    }

    setSubmitting(true);
    setSubmitError("");

    try {
      const checkout = await startCheckout({
        amount,
        method,
        cardholder: sanitizeInput(values.cardholder),
        transactionMode,
        description: sanitizeInput(paymentLinkData?.description || "Payment for order"),
      });
      if (method === "card") {
        persistCardDetails(values);
      }
      navigate("/processing", { state: { checkout, paymentMethod: method } });
    } catch (error: unknown) {
      const err = error as Error;
      setSubmitError(err?.message || "Payment failed. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  const amount = parseFloat(amountInput) || 0;
  const isValidAmount = amount >= MIN_AMOUNT && amount <= MAX_AMOUNT;

  const displayAmount = paymentLinkData?.amount ?? (amountInput ? parseFloat(amountInput) : 0);
  const displayCurrency = paymentLinkData?.currency || "INR";
  const displayMerchant = paymentLinkData?.merchantName || "PayFlow Merchant";
  const displayDescription = paymentLinkData?.description || "Payment for order";
  
  const disabled =
    submitting ||
    !amountInput ||
    !isValidAmount ||
    (method === "card" &&
      (!values.cardNumber || !values.expiry || !values.cvv || !values.cardholder.trim())) ||
    (method === "upi" && !IS_PRODUCTION);

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-cyan-50">
      <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <header>
          <h1 className="sr-only">PayFlow Checkout</h1>
        </header>
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-8 flex items-center justify-between"
        >
          <div className="flex items-center gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-to-br from-indigo-600 to-violet-600 shadow-lg shadow-indigo-500/25">
              <svg className="h-7 w-7 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div>
              <h1 className="text-2xl font-bold bg-gradient-to-r from-indigo-600 to-violet-600 bg-clip-text text-transparent">PayFlow</h1>
              <p className="text-sm text-slate-500 font-medium">Secure Payment Gateway</p>
            </div>
          </div>
          <div className="flex items-center gap-3 rounded-full border border-green-200 bg-green-50/80 px-4 py-2">
            <span className="relative flex h-2.5 w-2.5">
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-green-500 opacity-75"></span>
              <span className="relative inline-flex h-2.5 w-2.5 rounded-full bg-green-500"></span>
            </span>
            <span className="text-sm font-semibold text-green-700">Secure</span>
            <span className="text-xs text-green-600">256-bit SSL</span>
          </div>
        </motion.div>

        <div className="grid gap-8 lg:grid-cols-3">
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.1 }}
            className="lg:col-span-2"
          >
            <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl shadow-slate-200/50">
              <div className="border-b border-slate-100 bg-gradient-to-r from-slate-50 to-white p-6">
                <div className="flex items-center justify-between">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="rounded-full bg-indigo-100 px-3 py-1 text-xs font-semibold text-indigo-700">Checkout</span>
                    </div>
                    <h2 className="mt-3 text-2xl font-bold text-slate-900">Order Summary</h2>
                  </div>
                  <div className="text-right">
                    <p className="text-sm text-slate-500">Total to Pay</p>
                    <p className="text-3xl font-bold text-slate-900">
                      {displayCurrency === "INR"
                        ? formatCurrency(displayAmount)
                        : `${displayCurrency} ${displayAmount?.toFixed?.(2) ?? displayAmount}`}
                    </p>
                  </div>
                </div>
              </div>

              <div className="space-y-6 p-6">
                <div className="grid grid-cols-3 gap-4">
                  <div className="group relative overflow-hidden rounded-xl border border-slate-200 bg-slate-50 p-4 transition-all hover:border-slate-300 hover:shadow-md">
                    <div className="absolute right-0 top-0 h-12 w-12 -translate-y-2 translate-x-2 rounded-full bg-indigo-100 transition-transform group-hover:scale-150"></div>
                    <p className="text-xs font-medium uppercase tracking-wider text-slate-500">Merchant</p>
                    <p className="mt-1 text-sm font-bold text-slate-900 truncate">{displayMerchant}</p>
                  </div>
                  <div className="group relative overflow-hidden rounded-xl border border-slate-200 bg-slate-50 p-4 transition-all hover:border-slate-300 hover:shadow-md">
                    <div className="absolute right-0 top-0 h-12 w-12 -translate-y-2 translate-x-2 rounded-full bg-violet-100 transition-transform group-hover:scale-150"></div>
                    <p className="text-xs font-medium uppercase tracking-wider text-slate-500">Order ID</p>
                    <p className="mt-1 font-mono text-sm font-bold text-slate-900">#{orderId}</p>
                  </div>
                  <div className="group relative overflow-hidden rounded-xl border border-slate-200 bg-slate-50 p-4 transition-all hover:border-slate-300 hover:shadow-md">
                    <div className="absolute right-0 top-0 h-12 w-12 -translate-y-2 translate-x-2 rounded-full bg-emerald-100 transition-transform group-hover:scale-150"></div>
                    <p className="text-xs font-medium uppercase tracking-wider text-slate-500">Date</p>
                    <p className="mt-1 text-sm font-bold text-slate-900">{today}</p>
                  </div>
                </div>

                <div className="rounded-xl border border-slate-200 bg-white p-5">
                  <h3 className="text-sm font-bold text-slate-900">Payment Breakdown</h3>
                  <div className="mt-4 space-y-3">
                    <div className="flex justify-between">
                      <span className="text-sm text-slate-600">{displayDescription}</span>
                      <span className="font-medium text-slate-900">{formatCurrency(displayAmount)}</span>
                    </div>
                    <div className="flex justify-between border-t border-dashed border-slate-200 pt-3">
                      <span className="text-sm text-slate-500">Processing Fee</span>
                      <span className="font-medium text-emerald-600">₹0.00</span>
                    </div>
                    <div className="flex justify-between border-t-2 border-slate-900 pt-3">
                      <span className="text-base font-bold text-slate-900">Total Amount</span>
                      <span className="text-xl font-bold text-slate-900">{formatCurrency(displayAmount)}</span>
                    </div>
                  </div>
                </div>

                {!paymentLinkData && !searchParams.get("amount") && (
                  <div className="rounded-xl border border-slate-200 bg-white p-4">
                    <button
                      type="button"
                      onClick={() => setShowAmountInput(!showAmountInput)}
                      className="flex items-center gap-2 text-sm font-medium text-cyan-600 hover:text-cyan-700"
                    >
                      <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                      </svg>
                      {showAmountInput ? "Hide custom amount" : "Enter custom amount"}
                    </button>
                    {showAmountInput && (
                      <div className="mt-3">
                        <label className="block text-sm font-medium text-slate-700">Amount (INR)</label>
                        <input
                          type="text"
                          value={amountInput}
                          onChange={handleAmountChange}
                          placeholder="Enter amount"
                          className={`mt-1 block w-full rounded-lg border-2 px-3 py-2 text-slate-900 placeholder-slate-400 outline-none transition-colors ${
                            submitError && (parseFloat(amountInput) < MIN_AMOUNT || parseFloat(amountInput) > MAX_AMOUNT)
                              ? "border-red-300 focus:border-red-500 focus:ring-2 focus:ring-red-500/20"
                              : "border-slate-300 focus:border-cyan-500 focus:ring-2 focus:ring-cyan-500/20"
                          }`}
                        />
                        <p className="mt-1 text-xs text-slate-500">
                          Minimum: ₹{MIN_AMOUNT.toLocaleString()} | Maximum: ₹{MAX_AMOUNT.toLocaleString()}
                        </p>
                      </div>
                    )}
                  </div>
                )}

                <div className="rounded-xl border border-amber-200 bg-amber-50 p-4">
                  <div className="flex items-start gap-3">
                    <svg className="mt-0.5 h-5 w-5 text-amber-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                    </svg>
                    <p className="text-sm text-amber-800">
                      Your payment is secured with PCI-compliant encryption. We never store your card details.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.2 }}
          >
            <div className="sticky top-8 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl shadow-slate-200/50">
              <div className="border-b border-slate-100 bg-gradient-to-r from-indigo-600 to-violet-600 p-6">
                <h3 className="text-lg font-semibold text-white">Payment Details</h3>
                <p className="mt-1 text-sm text-indigo-100">Complete your payment securely</p>
              </div>

              <div className="p-6">
                <h4 className="mb-4 text-sm font-semibold text-slate-700">Choose Payment Method</h4>
                <div className="mb-6 grid grid-cols-2 gap-3">
                  {paymentMethods.map((pm) => (
                    <button
                      key={pm.id}
                      onClick={() => setMethod(pm.id)}
                      className={`group relative flex items-center gap-3 rounded-xl border-2 p-4 text-left transition-all hover:shadow-md ${
                        method === pm.id
                          ? `border-${pm.accent}-500 bg-${pm.accent}-50 ring-2 ring-${pm.accent}-200 ring-offset-2`
                          : "border-slate-200 bg-white hover:border-slate-300"
                      }`}
                      style={method === pm.id ? { 
                        borderColor: pm.accent === 'indigo' ? '#6366f1' : pm.accent === 'emerald' ? '#10b981' : pm.accent === 'amber' ? '#f59e0b' : '#8b5cf6',
                        backgroundColor: pm.accent === 'indigo' ? '#eef2ff' : pm.accent === 'emerald' ? '#ecfdf5' : pm.accent === 'amber' ? '#fffbeb' : '#f5f3ff'
                      } : {}}
                    >
                      <div className={`flex h-10 w-10 items-center justify-center rounded-lg bg-slate-100 transition-colors group-hover:bg-slate-200 ${
                        method === pm.id ? 'bg-white shadow-sm' : ''
                      }`} style={method === pm.id ? {
                        color: pm.accent === 'indigo' ? '#6366f1' : pm.accent === 'emerald' ? '#10b981' : pm.accent === 'amber' ? '#f59e0b' : '#8b5cf6'
                      } : {}}>
                        {pm.icon}
                      </div>
                      <div className="flex-1">
                        <p className={`font-semibold ${method === pm.id ? 'text-slate-900' : 'text-slate-700'}`}>{pm.name}</p>
                        <p className="text-xs text-slate-500">{pm.desc}</p>
                      </div>
                      <div className={`h-5 w-5 rounded-full border-2 transition-all ${
                        method === pm.id ? 'border-indigo-500 bg-indigo-500' : 'border-slate-300'
                      }`}>
                        {method === pm.id && (
                          <svg className="h-5 w-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                          </svg>
                        )}
                      </div>
                    </button>
                  ))}
                </div>

                {!IS_PRODUCTION && (
                <div className="mb-6">
                  <label className="mb-3 block text-xs font-semibold uppercase tracking-wider text-slate-500">
                    Processing Mode
                  </label>
                  <div className="grid grid-cols-2 gap-3">
                    {[
                      { id: TRANSACTION_MODES.TEST, label: "Test Mode", desc: "Sandbox / Simulation", badge: "Recommended" },
                      { id: TRANSACTION_MODES.PRODUCTION, label: "Production", desc: "Live Transactions", badge: null },
                    ].map((env) => (
                      <button
                        key={env.id}
                        onClick={() => setTransactionMode(env.id)}
                        className={`relative rounded-xl border-2 p-4 text-left transition-all ${
                          transactionMode === env.id
                            ? "border-indigo-500 bg-indigo-50 ring-2 ring-indigo-200 ring-offset-2"
                            : "border-slate-200 hover:border-slate-300 bg-white"
                        }`}
                      >
                        {env.badge && (
                          <span className="absolute -top-2 right-3 rounded-full bg-emerald-100 px-2 py-0.5 text-[10px] font-bold uppercase text-emerald-700">
                            {env.badge}
                          </span>
                        )}
                        <p className={`text-sm font-bold ${transactionMode === env.id ? "text-indigo-700" : "text-slate-700"}`}>
                          {env.label}
                        </p>
                        <p className="mt-1 text-xs text-slate-500">{env.desc}</p>
                      </button>
                    ))}
                  </div>
                </div>
                )}

<div className="mb-6">
                  {method === "card" ? (
                    <CardForm values={values} errors={errors} onChange={handleChange} />
                   ) : (
                    <UpiQR amount={amount} onPay={handlePay} autoShow={!IS_PRODUCTION} />
                   )}
                </div>

                {submitError && (
                  <div className="mb-4 rounded-xl border border-red-200 bg-red-50 p-4">
                    <div className="flex items-center gap-2 text-red-700">
                      <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                      <span className="text-sm font-medium">{submitError}</span>
                    </div>
                  </div>
                )}

                <button
                  onClick={handlePay}
                  disabled={disabled}
                  className="group relative w-full overflow-hidden rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 py-4 text-base font-bold text-white shadow-lg shadow-indigo-500/30 transition-all hover:shadow-xl hover:shadow-indigo-500/40 disabled:cursor-not-allowed disabled:opacity-50 disabled:shadow-none"
                >
                  <div className="absolute inset-0 bg-gradient-to-r from-indigo-500 to-violet-500 opacity-0 transition-opacity group-hover:opacity-100"></div>
                  <span className="relative flex items-center justify-center gap-2">
                    {submitting ? (
                      <>
                        <svg className="h-5 w-5 animate-spin" fill="none" viewBox="0 0 24 24">
                          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                        </svg>
                        Processing Payment...
                      </>
                    ) : !IS_PRODUCTION && method === "upi" ? (
                      <>
                        <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
                        </svg>
                        Pay with UPI
                      </>
                    ) : (
                      <>
                        <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                        Pay {formatCurrency(displayAmount) || "₹0"}
                      </>
                    )}
                  </span>
                </button>

                <div className="mt-4 flex items-center justify-center gap-6 border-t border-slate-100 pt-4">
                  <div className="flex items-center gap-2 text-xs text-slate-500">
                    <svg className="h-4 w-4 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                    </svg>
                    <span className="font-medium">256-bit SSL Encryption</span>
                  </div>
                  <div className="flex items-center gap-2 text-xs text-slate-500">
                    <svg className="h-4 w-4 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                    </svg>
                    <span className="font-medium">PCI-DSS Compliant</span>
                  </div>
                </div>
              </div>
            </div>
          </motion.div>
        </div>
      </div>

      {!IS_PRODUCTION && (
        <div style={{ position: "fixed", bottom: 16, right: 16, zIndex: 9999 }}>
          <div style={{ display: "flex", flexWrap: "wrap", gap: "4px", maxWidth: "320px" }}>
            <div style={{ fontSize: "10px", color: "#666", width: "100%", marginBottom: "4px" }}>💳 Card Schemes</div>
            <button onClick={() => setValues({ cardNumber: "4111111111111111", expiry: "12/28", cvv: "123", cardholder: "Test User" })} style={{ background: "#4338ca", color: "white", padding: "6px 10px", borderRadius: "6px", fontSize: "11px" }}>Visa</button>
            <button onClick={() => setValues({ cardNumber: "5111111111111111", expiry: "12/28", cvv: "123", cardholder: "Test User" })} style={{ background: "#4338ca", color: "white", padding: "6px 10px", borderRadius: "6px", fontSize: "11px" }}>Mastercard</button>
            <button onClick={() => setValues({ cardNumber: "371111111111111", expiry: "12/28", cvv: "1234", cardholder: "Test User" })} style={{ background: "#4338ca", color: "white", padding: "6px 10px", borderRadius: "6px", fontSize: "11px" }}>Amex</button>
            <button onClick={() => setValues({ cardNumber: "6011111111111111", expiry: "12/28", cvv: "123", cardholder: "Test User" })} style={{ background: "#4338ca", color: "white", padding: "6px 10px", borderRadius: "6px", fontSize: "11px" }}>RuPay</button>
            
            <div style={{ fontSize: "10px", color: "#666", width: "100%", marginTop: "8px", marginBottom: "4px" }}>🔄 Payment Outcomes</div>
            <button onClick={() => setValues({ cardNumber: "4111111111111111", expiry: "12/28", cvv: "123", cardholder: "Test User" })} style={{ background: "#059669", color: "white", padding: "6px 10px", borderRadius: "6px", fontSize: "11px" }}>Success</button>
            <button onClick={() => setValues({ cardNumber: "4000000000000000", expiry: "12/28", cvv: "123", cardholder: "Test User" })} style={{ background: "#dc2626", color: "white", padding: "6px 10px", borderRadius: "6px", fontSize: "11px" }}>Failed (4000)</button>
            <button onClick={() => setValues({ cardNumber: "4002000000000000", expiry: "12/28", cvv: "123", cardholder: "Test User" })} style={{ background: "#f59e0b", color: "white", padding: "6px 10px", borderRadius: "6px", fontSize: "11px" }}>3DS (4002)</button>
            <button onClick={() => setValues({ cardNumber: "4003000000000000", expiry: "12/28", cvv: "123", cardholder: "Test User" })} style={{ background: "#8b5cf6", color: "white", padding: "6px 10px", borderRadius: "6px", fontSize: "11px" }}>OTP Flow (4003)</button>
            
            <div style={{ fontSize: "10px", color: "#666", width: "100%", marginTop: "8px", marginBottom: "4px" }}>💱 Payment Methods</div>
            <button onClick={() => { setMethod("card"); setAmountInput("100"); }} style={{ background: "#4338ca", color: "white", padding: "6px 10px", borderRadius: "6px", fontSize: "11px" }}>Card</button>
            <button onClick={() => { setMethod("upi"); setAmountInput("100"); }} style={{ background: "#0891b2", color: "white", padding: "6px 10px", borderRadius: "6px", fontSize: "11px" }}>UPI</button>
            <button onClick={() => { localStorage.clear(); sessionStorage.clear(); }} style={{ background: "#475569", color: "white", padding: "6px 10px", borderRadius: "6px", fontSize: "11px" }}>Clear</button>
          </div>
        </div>
      )}
    </div>
  );
}
