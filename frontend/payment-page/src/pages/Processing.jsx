import { motion } from "framer-motion";
import { useEffect, useState, useCallback } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { formatCurrency, getStoredTransaction } from "../lib/payment";

const API_BASE_URL = window.__ENV__?.API_BASE_URL || "/api/v1";
const IS_PRODUCTION = window.__ENV__?.IS_PRODUCTION || false;

const FINAL_STATES = ["CAPTURED", "COMPLETED", "SUCCESS"];
const FAILURE_STATES = ["FAILED", "EXPIRED"];

export default function Processing() {
  const location = useLocation();
  const navigate = useNavigate();
  const checkout = location.state?.checkout ?? getStoredTransaction();
  const [error, setError] = useState(null);
  const [status, setStatus] = useState("processing");
  const [pollAttempt, setPollAttempt] = useState(0);
  const [showOtpModal, setShowOtpModal] = useState(false);
  const [otp, setOtp] = useState("");
  const [otpError, setOtpError] = useState("");

  const pollBackendForStatus = useCallback(async () => {
    if (!checkout?.payment?.id) return;

    const maxAttempts = 30; // 30 attempts * 3s = 90 seconds max

    for (let attempt = 0; attempt < maxAttempts; attempt++) {
      try {
        const response = await fetch(
          `${API_BASE_URL}/payments/${checkout.payment.id}/status`,
          {
            headers: checkout.token ? { Authorization: `Bearer ${checkout.token}` } : {},
          }
        );
        const data = await response.json();

        if (data.success && data.data) {
          const paymentStatus = data.data.status;
          
          setStatus(paymentStatus);
          setPollAttempt(attempt);

          if (paymentStatus === "CREATED") {
            try {
              const captureResponse = await fetch(
                `${API_BASE_URL}/payments/${checkout.payment.id}/capture`,
                {
                  method: "POST",
                  headers: {
                    "Content-Type": "application/json",
                    ...(checkout.token ? { Authorization: `Bearer ${checkout.token}` } : {}),
                  },
                  body: JSON.stringify({}),
                }
              );
              const captureData = await captureResponse.json();
              if (captureData.success && captureData.data) {
                setStatus(captureData.data.status);
              }
            } catch (captureErr) {
              console.error("Capture error:", captureErr);
            }
            await new Promise((r) => setTimeout(r, 1000));
            continue;
          }

          if (FINAL_STATES.includes(paymentStatus)) {
            navigate("/success", {
              replace: true,
              state: {
                transaction: buildTransaction(paymentStatus),
              },
            });
            return;
          }

          if (FAILURE_STATES.includes(paymentStatus)) {
            setError(data.data.statusMessage || "Payment failed");
            setTimeout(() => {
              navigate("/failure", {
                replace: true,
                state: { transaction: checkout, error: data.data.statusMessage || "Payment failed" },
              });
            }, 2000);
            return;
          }

          if (paymentStatus === "AUTHORIZATION_PENDING") {
            setShowOtpModal(true);
            return;
          }

          if (paymentStatus === "CHALLENGE_REQUIRED" || data.data?.requires3ds) {
            const challengeUrl = data.data?.threeDsChallengeUrl;
            if (challengeUrl) {
              setTimeout(() => {
                window.location.href = challengeUrl;
              }, 1500);
              return;
            }
          }

          await new Promise((r) => setTimeout(r, 3000));
        }
      } catch (err) {
        console.error("Status poll error:", err);
        await new Promise((r) => setTimeout(r, 3000));
      }
    }

    setError("Payment is taking longer than expected. Please try again or contact support.");
    setTimeout(() => {
      navigate("/failure", {
        replace: true,
        state: { transaction: checkout, error: "Payment timeout" },
      });
    }, 3000);
  }, [checkout, navigate]);

  useEffect(() => {
    if (!checkout?.payment?.id) {
      navigate("/", { replace: true });
      return;
    }

    if (IS_PRODUCTION && checkout.payment.checkoutUrl) {
      window.location.href = checkout.payment.checkoutUrl;
      return;
    }

    pollBackendForStatus();
  }, [checkout, navigate, pollBackendForStatus]);

  const buildTransaction = (status) => ({
    id: checkout.payment.id,
    orderId: checkout.order?.id,
    orderReference: checkout.order?.externalReference,
    status: status,
    amount: checkout.amount,
    amountLabel: formatCurrency(checkout.amount),
    method: checkout.method,
    methodLabel: checkout.method === "upi" ? "UPI" : "Card",
    customerLabel:
      checkout.cardholder ||
      `${checkout.customer?.firstName} ${checkout.customer?.lastName}`.trim(),
    environmentLabel: "Sandbox lane",
    correlationId: checkout.correlationId,
  });

  const handleOtpSubmit = async () => {
    if (!otp || otp.length !== 6) {
      setOtpError("Please enter 6-digit OTP");
      return;
    }

    setOtpError("");
    setStatus("processing");
    setShowOtpModal(false);

    try {
      const response = await fetch(
        `${API_BASE_URL}/payments/${checkout.payment.id}/verify-otp`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${checkout.token}`,
          },
          body: JSON.stringify({ otp }),
        }
      );
      const data = await response.json();

      if (data.success && data.data?.status === "CAPTURED") {
        navigate("/success", {
          replace: true,
          state: { transaction: buildTransaction("CAPTURED") },
        });
        return;
      }

      pollBackendForStatus();
    } catch (err) {
      setOtpError("Verification failed. Try 123456");
      setShowOtpModal(true);
      setStatus("pending_otp");
    }
  };

  const handleCancel = () => {
    setShowOtpModal(false);
    navigate("/failure", { replace: true, state: { transaction: checkout, error: "Payment cancelled by user" } });
  };

  if (showOtpModal) {
    return (
      <main className="flex min-h-screen items-center justify-center px-4 py-10">
        <motion.section
          initial={{ opacity: 0, scale: 0.97 }}
          animate={{ opacity: 1, scale: 1 }}
          className="w-full max-w-md overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl"
        >
          <div className="bg-gradient-to-r from-cyan-600 to-teal-600 p-6 text-white">
            <h1 className="text-xl font-semibold">Verify Payment</h1>
            <p className="mt-2 text-sm text-cyan-100">
              Enter the 6-digit OTP sent to your registered mobile number
            </p>
          </div>
          <div className="p-6">
            <div className="mb-4">
              <label className="mb-2 block text-sm font-medium text-slate-700">
                OTP Code
              </label>
              <input
                type="text"
                value={otp}
                onChange={(e) => setOtp(e.target.value.replace(/\D/g, "").slice(0, 6))}
                placeholder="Enter 6-digit OTP"
                className="w-full rounded-xl border-2 border-slate-300 px-4 py-3 text-center text-2xl tracking-widest focus:border-cyan-500 focus:outline-none"
                maxLength={6}
              />
              {otpError && (
                <p className="mt-2 text-sm text-red-600">{otpError}</p>
              )}
            </div>
            <p className="mb-4 text-center text-sm text-slate-500">
              Test OTP: <span className="font-mono font-semibold">123456</span>
            </p>
            <div className="flex gap-3">
              <button
                onClick={handleCancel}
                className="flex-1 rounded-xl border border-slate-300 px-4 py-3 font-semibold text-slate-700"
              >
                Cancel
              </button>
              <button
                onClick={handleOtpSubmit}
                disabled={status === "processing"}
                className="flex-1 rounded-xl bg-slate-950 px-4 py-3 font-semibold text-white"
              >
                {status === "processing" ? "Verifying..." : "Verify OTP"}
              </button>
            </div>
          </div>
        </motion.section>
      </main>
    );
  }

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
