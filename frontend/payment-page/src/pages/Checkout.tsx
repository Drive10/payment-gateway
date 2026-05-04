import { motion } from "framer-motion";
import { useState, useEffect, useMemo, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import CardForm from "../components/CardForm";
import UpiQR from "../components/UpiQR";
import NetBankingForm from "../components/NetBankingForm";
import WalletForm from "../components/WalletForm";
import {
  TRANSACTION_MODES,
  formatCurrency,
  startCheckout,
  validateCardForm,
} from "../lib/payment";

const API_BASE_URL = window.__ENV__?.API_BASE_URL || "http://localhost:8080";
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
    name: "Credit/Debit Card",
    icon: (
      <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
      </svg>
    ),
  },
  {
    id: "upi",
    name: "UPI",
    icon: (
      <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
      </svg>
    ),
  },
  {
    id: "netbanking",
    name: "Net Banking",
    icon: (
      <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h-2M7 19h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2 2v10a2 2 0 002 2zM9 9h6v6H9V9z" />
      </svg>
    ),
  },
  {
    id: "wallet",
    name: "Wallet",
    icon: (
      <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
      </svg>
    ),
  },
];

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
      navigate("/processing", { state: { checkout, paymentMethod: method } });
    } catch (error: unknown) {
      const err = error as Error;
      setSubmitError(err?.message || "Payment failed. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  const MIN_AMOUNT = 10;
  const MAX_AMOUNT = 100000;
  
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
    (method === "netbanking" && !values.bankCode) ||
    (method === "wallet" && !values.wallet) ||
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
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-to-br from-cyan-600 to-teal-600 shadow-lg shadow-cyan-500/20">
              <svg className="h-6 w-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2zm7-5a2 2 0 11-4 0 2 2 0 014 0z" />
              </svg>
            </div>
            <div>
              <h1 className="text-xl font-bold text-slate-900">PayFlow</h1>
              <p className="text-sm text-slate-500">Secure Payment Gateway</p>
            </div>
          </div>
          <div className="flex items-center gap-2 rounded-full bg-green-50 px-4 py-2">
            <span className="relative flex h-3 w-3">
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-green-400 opacity-75"></span>
              <span className="relative inline-flex h-3 w-3 rounded-full bg-green-500"></span>
            </span>
            <span className="text-sm font-medium text-green-700">Secure Connection</span>
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
                    <h1 className="text-sm font-medium text-slate-500">Order Summary</h1>
                    <h2 className="mt-1 text-2xl font-bold text-slate-900">Checkout</h2>
                  </div>
                  <div className="text-right">
                    <p className="text-sm text-slate-500">Total Amount</p>
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
                  <div className="rounded-xl bg-slate-50 p-4">
                    <p className="text-xs font-medium uppercase tracking-wider text-slate-500">Merchant</p>
                    <p className="mt-1 text-sm font-semibold text-slate-900">{displayMerchant}</p>
                  </div>
                  <div className="rounded-xl bg-slate-50 p-4">
                    <p className="text-xs font-medium uppercase tracking-wider text-slate-500">Order ID</p>
                    <p className="mt-1 font-mono text-sm text-slate-900">#{orderId}</p>
                  </div>
                  <div className="rounded-xl bg-slate-50 p-4">
                    <p className="text-xs font-medium uppercase tracking-wider text-slate-500">Date</p>
                    <p className="mt-1 text-sm font-semibold text-slate-900">{today}</p>
                  </div>
                </div>

                <div className="rounded-xl border border-slate-200 bg-white p-4">
                  <h3 className="text-sm font-semibold text-slate-900">Payment Details</h3>
                  <div className="mt-4 space-y-3">
                    <div className="flex justify-between text-sm">
                      <span className="text-slate-600">{displayDescription}</span>
                      <span className="font-medium text-slate-900">{formatCurrency(displayAmount)}</span>
                    </div>
                    <div className="flex justify-between border-t border-slate-100 pt-3 text-sm">
                      <span className="text-slate-600">Processing Fee</span>
                      <span className="font-medium text-green-600">Free</span>
                    </div>
                    <div className="flex justify-between border-t border-slate-100 pt-3 text-sm">
                      <span className="font-semibold text-slate-900">Total</span>
                      <span className="font-bold text-slate-900">{formatCurrency(displayAmount)}</span>
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
              <div className="border-b border-slate-100 bg-gradient-to-r from-cyan-600 to-teal-600 p-6">
                <h3 className="text-lg font-semibold text-white">Payment Method</h3>
                <p className="mt-1 text-sm text-cyan-100">Select how you want to pay</p>
              </div>

              <div className="p-6">
                <div className="mb-6 grid grid-cols-4 gap-3">
                  {paymentMethods.map((pm) => (
                    <button
                      key={pm.id}
                      onClick={() => setMethod(pm.id)}
                      className={`flex flex-col items-center justify-center gap-2 rounded-xl border-2 p-3 transition-all ${
                        method === pm.id
                          ? "border-cyan-500 bg-cyan-50 text-cyan-700"
                          : "border-slate-200 text-slate-600 hover:border-slate-300"
                      }`}
                    >
                      {pm.icon}
                      <span className="text-xs font-medium">{pm.name}</span>
                    </button>
                  ))}
                </div>

                {!IS_PRODUCTION && (
                <div className="mb-6">
                  <label className="mb-2 block text-sm font-medium text-slate-700">
                    Processing Environment
                  </label>
                  <div className="grid grid-cols-2 gap-3">
                    {[
                      { id: TRANSACTION_MODES.TEST, label: "Test/Sandbox", desc: "Simulated" },
                      { id: TRANSACTION_MODES.PRODUCTION, label: "Production", desc: "Live transactions" },
                    ].map((env) => (
                      <button
                        key={env.id}
                        onClick={() => setTransactionMode(env.id)}
                        className={`rounded-xl border-2 p-3 text-left transition-all ${
                          transactionMode === env.id
                            ? "border-cyan-500 bg-cyan-50"
                            : "border-slate-200 hover:border-slate-300"
                        }`}
                      >
                        <p className={`text-sm font-semibold ${transactionMode === env.id ? "text-cyan-700" : "text-slate-700"}`}>
                          {env.label}
                        </p>
                        <p className="text-xs text-slate-500">{env.desc}</p>
                      </button>
                    ))}
                  </div>
                </div>
                )}

<div className="mb-6">
                  {method === "card" ? (
                    <CardForm values={values} errors={errors} onChange={handleChange} />
                   ) : method === "upi" ? (
                    <UpiQR amount={amount} onPay={handlePay} autoShow={!IS_PRODUCTION} />
                   ) : method === "netbanking" ? (
                     <NetBankingForm values={values} onChange={handleChange} />
                   ) : (
                     <WalletForm values={values} onChange={handleChange} />
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
                  className="w-full rounded-xl bg-gradient-to-r from-cyan-600 to-teal-600 py-4 text-base font-semibold text-white shadow-lg shadow-cyan-500/30 transition-all hover:shadow-xl hover:shadow-cyan-500/40 disabled:cursor-not-allowed disabled:opacity-50 disabled:shadow-none"
                >
                  {submitting ? (
                    <span className="flex items-center justify-center gap-2">
                      <svg className="h-5 w-5 animate-spin" fill="none" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                      </svg>
                      Processing...
                    </span>
                  ) : !IS_PRODUCTION && method === "upi" ? (
                    "Confirm UPI Payment"
                  ) : (
                    `Pay ${formatCurrency(displayAmount) || "₹0"}`
                  )}
                </button>

                <div className="mt-4 flex items-center justify-center gap-4">
                  <div className="flex items-center gap-1 text-xs text-slate-500">
                    <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                    </svg>
                    <span>SSL Encrypted</span>
                  </div>
                  <div className="flex items-center gap-1 text-xs text-slate-500">
                    <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                    </svg>
                    <span>PCI Compliant</span>
                  </div>
                </div>
              </div>
            </div>
          </motion.div>
        </div>
      </div>
    </div>
  );
}
